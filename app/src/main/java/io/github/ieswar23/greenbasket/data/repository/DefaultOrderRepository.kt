package io.github.ieswar23.greenbasket.data.repository

import io.github.ieswar23.greenbasket.data.local.dao.CartDao
import io.github.ieswar23.greenbasket.data.local.dao.CatalogDao
import io.github.ieswar23.greenbasket.data.local.dao.OrderDao
import io.github.ieswar23.greenbasket.data.local.entity.OrderEntity
import io.github.ieswar23.greenbasket.data.preferences.PreferencesRepository
import io.github.ieswar23.greenbasket.data.remote.GroceryApi
import io.github.ieswar23.greenbasket.data.remote.dto.OrderLineDto
import io.github.ieswar23.greenbasket.data.remote.dto.PlaceOrderRequest
import io.github.ieswar23.greenbasket.data.toDomain
import io.github.ieswar23.greenbasket.data.toOrderItemEntity
import io.github.ieswar23.greenbasket.domain.CartCalculator
import io.github.ieswar23.greenbasket.domain.CouponCatalog
import io.github.ieswar23.greenbasket.domain.model.CartItem
import io.github.ieswar23.greenbasket.domain.model.Order
import io.github.ieswar23.greenbasket.domain.model.OrderStatus
import io.github.ieswar23.greenbasket.util.TimeProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultOrderRepository @Inject constructor(
    private val api: GroceryApi,
    private val orderDao: OrderDao,
    private val catalogDao: CatalogDao,
    private val cartDao: CartDao,
    private val preferences: PreferencesRepository,
    private val calculator: CartCalculator,
    private val time: TimeProvider,
) : OrderRepository {

    /** Emits the current time periodically so simulated order statuses progress while the screen is open. */
    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(time.now())
            delay(STATUS_REFRESH_MS)
        }
    }

    override fun observeOrders(): Flow<List<Order>> =
        combine(orderDao.observeOrders(), ticker) { rows, now -> rows.map { it.toDomain(now) } }
            .distinctUntilChanged()

    override fun observeOrder(orderId: String): Flow<Order?> =
        combine(orderDao.observeOrder(orderId), ticker) { row, now -> row?.toDomain(now) }
            .distinctUntilChanged()

    override suspend fun placeOrder(params: PlaceOrderParams): Result<String> = try {
        require(params.items.isNotEmpty()) { "Your cart is empty" }
        val response = api.placeOrder(
            PlaceOrderRequest(
                items = params.items.map { OrderLineDto(it.product.id, it.quantity) },
                slotId = params.slot.id,
                addressId = params.address.id,
                paymentMethod = params.paymentMethod.name,
                couponCode = params.bill.appliedCoupon?.takeIf { params.bill.couponDiscount > 0 }?.code,
                amount = params.bill.grandTotal,
            ),
        )
        val bill = params.bill
        val order = OrderEntity(
            id = response.orderId,
            placedAt = time.now(),
            status = OrderStatus.PLACED.name,
            slotLabel = params.slotLabel,
            isExpress = params.slot.isExpress,
            slotStartMillis = params.slot.startMillis,
            addressLabel = params.address.label,
            addressLine = params.address.fullLine,
            paymentMethod = params.paymentMethod.name,
            itemTotal = bill.itemTotal,
            mrpTotal = bill.mrpTotal,
            deliveryFee = bill.deliveryFee,
            handlingFee = bill.handlingFee,
            couponCode = bill.appliedCoupon?.code?.takeIf { bill.couponDiscount > 0 },
            couponDiscount = bill.couponDiscount,
            grandTotal = bill.grandTotal,
        )
        orderDao.insert(order, params.items.map { it.product.toOrderItemEntity(order.id, it.quantity) })
        cartDao.clear()
        preferences.setCouponCode(null)
        Result.success(order.id)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }

    override suspend fun importHistoryIfEmpty() {
        if (orderDao.count() > 0) return
        val now = time.now()
        val history = runCatching { api.orderHistory().orders.orEmpty() }.getOrDefault(emptyList())
        history.forEach { dto ->
            val products = catalogDao.getProducts(dto.items.map { it.productId }).associateBy { it.id }
            val items = dto.items.mapNotNull { line ->
                products[line.productId]?.let { CartItem(it.toDomain(), line.quantity) }
            }
            if (items.isEmpty()) return@forEach
            val bill = calculator.calculate(items, CouponCatalog.find(dto.couponCode))
            val placedAt = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -dto.placedDaysAgo)
                set(Calendar.HOUR_OF_DAY, dto.hour)
                set(Calendar.MINUTE, dto.minute)
            }.timeInMillis
            val order = OrderEntity(
                id = dto.id,
                placedAt = placedAt,
                status = dto.status,
                slotLabel = dto.slotLabel,
                isExpress = dto.slotLabel.startsWith("Express"),
                slotStartMillis = placedAt,
                addressLabel = "Home",
                addressLine = "Flat 1204, Lotus Residency, Hiranandani Gardens, Powai, Mumbai - 400076",
                paymentMethod = dto.paymentMethod,
                itemTotal = bill.itemTotal,
                mrpTotal = bill.mrpTotal,
                deliveryFee = bill.deliveryFee,
                handlingFee = bill.handlingFee,
                couponCode = bill.appliedCoupon?.code?.takeIf { bill.couponDiscount > 0 },
                couponDiscount = bill.couponDiscount,
                grandTotal = bill.grandTotal,
            )
            orderDao.insert(order, items.map { it.product.toOrderItemEntity(order.id, it.quantity) })
        }
    }

    private companion object {
        const val STATUS_REFRESH_MS = 30_000L
    }
}
