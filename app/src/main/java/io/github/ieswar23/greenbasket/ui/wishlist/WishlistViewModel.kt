package io.github.ieswar23.greenbasket.ui.wishlist

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.data.repository.WishlistRepository
import io.github.ieswar23.greenbasket.domain.model.ProductItem
import io.github.ieswar23.greenbasket.domain.withUserState
import io.github.ieswar23.greenbasket.ui.common.ProductActionsViewModel
import io.github.ieswar23.greenbasket.ui.common.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WishlistViewModel @Inject constructor(
    cartRepository: CartRepository,
    wishlistRepository: WishlistRepository,
) : ProductActionsViewModel(cartRepository, wishlistRepository) {

    val uiState: StateFlow<UiState<List<ProductItem>>> = wishlistRepository.observeProducts()
        .withUserState(cartRepository.observeQuantities(), wishlistRepository.observeIds())
        .map { items -> if (items.isEmpty()) UiState.Empty else UiState.Content(items) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
