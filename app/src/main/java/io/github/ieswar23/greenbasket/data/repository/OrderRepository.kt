package io.github.ieswar23.greenbasket.data.repository

import io.github.ieswar23.greenbasket.domain.model.Address
import io.github.ieswar23.greenbasket.domain.model.Bill
import io.github.ieswar23.greenbasket.domain.model.CartItem
import io.github.ieswar23.greenbasket.domain.model.DeliverySlot
import io.github.ieswar23.greenbasket.domain.model.Order
import io.github.ieswar23.greenbasket.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

data class PlaceOrderParams(
    val items: List<CartItem>,
    val bill: Bill,
    val slot: DeliverySlot,
    val slotLabel: String,
    val address: Address,
    val paymentMethod: PaymentMethod,
)

interface OrderRepository {
    fun observeOrders(): Flow<List<Order>>
    fun observeOrder(orderId: String): Flow<Order?>

    /** Submits the order to the API, persists it locally and clears the cart. Returns the order id. */
    suspend fun placeOrder(params: PlaceOrderParams): Result<String>

    /** Imports the customer's past orders on first launch. */
    suspend fun importHistoryIfEmpty()
}
