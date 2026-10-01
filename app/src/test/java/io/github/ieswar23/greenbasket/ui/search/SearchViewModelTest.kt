package io.github.ieswar23.greenbasket.ui.search

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.greenbasket.testing.FakeCartRepository
import io.github.ieswar23.greenbasket.testing.FakeCatalogRepository
import io.github.ieswar23.greenbasket.testing.FakePreferencesRepository
import io.github.ieswar23.greenbasket.testing.FakeWishlistRepository
import io.github.ieswar23.greenbasket.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    /** A StandardTestDispatcher gives the test full control over virtual time for the debounce. */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private lateinit var catalog: FakeCatalogRepository
    private lateinit var preferences: FakePreferencesRepository
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        catalog = FakeCatalogRepository()
        preferences = FakePreferencesRepository()
        viewModel = SearchViewModel(
            catalogRepository = catalog,
            preferences = preferences,
            cartRepository = FakeCartRepository(),
            wishlistRepository = FakeWishlistRepository(),
        )
    }

    private fun TestScope.observe() {
        backgroundScope.launch { viewModel.results.collect {} }
        backgroundScope.launch { viewModel.recentSearches.collect {} }
        runCurrent()
    }

    @Test
    fun `rapid typing triggers a single search after the debounce window`() = runTest(mainDispatcherRule.dispatcher) {
        observe()

        listOf("p", "pa", "pan", "pane", "paneer").forEach { text ->
            viewModel.onQueryChanged(text)
            advanceTimeBy(100)
        }
        assertThat(catalog.searchedQueries).isEmpty()
        assertThat(viewModel.results.value).isEqualTo(SearchResults.Idle)

        advanceTimeBy(SearchViewModel.DEBOUNCE_MS)
        runCurrent()

        assertThat(catalog.searchedQueries).containsExactly("paneer")
        val results = viewModel.results.value as SearchResults.Found
        assertThat(results.query).isEqualTo("paneer")
        assertThat(results.items.map { it.product.id }).containsExactly("paneer")
    }

    @Test
    fun `nothing is searched before the debounce elapses`() = runTest(mainDispatcherRule.dispatcher) {
        observe()

        viewModel.onQueryChanged("milk")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MS - 1)
        runCurrent()
        assertThat(catalog.searchedQueries).isEmpty()

        advanceTimeBy(2)
        runCurrent()
        assertThat(catalog.searchedQueries).containsExactly("milk")
    }

    @Test
    fun `queries shorter than the minimum length stay idle`() = runTest(mainDispatcherRule.dispatcher) {
        observe()

        viewModel.onQueryChanged("m")
        advanceTimeBy(1_000)
        runCurrent()

        assertThat(viewModel.results.value).isEqualTo(SearchResults.Idle)
        assertThat(catalog.searchedQueries).isEmpty()
    }

    @Test
    fun `unknown term produces an empty result`() = runTest(mainDispatcherRule.dispatcher) {
        observe()

        viewModel.onQueryChanged("  quinoa ")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MS + 1)
        runCurrent()

        assertThat(viewModel.results.value).isEqualTo(SearchResults.Empty("quinoa"))
    }

    @Test
    fun `clearing the query returns to idle without waiting for the debounce`() = runTest(mainDispatcherRule.dispatcher) {
        observe()
        viewModel.onQueryChanged("curd")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MS + 1)
        runCurrent()
        assertThat(viewModel.results.value).isInstanceOf(SearchResults.Found::class.java)

        viewModel.onQueryChanged("")
        runCurrent()

        assertThat(viewModel.results.value).isEqualTo(SearchResults.Idle)
    }

    @Test
    fun `committed queries and suggestions are saved to recent searches`() = runTest(mainDispatcherRule.dispatcher) {
        observe()

        viewModel.onQueryChanged("ghee")
        viewModel.commitQuery()
        viewModel.onSuggestionSelected("Eggs")
        runCurrent()

        assertThat(preferences.currentRecent).containsExactly("Eggs", "ghee").inOrder()
        assertThat(viewModel.recentSearches.value).containsExactly("Eggs", "ghee").inOrder()

        viewModel.clearRecent()
        runCurrent()
        assertThat(viewModel.recentSearches.value).isEmpty()
    }
}
