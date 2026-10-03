package io.github.ieswar23.greenbasket.ui.orders

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.greenbasket.domain.ReorderPlanner
import io.github.ieswar23.greenbasket.testing.FakeCartRepository
import io.github.ieswar23.greenbasket.testing.FakeCatalogRepository
import io.github.ieswar23.greenbasket.testing.FakeOrderRepository
import io.github.ieswar23.greenbasket.testing.MainDispatcherRule
import io.github.ieswar23.greenbasket.testing.TestData
import io.github.ieswar23.greenbasket.testing.TestData.curd
import io.github.ieswar23.greenbasket.testing.TestData.eggs
import io.github.ieswar23.greenbasket.testing.TestData.ghee
import io.github.ieswar23.greenbasket.testing.TestData.milk
import io.github.ieswar23.greenbasket.testing.TestData.paneer
import io.github.ieswar23.greenbasket.ui.common.OrderArgs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var catalog: FakeCatalogRepository
    private lateinit var cart: FakeCartRepository
    private lateinit var viewModel: OrderDetailViewModel

    /** Paneer has since sold out and ghee got more expensive (paid 299, now 329). */
    private val order = TestData.order(
        "GB1001", 0L, milk to 2, paneer to 1, ghee to 1, eggs to 3,
        paidPrices = mapOf("ghee" to 299),
    )

    @Before
    fun setUp() {
        catalog = FakeCatalogRepository(
            products = TestData.dairy.map { if (it.id == "paneer") it.copy(inStock = false) else it },
        )
        cart = FakeCartRepository()
        viewModel = OrderDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(OrderArgs.ORDER_ID to order.id)),
            orderRepository = FakeOrderRepository(listOf(order)),
            catalogRepository = catalog,
            cartRepository = cart,
            reorderPlanner = ReorderPlanner(),
        )
    }

    private fun TestScope.observeState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun `state flags items that can no longer be bought`() = runTest {
        observeState()

        val state = viewModel.uiState.value!!
        assertThat(state.order.id).isEqualTo("GB1001")
        assertThat(state.unavailableIds).containsExactly("paneer")
        assertThat(state.canReorder).isTrue()
    }

    @Test
    fun `reorder adds available items at current prices and reports what was skipped`() = runTest {
        observeState()

        viewModel.reorderResults.test {
            viewModel.reorder()

            val plan = awaitItem()
            assertThat(plan.addedUnits).isEqualTo(6)
            assertThat(plan.unavailableUnits).isEqualTo(1)
        }
        assertThat(cart.items.value.associate { it.product.id to it.quantity })
            .containsExactly("milk", 2, "ghee", 1, "eggs", 3)
        assertThat(cart.items.value.first { it.product.id == "ghee" }.lineTotal).isEqualTo(329)
    }

    @Test
    fun `reorder merges with what's already in the cart up to the per-item limit`() = runTest {
        observeState()
        cart.seed(milk to 9, curd to 1)

        viewModel.reorderResults.test {
            viewModel.reorder()

            val plan = awaitItem()
            assertThat(plan.cappedUnits).isEqualTo(1)
            assertThat(plan.addedUnits).isEqualTo(5)
        }
        assertThat(cart.items.value.associate { it.product.id to it.quantity })
            .containsExactly("milk", 10, "curd", 1, "ghee", 1, "eggs", 3)
    }

    @Test
    fun `reorder is disabled when nothing in the order is available`() = runTest {
        observeState()
        catalog.products.value = catalog.products.value.map { it.copy(inStock = false) }

        assertThat(viewModel.uiState.value!!.canReorder).isFalse()

        viewModel.reorderResults.test {
            viewModel.reorder()

            val plan = awaitItem()
            assertThat(plan.addedUnits).isEqualTo(0)
            assertThat(plan.unavailableUnits).isEqualTo(7)
        }
        assertThat(cart.items.value).isEmpty()
    }
}
