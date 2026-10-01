package io.github.ieswar23.greenbasket.ui.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.data.repository.OrderRepository
import io.github.ieswar23.greenbasket.domain.model.Order
import io.github.ieswar23.greenbasket.ui.common.OrderArgs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    orderRepository: OrderRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle[OrderArgs.ORDER_ID])

    val order: StateFlow<Order?> = orderRepository.observeOrder(orderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _reordered = Channel<Int>(Channel.BUFFERED)

    /** Emits the number of units added to the cart after a re-order. */
    val reordered: Flow<Int> = _reordered.receiveAsFlow()

    fun reorder() {
        val current = order.value ?: return
        viewModelScope.launch {
            cartRepository.addAll(current.items.associate { it.productId to it.quantity })
            _reordered.send(current.itemCount)
        }
    }
}
