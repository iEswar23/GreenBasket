package io.github.ieswar23.greenbasket.ui.home

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.greenbasket.domain.BuyAgainRanker
import io.github.ieswar23.greenbasket.domain.DeliverySlotProvider
import io.github.ieswar23.greenbasket.domain.model.Category
import io.github.ieswar23.greenbasket.domain.model.OrderStatus
import io.github.ieswar23.greenbasket.testing.FakeAddressRepository
import io.github.ieswar23.greenbasket.testing.FakeCartRepository
import io.github.ieswar23.greenbasket.testing.FakeCatalogRepository
import io.github.ieswar23.greenbasket.testing.FakeOrderRepository
import io.github.ieswar23.greenbasket.testing.FakePreferencesRepository
import io.github.ieswar23.greenbasket.testing.FakeWishlistRepository
import io.github.ieswar23.greenbasket.testing.MainDispatcherRule
import io.github.ieswar23.greenbasket.testing.TestData
import io.github.ieswar23.greenbasket.testing.TestData.curd
import io.github.ieswar23.greenbasket.testing.TestData.eggs
import io.github.ieswar23.greenbasket.testing.TestData.ghee
import io.github.ieswar23.greenbasket.testing.TestData.milk
import io.github.ieswar23.greenbasket.util.TimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = 1_790_000_000_000L
    private val time = TimeProvider { now }

    private lateinit var catalog: FakeCatalogRepository
    private lateinit var orders: FakeOrderRepository
    private lateinit var cart: FakeCartRepository
    private lateinit var viewModel: HomeViewModel

    private fun daysAgo(days: Int): Long = now - TimeUnit.DAYS.toMillis(days.toLong())

    @Before
    fun setUp() {
        catalog = FakeCatalogRepository().apply {
            categories.value = listOf(Category(id = "dairy_eggs", name = "Dairy & Eggs", subtitle = "", emoji = "🥛", tint = 0))
        }
        orders = FakeOrderRepository()
        cart = FakeCartRepository()
        val preferences = FakePreferencesRepository()
        viewModel = HomeViewModel(
            catalogRepository = catalog,
            orderRepository = orders,
            cartRepository = cart,
            wishlistRepository = FakeWishlistRepository(),
            addressRepository = FakeAddressRepository(preferences),
            preferences = preferences,
            slotProvider = DeliverySlotProvider(),
            time = time,
            buyAgainRanker = BuyAgainRanker(time),
        )
    }

    private fun TestScope.observeState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private val content: HomeUiState.Content get() = viewModel.uiState.value as HomeUiState.Content

    private fun seedHistory() {
        orders.orders.value = listOf(
            TestData.order("o1", daysAgo(2), milk to 2, eggs to 1),
            TestData.order("o2", daysAgo(9), milk to 1, curd to 1),
            TestData.order("o3", daysAgo(16), milk to 1, curd to 1, ghee to 1),
            TestData.order("o4", daysAgo(1), TestData.cheese to 5, status = OrderStatus.CANCELLED),
        )
    }

    @Test
    fun `buy again is empty without order history`() = runTest {
        observeState()

        assertThat(content.buyAgain).isEmpty()
    }

    @Test
    fun `buy again ranks order history and carries cart quantities`() = runTest {
        observeState()
        seedHistory()
        cart.seed(curd to 2)

        val shelf = content.buyAgain
        assertThat(shelf.map { it.product.id }).containsExactly("milk", "curd", "eggs", "ghee").inOrder()
        assertThat(shelf.first { it.product.id == "curd" }.quantity).isEqualTo(2)
        assertThat(shelf.first { it.product.id == "milk" }.quantity).isEqualTo(0)
    }

    @Test
    fun `buy again drops products that go out of stock`() = runTest {
        observeState()
        seedHistory()

        catalog.products.value = catalog.products.value.map { if (it.id == "milk") it.copy(inStock = false) else it }

        assertThat(content.buyAgain.map { it.product.id }).containsExactly("curd", "eggs", "ghee").inOrder()
    }

    @Test
    fun `adding from the buy again shelf updates the card's stepper`() = runTest {
        observeState()
        seedHistory()

        viewModel.increment(content.buyAgain.first().product)
        viewModel.increment(content.buyAgain.first().product)

        assertThat(content.buyAgain.first().quantity).isEqualTo(2)
    }

    @Test
    fun `sold out products can't be added`() = runTest {
        observeState()

        viewModel.increment(milk.copy(inStock = false))

        assertThat(cart.items.value).isEmpty()
    }
}
