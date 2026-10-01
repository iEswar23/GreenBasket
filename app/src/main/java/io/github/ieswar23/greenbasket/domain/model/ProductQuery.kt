package io.github.ieswar23.greenbasket.domain.model

enum class SortOption { RELEVANCE, PRICE_LOW_TO_HIGH, PRICE_HIGH_TO_LOW, DISCOUNT, NAME }

data class ProductFilter(
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val minDiscount: Int = 0,
    val brands: Set<String> = emptySet(),
) {
    val activeCount: Int
        get() = listOf(minPrice != null || maxPrice != null, minDiscount > 0, brands.isNotEmpty()).count { it }

    companion object {
        val NONE = ProductFilter()
    }
}

/** Facets available for filtering a product listing. */
data class FilterOptions(
    val brands: List<String>,
    val minPrice: Int,
    val maxPrice: Int,
) {
    companion object {
        val EMPTY = FilterOptions(emptyList(), 0, 0)
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
