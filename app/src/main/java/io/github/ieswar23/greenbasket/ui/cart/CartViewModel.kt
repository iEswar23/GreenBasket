package io.github.ieswar23.greenbasket.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.data.preferences.PreferencesRepository
import io.github.ieswar23.greenbasket.data.repository.AddressRepository
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.domain.CartCalculator
import io.github.ieswar23.greenbasket.domain.CouponCatalog
import io.github.ieswar23.greenbasket.domain.DeliverySlotProvider
import io.github.ieswar23.greenbasket.domain.model.Address
import io.github.ieswar23.greenbasket.domain.model.Bill
import io.github.ieswar23.greenbasket.domain.model.CartItem
import io.github.ieswar23.greenbasket.domain.model.Coupon
import io.github.ieswar23.greenbasket.domain.model.DeliverySlot
import io.github.ieswar23.greenbasket.util.TimeProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartUiState(
    val isLoading: Boolean = true,
    val items: List<CartItem> = emptyList(),
    val bill: Bill = Bill.EMPTY,
    val slot: DeliverySlot? = null,
    val address: Address? = null,
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
}

sealed interface CartEvent {
    data class ItemRemoved(val item: CartItem) : CartEvent
    data class CouponApplied(val code: String, val discount: Int) : CartEvent
    data class CouponShortfall(val code: String, val shortfall: Int) : CartEvent
    data object CouponInvalid : CartEvent
}

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val preferences: PreferencesRepository,
    private val calculator: CartCalculator,
    addressRepository: AddressRepository,
    slotProvider: DeliverySlotProvider,
    time: TimeProvider,
) : ViewModel() {

    private val _events = Channel<CartEvent>(Channel.BUFFERED)
    val events: Flow<CartEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<CartUiState> = combine(
        cartRepository.observeCart(),
        preferences.couponCode,
        preferences.deliverySlotId,
        addressRepository.observeSelected(),
    ) { items, couponCode, slotId, address ->
        CartUiState(
            isLoading = false,
            items = items,
            bill = calculator.calculate(items, CouponCatalog.find(couponCode)),
            slot = slotProvider.resolve(slotId, time.now()),
            address = address,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    val coupons: List<Coupon> get() = CouponCatalog.all

    fun increment(item: CartItem) {
        viewModelScope.launch { cartRepository.increment(item.product.id) }
    }

    /** Decrementing the last unit removes the line, which offers an undo like a swipe does. */
    fun decrement(item: CartItem) {
        if (item.quantity <= 1) {
            remove(item)
        } else {
            viewModelScope.launch { cartRepository.decrement(item.product.id) }
        }
    }

    fun remove(item: CartItem) {
        viewModelScope.launch {
            cartRepository.remove(item.product.id)
            _events.send(CartEvent.ItemRemoved(item))
        }
    }

    fun undoRemove(item: CartItem) {
        viewModelScope.launch { cartRepository.restore(item) }
    }

    fun applyCoupon(code: String) {
        val coupon = CouponCatalog.find(code)
        if (coupon == null) {
            _events.trySend(CartEvent.CouponInvalid)
            return
        }
        val itemTotal = uiState.value.bill.itemTotal
        val shortfall = coupon.minOrder - itemTotal
        if (shortfall > 0) {
            _events.trySend(CartEvent.CouponShortfall(coupon.code, shortfall))
            return
        }
        viewModelScope.launch {
            preferences.setCouponCode(coupon.code)
            _events.send(CartEvent.CouponApplied(coupon.code, calculator.discountFor(coupon, itemTotal)))
        }
    }

    fun removeCoupon() {
        viewModelScope.launch { preferences.setCouponCode(null) }
    }
}
