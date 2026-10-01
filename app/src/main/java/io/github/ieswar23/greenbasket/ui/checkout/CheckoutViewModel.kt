package io.github.ieswar23.greenbasket.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.data.preferences.PreferencesRepository
import io.github.ieswar23.greenbasket.data.repository.AddressRepository
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.data.repository.OrderRepository
import io.github.ieswar23.greenbasket.data.repository.PlaceOrderParams
import io.github.ieswar23.greenbasket.domain.CartCalculator
import io.github.ieswar23.greenbasket.domain.CouponCatalog
import io.github.ieswar23.greenbasket.domain.DeliverySlotProvider
import io.github.ieswar23.greenbasket.domain.model.Address
import io.github.ieswar23.greenbasket.domain.model.Bill
import io.github.ieswar23.greenbasket.domain.model.CartItem
import io.github.ieswar23.greenbasket.domain.model.DeliverySlot
import io.github.ieswar23.greenbasket.domain.model.PaymentMethod
import io.github.ieswar23.greenbasket.util.TimeProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckoutUiState(
    val isLoading: Boolean = true,
    val items: List<CartItem> = emptyList(),
    val bill: Bill = Bill.EMPTY,
    val slot: DeliverySlot? = null,
    val address: Address? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val isPlacing: Boolean = false,
) {
    val canPlaceOrder: Boolean get() = !isLoading && !isPlacing && items.isNotEmpty() && address != null && slot != null
}

sealed interface CheckoutEvent {
    data class OrderPlaced(val orderId: String) : CheckoutEvent
    data class Failed(val message: String?) : CheckoutEvent
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    cartRepository: CartRepository,
    addressRepository: AddressRepository,
    private val orderRepository: OrderRepository,
    private val preferences: PreferencesRepository,
    calculator: CartCalculator,
    slotProvider: DeliverySlotProvider,
    time: TimeProvider,
) : ViewModel() {

    private val isPlacing = MutableStateFlow(false)
    private val _events = Channel<CheckoutEvent>(Channel.BUFFERED)
    val events: Flow<CheckoutEvent> = _events.receiveAsFlow()

    private val baseState = combine(
        cartRepository.observeCart(),
        preferences.couponCode,
        preferences.deliverySlotId,
        addressRepository.observeSelected(),
        preferences.paymentMethod,
    ) { items, coupon, slotId, address, payment ->
        CheckoutUiState(
            isLoading = false,
            items = items,
            bill = calculator.calculate(items, CouponCatalog.find(coupon)),
            slot = slotProvider.resolve(slotId, time.now()),
            address = address,
            paymentMethod = payment,
        )
    }

    val uiState: StateFlow<CheckoutUiState> = combine(baseState, isPlacing) { state, placing ->
        state.copy(isPlacing = placing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckoutUiState())

    fun selectPayment(method: PaymentMethod) {
        viewModelScope.launch { preferences.setPaymentMethod(method) }
    }

    fun placeOrder(slotLabel: String) {
        val state = uiState.value
        if (!state.canPlaceOrder) return
        val slot = state.slot ?: return
        val address = state.address ?: return
        isPlacing.value = true
        viewModelScope.launch {
            orderRepository.placeOrder(
                PlaceOrderParams(
                    items = state.items,
                    bill = state.bill,
                    slot = slot,
                    slotLabel = slotLabel,
                    address = address,
                    paymentMethod = state.paymentMethod,
                ),
            ).onSuccess { orderId ->
                _events.send(CheckoutEvent.OrderPlaced(orderId))
            }.onFailure { error ->
                isPlacing.value = false
                _events.send(CheckoutEvent.Failed(error.message))
            }
        }
    }
}
