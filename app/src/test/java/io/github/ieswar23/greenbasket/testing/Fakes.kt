package io.github.ieswar23.greenbasket.testing

import io.github.ieswar23.greenbasket.data.preferences.PreferencesRepository
import io.github.ieswar23.greenbasket.data.repository.AddressRepository
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.data.repository.CatalogRepository
import io.github.ieswar23.greenbasket.data.repository.OrderRepository
import io.github.ieswar23.greenbasket.data.repository.PlaceOrderParams
import io.github.ieswar23.greenbasket.data.repository.SyncState
import io.github.ieswar23.greenbasket.data.repository.WishlistRepository
import io.github.ieswar23.greenbasket.domain.model.Address
import io.github.ieswar23.greenbasket.domain.model.Banner
import io.github.ieswar23.greenbasket.domain.model.CartItem
import io.github.ieswar23.greenbasket.domain.model.Category
import io.github.ieswar23.greenbasket.domain.model.Order
import io.github.ieswar23.greenbasket.domain.model.PaymentMethod
import io.github.ieswar23.greenbasket.domain.model.Product
import io.github.ieswar23.greenbasket.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FakeCatalogRepository(
    products: List<Product> = TestData.dairy,
) : CatalogRepository {

    val products = MutableStateFlow(products)
    val categories = MutableStateFlow<List<Category>>(emptyList())

    /** Every query that actually hit the "database", in order. */
    val searchedQueries = mutableListOf<String>()

    override val syncState: StateFlow<SyncState> = MutableStateFlow(SyncState.Synced)
    override suspend fun syncIfNeeded() = Unit
    override suspend fun refresh(): Result<Unit> = Result.success(Unit)
    override fun observeCategories(): Flow<List<Category>> = categories
    override suspend fun getCategory(id: String): Category? =
        Category(id = id, name = "Dairy & Eggs", subtitle = "", emoji = "🥛", tint = 0)
    override fun observeBanners(): Flow<List<Banner>> = flowOf(emptyList())
    override fun observeProducts(categoryId: String): Flow<List<Product>> =
        products.map { list -> list.filter { it.categoryId == categoryId } }
    override fun observeProduct(id: String): Flow<Product?> = products.map { list -> list.firstOrNull { it.id == id } }
    override fun observeVariants(variantGroup: String): Flow<List<Product>> =
        products.map { list -> list.filter { it.variantGroup == variantGroup } }
    override fun observeSimilar(product: Product, limit: Int): Flow<List<Product>> = flowOf(emptyList())
    override fun observeBestDeals(limit: Int): Flow<List<Product>> = flowOf(emptyList())
    override fun observeProductsByIds(ids: Collection<String>): Flow<List<Product>> =
        products.map { list -> list.filter { it.id in ids } }
    override suspend fun getProducts(ids: Collection<String>): List<Product> = products.value.filter { it.id in ids }

    override fun search(query: String): Flow<List<Product>> = flow {
        searchedQueries += query
        emit(products.value.filter { it.name.contains(query, ignoreCase = true) })
    }
}

class FakeCartRepository(
    private val catalog: List<Product> = TestData.dairy,
) : CartRepository {

    val items = MutableStateFlow<List<CartItem>>(emptyList())
    private var clock = 0L

    override fun observeCart(): Flow<List<CartItem>> = items
    override fun observeQuantities(): Flow<Map<String, Int>> =
        items.map { list -> list.associate { it.product.id to it.quantity } }
    override fun observeItemCount(): Flow<Int> = items.map { list -> list.sumOf { it.quantity } }

    override suspend fun increment(productId: String) = change(productId, +1)
    override suspend fun decrement(productId: String) = change(productId, -1)

    override suspend fun setQuantity(productId: String, quantity: Int) {
        val current = items.value.firstOrNull { it.product.id == productId }?.quantity ?: 0
        change(productId, quantity - current)
    }

    override suspend fun remove(productId: String) {
        items.value = items.value.filterNot { it.product.id == productId }
    }

    override suspend fun restore(item: CartItem) {
        items.value = (items.value.filterNot { it.product.id == item.product.id } + item).sortedBy { it.addedAt }
    }

    override suspend fun addAll(lines: Map<String, Int>) = lines.forEach { (id, qty) -> change(id, qty) }

    override suspend fun clear() {
        items.value = emptyList()
    }

    fun seed(vararg lines: Pair<Product, Int>) {
        items.value = lines.map { (product, quantity) -> CartItem(product, quantity, addedAt = ++clock) }
    }

    private fun change(productId: String, delta: Int) {
        val existing = items.value.firstOrNull { it.product.id == productId }
        val newQuantity = ((existing?.quantity ?: 0) + delta).coerceAtMost(CartRepository.MAX_QUANTITY_PER_ITEM)
        items.value = when {
            newQuantity <= 0 -> items.value.filterNot { it.product.id == productId }
            existing != null -> items.value.map { if (it.product.id == productId) it.copy(quantity = newQuantity) else it }
            else -> items.value + CartItem(catalog.first { it.id == productId }, newQuantity, addedAt = ++clock)
        }
    }
}

class FakeWishlistRepository : WishlistRepository {
    val ids = MutableStateFlow<Set<String>>(emptySet())
    private val catalog = TestData.dairy

    override fun observeIds(): Flow<Set<String>> = ids
    override fun observeProducts(): Flow<List<Product>> = ids.map { set -> catalog.filter { it.id in set } }
    override suspend fun toggle(productId: String): Boolean {
        val added = productId !in ids.value
        ids.value = if (added) ids.value + productId else ids.value - productId
        return added
    }
}

class FakePreferencesRepository : PreferencesRepository {
    private val recent = MutableStateFlow<List<String>>(emptyList())
    private val theme = MutableStateFlow(ThemeMode.SYSTEM)
    private val addressId = MutableStateFlow<Long?>(null)
    private val slotId = MutableStateFlow<String?>(null)
    private val coupon = MutableStateFlow<String?>(null)
    private val payment = MutableStateFlow(PaymentMethod.UPI)

    override val recentSearches: Flow<List<String>> = recent
    override val themeMode: Flow<ThemeMode> = theme
    override val selectedAddressId: Flow<Long?> = addressId
    override val deliverySlotId: Flow<String?> = slotId
    override val couponCode: Flow<String?> = coupon
    override val paymentMethod: Flow<PaymentMethod> = payment

    val currentCoupon: String? get() = coupon.value
    val currentRecent: List<String> get() = recent.value

    override suspend fun addRecentSearch(query: String) {
        recent.value = PreferencesRepository.mergeRecent(recent.value, query)
    }
    override suspend fun removeRecentSearch(query: String) {
        recent.value = recent.value - query
    }
    override suspend fun clearRecentSearches() {
        recent.value = emptyList()
    }
    override suspend fun setThemeMode(mode: ThemeMode) {
        theme.value = mode
    }
    override suspend fun setSelectedAddressId(id: Long) {
        addressId.value = id
    }
    override suspend fun setDeliverySlotId(id: String) {
        slotId.value = id
    }
    override suspend fun setCouponCode(code: String?) {
        coupon.value = code
    }
    override suspend fun setPaymentMethod(method: PaymentMethod) {
        payment.value = method
    }
}

class FakeAddressRepository(
    private val preferences: PreferencesRepository,
) : AddressRepository {
    private val addresses = MutableStateFlow(
        listOf(
            Address(1, "Home", "Aarav Mehta", "+91 98200 41736", "Flat 1204, Lotus Residency", "Powai", "Mumbai", "400076"),
        ),
    )

    override fun observeAddresses(): Flow<List<Address>> = addresses
    override fun observeSelected(): Flow<Address?> =
        combine(addresses, preferences.selectedAddressId) { list, id -> list.firstOrNull { it.id == id } ?: list.firstOrNull() }
    override suspend fun select(addressId: Long) = preferences.setSelectedAddressId(addressId)
    override suspend fun add(address: Address): Long {
        val id = (addresses.value.maxOfOrNull { it.id } ?: 0) + 1
        addresses.value = addresses.value + address.copy(id = id)
        return id
    }
    override suspend fun seedIfEmpty() = Unit
}

class FakeOrderRepository(orders: List<Order> = emptyList()) : OrderRepository {
    val orders = MutableStateFlow(orders)

    override fun observeOrders(): Flow<List<Order>> = orders
    override fun observeOrder(orderId: String): Flow<Order?> = orders.map { list -> list.firstOrNull { it.id == orderId } }
    override suspend fun placeOrder(params: PlaceOrderParams): Result<String> = Result.failure(UnsupportedOperationException())
    override suspend fun importHistoryIfEmpty() = Unit
}
