package io.github.ieswar23.greenbasket.ui.search

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.data.preferences.PreferencesRepository
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.data.repository.CatalogRepository
import io.github.ieswar23.greenbasket.data.repository.WishlistRepository
import io.github.ieswar23.greenbasket.domain.model.ProductItem
import io.github.ieswar23.greenbasket.domain.withUserState
import io.github.ieswar23.greenbasket.ui.common.ProductActionsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchResults {
    /** No query yet: show recent + trending searches. */
    data object Idle : SearchResults
    data object Loading : SearchResults
    data class Empty(val query: String) : SearchResults
    data class Found(val query: String, val items: List<ProductItem>) : SearchResults
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val preferences: PreferencesRepository,
    cartRepository: CartRepository,
    wishlistRepository: WishlistRepository,
) : ProductActionsViewModel(cartRepository, wishlistRepository) {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val quantities = cartRepository.observeQuantities()
    private val wishlist = wishlistRepository.observeIds()

    /**
     * Results are driven by a debounced query: typing "m-i-l-k" quickly triggers a single search.
     * Clearing the field resets immediately (no debounce for blank input).
     */
    val results: StateFlow<SearchResults> = _query
        .map { it.trim() }
        .debounce { text -> if (text.isEmpty()) 0L else DEBOUNCE_MS }
        .distinctUntilChanged()
        .flatMapLatest { text ->
            if (text.length < MIN_QUERY_LENGTH) {
                flowOf(SearchResults.Idle)
            } else {
                catalogRepository.search(text)
                    .withUserState(quantities, wishlist)
                    .map { items ->
                        if (items.isEmpty()) SearchResults.Empty(text) else SearchResults.Found(text, items)
                    }
                    .onStart { emit(SearchResults.Loading) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults.Idle)

    val recentSearches: StateFlow<List<String>> = preferences.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChanged(text: String) {
        _query.value = text
    }

    /** Called on IME "search" or when the user opens a product from the results. */
    fun commitQuery() {
        val text = _query.value.trim()
        if (text.length < MIN_QUERY_LENGTH) return
        viewModelScope.launch { preferences.addRecentSearch(text) }
    }

    fun onSuggestionSelected(text: String) {
        _query.value = text
        commitQuery()
    }

    fun removeRecent(text: String) {
        viewModelScope.launch { preferences.removeRecentSearch(text) }
    }

    fun clearRecent() {
        viewModelScope.launch { preferences.clearRecentSearches() }
    }

    companion object {
        const val DEBOUNCE_MS = 300L
        const val MIN_QUERY_LENGTH = 2
    }
}
