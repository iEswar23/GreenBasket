package io.github.ieswar23.greenbasket.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.data.repository.OrderRepository
import io.github.ieswar23.greenbasket.domain.model.Order
import io.github.ieswar23.greenbasket.ui.common.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class OrdersViewModel @Inject constructor(
    orderRepository: OrderRepository,
) : ViewModel() {

    val uiState: StateFlow<UiState<List<Order>>> = orderRepository.observeOrders()
        .map { orders -> if (orders.isEmpty()) UiState.Empty else UiState.Content(orders) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
