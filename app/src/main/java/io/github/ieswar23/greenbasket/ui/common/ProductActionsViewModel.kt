package io.github.ieswar23.greenbasket.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.data.repository.WishlistRepository
import io.github.ieswar23.greenbasket.domain.model.Product
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Shared cart / wishlist actions for every screen that renders product cards.
 */
abstract class ProductActionsViewModel(
    protected val cartRepository: CartRepository,
    protected val wishlistRepository: WishlistRepository,
) : ViewModel() {

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    fun increment(product: Product) {
        if (!product.inStock) return
        viewModelScope.launch { cartRepository.increment(product.id) }
    }

    fun decrement(product: Product) {
        viewModelScope.launch { cartRepository.decrement(product.id) }
    }

    fun toggleWishlist(product: Product) {
        viewModelScope.launch {
            val added = wishlistRepository.toggle(product.id)
            sendEvent(
                UiEvent.Message(
                    if (added) R.string.wishlist_added else R.string.wishlist_removed,
                    listOf(product.name),
                ),
            )
        }
    }

    protected fun sendEvent(event: UiEvent) {
        _events.trySend(event)
    }
}
