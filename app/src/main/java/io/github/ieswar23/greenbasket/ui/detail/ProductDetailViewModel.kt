package io.github.ieswar23.greenbasket.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.data.preferences.PreferencesRepository
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.data.repository.CatalogRepository
import io.github.ieswar23.greenbasket.data.repository.WishlistRepository
import io.github.ieswar23.greenbasket.domain.DeliverySlotProvider
import io.github.ieswar23.greenbasket.domain.model.DeliverySlot
import io.github.ieswar23.greenbasket.domain.model.Product
import io.github.ieswar23.greenbasket.domain.model.ProductItem
import io.github.ieswar23.greenbasket.domain.withUserState
import io.github.ieswar23.greenbasket.ui.common.ProductActionsViewModel
import io.github.ieswar23.greenbasket.ui.common.ProductArgs
import io.github.ieswar23.greenbasket.util.TimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState
    data object NotFound : ProductDetailUiState
    data class Content(
        val product: Product,
        val variants: List<Product>,
        val similar: List<ProductItem>,
        val quantity: Int,
        val isWishlisted: Boolean,
        val cartItemCount: Int,
        val slot: DeliverySlot,
    ) : ProductDetailUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    catalogRepository: CatalogRepository,
    cartRepository: CartRepository,
    wishlistRepository: WishlistRepository,
    preferences: PreferencesRepository,
    slotProvider: DeliverySlotProvider,
    time: TimeProvider,
) : ProductActionsViewModel(cartRepository, wishlistRepository) {

    private val productId = savedStateHandle.getStateFlow(ProductArgs.PRODUCT_ID, "")

    private val quantities = cartRepository.observeQuantities()
    private val wishlist = wishlistRepository.observeIds()
    private val slot = preferences.deliverySlotId.map { slotProvider.resolve(it, time.now()) }

    val uiState: StateFlow<ProductDetailUiState> = productId
        .flatMapLatest { id -> catalogRepository.observeProduct(id) }
        .flatMapLatest { product ->
            if (product == null) {
                flowOf(ProductDetailUiState.NotFound)
            } else {
                combine(
                    catalogRepository.observeVariants(product.variantGroup),
                    catalogRepository.observeSimilar(product).withUserState(quantities, wishlist),
                    quantities,
                    wishlist,
                    slot,
                ) { variants, similar, cart, liked, deliverySlot ->
                    ProductDetailUiState.Content(
                        product = product,
                        variants = variants,
                        similar = similar,
                        quantity = cart[product.id] ?: 0,
                        isWishlisted = product.id in liked,
                        cartItemCount = cart.values.sum(),
                        slot = deliverySlot,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailUiState.Loading)

    /** Switches the screen to another pack size of the same product without adding a back-stack entry. */
    fun selectVariant(productId: String) {
        savedStateHandle[ProductArgs.PRODUCT_ID] = productId
    }
}
