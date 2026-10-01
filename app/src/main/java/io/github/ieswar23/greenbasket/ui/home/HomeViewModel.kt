package io.github.ieswar23.greenbasket.ui.home

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.data.preferences.PreferencesRepository
import io.github.ieswar23.greenbasket.data.repository.AddressRepository
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.data.repository.CatalogRepository
import io.github.ieswar23.greenbasket.data.repository.OrderRepository
import io.github.ieswar23.greenbasket.data.repository.SyncState
import io.github.ieswar23.greenbasket.data.repository.WishlistRepository
import io.github.ieswar23.greenbasket.domain.DeliverySlotProvider
import io.github.ieswar23.greenbasket.domain.model.Address
import io.github.ieswar23.greenbasket.domain.model.Banner
import io.github.ieswar23.greenbasket.domain.model.Category
import io.github.ieswar23.greenbasket.domain.model.DeliverySlot
import io.github.ieswar23.greenbasket.domain.model.ProductItem
import io.github.ieswar23.greenbasket.domain.withUserState
import io.github.ieswar23.greenbasket.ui.common.ProductActionsViewModel
import io.github.ieswar23.greenbasket.ui.common.UiEvent
import io.github.ieswar23.greenbasket.util.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Content(
        val banners: List<Banner>,
        val categories: List<Category>,
        val bestDeals: List<ProductItem>,
        val buyAgain: List<ProductItem>,
    ) : HomeUiState
}

data class HomeHeader(val address: Address?, val slot: DeliverySlot)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val orderRepository: OrderRepository,
    cartRepository: CartRepository,
    wishlistRepository: WishlistRepository,
    addressRepository: AddressRepository,
    preferences: PreferencesRepository,
    slotProvider: DeliverySlotProvider,
    time: TimeProvider,
) : ProductActionsViewModel(cartRepository, wishlistRepository) {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val quantities = cartRepository.observeQuantities()
    private val wishlist = wishlistRepository.observeIds()

    val uiState: StateFlow<HomeUiState> = combine(
        catalogRepository.syncState,
        catalogRepository.observeBanners(),
        catalogRepository.observeCategories(),
        catalogRepository.observeBestDeals().withUserState(quantities, wishlist),
        catalogRepository.observeBuyAgain().withUserState(quantities, wishlist),
    ) { sync, banners, categories, deals, buyAgain ->
        when {
            categories.isNotEmpty() -> HomeUiState.Content(banners, categories, deals, buyAgain)
            sync is SyncState.Failed -> HomeUiState.Error(sync.message)
            else -> HomeUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    val header: StateFlow<HomeHeader?> = combine(
        addressRepository.observeSelected(),
        preferences.deliverySlotId,
    ) { address, slotId ->
        HomeHeader(address, slotProvider.resolve(slotId, time.now()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            catalogRepository.refresh()
                .onSuccess { orderRepository.importHistoryIfEmpty() }
                .onFailure { sendEvent(UiEvent.Message(R.string.error_refresh_failed)) }
            _isRefreshing.value = false
        }
    }
}
