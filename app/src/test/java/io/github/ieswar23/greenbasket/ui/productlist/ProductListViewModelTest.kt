package io.github.ieswar23.greenbasket.ui.productlist

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.greenbasket.domain.model.ProductFilter
import io.github.ieswar23.greenbasket.domain.model.ProductItem
import io.github.ieswar23.greenbasket.domain.model.SortOption
import io.github.ieswar23.greenbasket.testing.FakeCartRepository
import io.github.ieswar23.greenbasket.testing.FakeCatalogRepository
import io.github.ieswar23.greenbasket.testing.FakeWishlistRepository
import io.github.ieswar23.greenbasket.testing.MainDispatcherRule
import io.github.ieswar23.greenbasket.testing.TestData
import io.github.ieswar23.greenbasket.ui.common.CategoryArgs
import io.github.ieswar23.greenbasket.ui.common.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var cart: FakeCartRepository
    private lateinit var viewModel: ProductListViewModel

    @Before
    fun setUp() {
        cart = FakeCartRepository()
        viewModel = ProductListViewModel(
            savedStateHandle = SavedStateHandle(mapOf(CategoryArgs.CATEGORY_ID to "dairy_eggs")),
            catalogRepository = FakeCatalogRepository(),
            cartRepository = cart,
            wishlistRepository = FakeWishlistRepository(),
        )
    }

    private fun TestScope.observe() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.filterOptions.collect {} }
    }

    private fun visibleIds(): List<String> {
        val content = viewModel.uiState.value.content
        return (content as UiState.Content<List<ProductItem>>).data.map { it.product.id }
    }

    @Test
    fun `relevance keeps catalog order`() = runTest {
        observe()

        assertThat(visibleIds()).isEqualTo(TestData.dairy.map { it.id })
        assertThat(viewModel.uiState.value.totalCount).isEqualTo(6)
    }

    @Test
    fun `sorting by price low to high and high to low`() = runTest {
        observe()

        viewModel.setSort(SortOption.PRICE_LOW_TO_HIGH)
        assertThat(visibleIds()).containsExactly("curd", "milk", "eggs", "paneer", "cheese", "ghee").inOrder()

        viewModel.setSort(SortOption.PRICE_HIGH_TO_LOW)
        assertThat(visibleIds().first()).isEqualTo("ghee")
        assertThat(viewModel.uiState.value.sort).isEqualTo(SortOption.PRICE_HIGH_TO_LOW)
    }

    @Test
    fun `sorting by discount puts the biggest percentage first`() = runTest {
        observe()

        viewModel.setSort(SortOption.DISCOUNT)

        // eggs 17%, curd 12%, ghee 9%, cheese 9%, paneer 6%, milk 0%
        assertThat(visibleIds()).containsExactly("eggs", "curd", "cheese", "ghee", "paneer", "milk").inOrder()
    }

    @Test
    fun `brand and discount filters combine`() = runTest {
        observe()

        viewModel.applySortAndFilter(SortOption.RELEVANCE, ProductFilter(minDiscount = 9, brands = setOf("FarmFresh")))

        assertThat(visibleIds()).containsExactly("curd", "cheese").inOrder()
        assertThat(viewModel.uiState.value.filter.activeCount).isEqualTo(2)
    }

    @Test
    fun `price range filter and facet options`() = runTest {
        observe()

        val options = viewModel.filterOptions.value
        assertThat(options.minPrice).isEqualTo(35)
        assertThat(options.maxPrice).isEqualTo(329)
        assertThat(options.brands).containsExactly("DailyGold", "FarmFresh", "HappyHens").inOrder()

        viewModel.setFilter(ProductFilter(minPrice = 50, maxPrice = 100))
        assertThat(visibleIds()).containsExactly("milk", "paneer", "eggs").inOrder()
    }

    @Test
    fun `filters that match nothing produce empty state until cleared`() = runTest {
        observe()

        viewModel.setFilter(ProductFilter(brands = setOf("HappyHens"), minDiscount = 30))
        assertThat(viewModel.uiState.value.content).isEqualTo(UiState.Empty)

        viewModel.clearFilters()
        assertThat(visibleIds()).hasSize(6)
    }

    @Test
    fun `cart quantities are merged into product items`() = runTest {
        observe()

        viewModel.increment(TestData.paneer)
        viewModel.increment(TestData.paneer)

        val items = (viewModel.uiState.value.content as UiState.Content<List<ProductItem>>).data
        assertThat(items.first { it.product.id == "paneer" }.quantity).isEqualTo(2)
        assertThat(items.first { it.product.id == "milk" }.quantity).isEqualTo(0)
    }
}
