package io.github.ieswar23.greenbasket.ui.productlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.data.repository.CatalogRepository
import io.github.ieswar23.greenbasket.data.repository.WishlistRepository
import io.github.ieswar23.greenbasket.domain.ProductQueryEngine
import io.github.ieswar23.greenbasket.domain.model.Category
import io.github.ieswar23.greenbasket.domain.model.FilterOptions
import io.github.ieswar23.greenbasket.domain.model.ProductFilter
import io.github.ieswar23.greenbasket.domain.model.ProductItem
import io.github.ieswar23.greenbasket.domain.model.SortOption
import io.github.ieswar23.greenbasket.domain.toItems
import io.github.ieswar23.greenbasket.ui.common.ProductActionsViewModel
import io.github.ieswar23.greenbasket.ui.common.CategoryArgs
import io.github.ieswar23.greenbasket.ui.common.UiEvent
import io.github.ieswar23.greenbasket.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductListUiState(
    val content: UiState<List<ProductItem>> = UiState.Loading,
    val sort: SortOption = SortOption.RELEVANCE,
    val filter: ProductFilter = ProductFilter.NONE,
    val totalCount: Int = 0,
) {
    val resultCount: Int get() = (content as? UiState.Content)?.data?.size ?: 0
}

@HiltViewModel
class ProductListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    cartRepository: CartRepository,
    wishlistRepository: WishlistRepository,
) : ProductActionsViewModel(cartRepository, wishlistRepository) {

    private val categoryId: String = checkNotNull(savedStateHandle[CategoryArgs.CATEGORY_ID]) {
        "ProductListFragment requires a categoryId argument"
    }

    private val sort = MutableStateFlow(SortOption.RELEVANCE)
    private val filter = MutableStateFlow(ProductFilter.NONE)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val products = catalogRepository.observeProducts(categoryId)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val category: StateFlow<Category?> = flow { emit(catalogRepository.getCategory(categoryId)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val filterOptions: StateFlow<FilterOptions> = products
        .map { ProductQueryEngine.optionsFor(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FilterOptions.EMPTY)

    val uiState: StateFlow<ProductListUiState> = combine(
        products,
        cartRepository.observeQuantities(),
        wishlistRepository.observeIds(),
        sort,
        filter,
    ) { all, quantities, wishlist, sortOption, productFilter ->
        val visible = ProductQueryEngine.apply(all, sortOption, productFilter)
        ProductListUiState(
            content = if (visible.isEmpty()) UiState.Empty else UiState.Content(visible.toItems(quantities, wishlist)),
            sort = sortOption,
            filter = productFilter,
            totalCount = all.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductListUiState())

    fun applySortAndFilter(sortOption: SortOption, productFilter: ProductFilter) {
        sort.value = sortOption
        filter.value = productFilter
    }

    fun setSort(sortOption: SortOption) {
        sort.value = sortOption
    }

    fun setFilter(productFilter: ProductFilter) {
        filter.value = productFilter
    }

    fun clearFilters() {
        filter.value = ProductFilter.NONE
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            catalogRepository.refresh().onFailure { sendEvent(UiEvent.Message(R.string.error_refresh_failed)) }
            _isRefreshing.value = false
        }
    }
}
