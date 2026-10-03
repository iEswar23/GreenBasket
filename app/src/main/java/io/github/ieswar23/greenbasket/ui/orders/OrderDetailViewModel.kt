package io.github.ieswar23.greenbasket.ui.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.data.repository.CatalogRepository
import io.github.ieswar23.greenbasket.data.repository.OrderRepository
import io.github.ieswar23.greenbasket.domain.ReorderPlan
import io.github.ieswar23.greenbasket.domain.ReorderPlanner
import io.github.ieswar23.greenbasket.domain.model.Order
import io.github.ieswar23.greenbasket.ui.common.OrderArgs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderDetailUiState(
    val order: Order,
    /** Products from this order that can't be bought right now. */
    val unavailableIds: Set<String> = emptySet(),
) {
    val canReorder: Boolean get() = order.items.any { it.productId !in unavailableIds }
}

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    orderRepository: OrderRepository,
    private val catalogRepository: CatalogRepository,
    private val cartRepository: CartRepository,
    private val reorderPlanner: ReorderPlanner,
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle[OrderArgs.ORDER_ID])

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<OrderDetailUiState?> = orderRepository.observeOrder(orderId)
        .flatMapLatest { order ->
            if (order == null) {
                flowOf(null)
            } else {
                val ids = order.items.map { it.productId }.toSet()
                catalogRepository.observeProductsByIds(ids)
                    .map { catalog ->
                        val available = catalog.filter { it.inStock }.map { it.id }.toSet()
                        OrderDetailUiState(order, unavailableIds = ids - available)
                    }
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _reorderResults = Channel<ReorderPlan>(Channel.BUFFERED)

    /** Emits what "Reorder" put in the cart (and what it had to skip). */
    val reorderResults: Flow<ReorderPlan> = _reorderResults.receiveAsFlow()

    private var reorderJob: Job? = null

    /** Adds every still-available item of this order to the cart, respecting the per-item limit. */
    fun reorder() {
        val order = uiState.value?.order ?: return
        if (reorderJob?.isActive == true) return
        reorderJob = viewModelScope.launch {
            val catalog = catalogRepository.getProducts(order.items.map { it.productId })
            val cart = cartRepository.observeQuantities().first()
            val plan = reorderPlanner.plan(order, catalog, cart, CartRepository.MAX_QUANTITY_PER_ITEM)
            if (plan.cartAdditions.isNotEmpty()) cartRepository.addAll(plan.cartAdditions)
            _reorderResults.send(plan)
        }
    }
}
