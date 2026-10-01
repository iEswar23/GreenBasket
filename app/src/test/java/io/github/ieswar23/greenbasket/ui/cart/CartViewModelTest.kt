package io.github.ieswar23.greenbasket.ui.cart

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.greenbasket.domain.CartCalculator
import io.github.ieswar23.greenbasket.domain.DeliverySlotProvider
import io.github.ieswar23.greenbasket.testing.FakeAddressRepository
import io.github.ieswar23.greenbasket.testing.FakeCartRepository
import io.github.ieswar23.greenbasket.testing.FakePreferencesRepository
import io.github.ieswar23.greenbasket.testing.MainDispatcherRule
import io.github.ieswar23.greenbasket.testing.TestData
import io.github.ieswar23.greenbasket.util.TimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var cart: FakeCartRepository
    private lateinit var preferences: FakePreferencesRepository
    private lateinit var viewModel: CartViewModel

    /** 10:00 local time so express delivery is available. */
    private val morning = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 1, 10, 0, 0)
    }.timeInMillis

    @Before
    fun setUp() {
        cart = FakeCartRepository()
        preferences = FakePreferencesRepository()
        viewModel = CartViewModel(
            cartRepository = cart,
            preferences = preferences,
            calculator = CartCalculator(),
            addressRepository = FakeAddressRepository(preferences),
            slotProvider = DeliverySlotProvider(),
            time = TimeProvider { morning },
        )
    }

    private fun TestScope.observeState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun `state exposes items, bill, default express slot and selected address`() = runTest {
        observeState()
        cart.seed(TestData.paneer to 2, TestData.curd to 1)

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.items.map { it.product.id }).containsExactly("paneer", "curd").inOrder()
        assertThat(state.bill.itemTotal).isEqualTo(213)
        assertThat(state.bill.deliveryFee).isEqualTo(0)
        assertThat(state.slot?.isExpress).isTrue()
        assertThat(state.address?.label).isEqualTo("Home")
    }

    @Test
    fun `incrementing updates quantity and bill`() = runTest {
        observeState()
        cart.seed(TestData.milk to 1)

        viewModel.increment(viewModel.uiState.value.items.single())

        assertThat(viewModel.uiState.value.items.single().quantity).isEqualTo(2)
        assertThat(viewModel.uiState.value.bill.itemTotal).isEqualTo(108)
    }

    @Test
    fun `removing an item emits undo event and undo restores it in place`() = runTest {
        observeState()
        cart.seed(TestData.milk to 1, TestData.paneer to 3, TestData.curd to 1)
        val paneer = viewModel.uiState.value.items[1]

        viewModel.events.test {
            viewModel.remove(paneer)
            assertThat(awaitItem()).isEqualTo(CartEvent.ItemRemoved(paneer))
        }
        assertThat(viewModel.uiState.value.items.map { it.product.id }).containsExactly("milk", "curd")

        viewModel.undoRemove(paneer)
        assertThat(viewModel.uiState.value.items.map { it.product.id }).containsExactly("milk", "paneer", "curd").inOrder()
        assertThat(viewModel.uiState.value.items[1].quantity).isEqualTo(3)
    }

    @Test
    fun `decrementing the last unit removes the line with an undo event`() = runTest {
        observeState()
        cart.seed(TestData.curd to 1)

        viewModel.events.test {
            viewModel.decrement(viewModel.uiState.value.items.single())
            assertThat(awaitItem()).isInstanceOf(CartEvent.ItemRemoved::class.java)
        }
        assertThat(viewModel.uiState.value.isEmpty).isTrue()
    }

    @Test
    fun `valid coupon is persisted and reflected in the bill`() = runTest {
        observeState()
        cart.seed(TestData.ghee to 1, TestData.paneer to 1) // ₹418

        viewModel.events.test {
            viewModel.applyCoupon("fresh50")
            assertThat(awaitItem()).isEqualTo(CartEvent.CouponApplied("FRESH50", 50))
        }
        assertThat(preferences.currentCoupon).isEqualTo("FRESH50")
        assertThat(viewModel.uiState.value.bill.couponDiscount).isEqualTo(50)
    }

    @Test
    fun `coupon below minimum order reports shortfall and is not saved`() = runTest {
        observeState()
        cart.seed(TestData.ghee to 1) // ₹329

        viewModel.events.test {
            viewModel.applyCoupon("FRESH50")
            assertThat(awaitItem()).isEqualTo(CartEvent.CouponShortfall("FRESH50", 70))
        }
        assertThat(preferences.currentCoupon).isNull()
    }

    @Test
    fun `unknown coupon code is rejected`() = runTest {
        observeState()
        cart.seed(TestData.ghee to 2)

        viewModel.events.test {
            viewModel.applyCoupon("FREEFOOD")
            assertThat(awaitItem()).isEqualTo(CartEvent.CouponInvalid)
        }
    }

    @Test
    fun `removing the coupon clears the discount`() = runTest {
        observeState()
        cart.seed(TestData.ghee to 2)
        preferences.setCouponCode("GREEN10")
        assertThat(viewModel.uiState.value.bill.couponDiscount).isEqualTo(65)

        viewModel.removeCoupon()

        assertThat(viewModel.uiState.value.bill.couponDiscount).isEqualTo(0)
        assertThat(viewModel.uiState.value.bill.appliedCoupon).isNull()
    }
}
