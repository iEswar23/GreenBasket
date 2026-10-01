package io.github.ieswar23.greenbasket.domain

import io.github.ieswar23.greenbasket.domain.model.FilterOptions
import io.github.ieswar23.greenbasket.domain.model.Product
import io.github.ieswar23.greenbasket.domain.model.ProductFilter
import io.github.ieswar23.greenbasket.domain.model.SortOption

/** Pure sorting and filtering for product listings. */
object ProductQueryEngine {

    fun apply(products: List<Product>, sort: SortOption, filter: ProductFilter): List<Product> {
        val filtered = products.filter { product ->
            (filter.minPrice == null || product.price >= filter.minPrice) &&
                (filter.maxPrice == null || product.price <= filter.maxPrice) &&
                product.discountPercent >= filter.minDiscount &&
                (filter.brands.isEmpty() || product.brand in filter.brands)
        }
        return when (sort) {
            SortOption.RELEVANCE -> filtered
            SortOption.PRICE_LOW_TO_HIGH -> filtered.sortedWith(compareBy<Product> { it.price }.thenBy { it.name })
            SortOption.PRICE_HIGH_TO_LOW -> filtered.sortedWith(compareByDescending<Product> { it.price }.thenBy { it.name })
            SortOption.DISCOUNT -> filtered.sortedWith(compareByDescending<Product> { it.discountPercent }.thenBy { it.price })
            SortOption.NAME -> filtered.sortedWith(compareBy<Product> { it.name.lowercase() }.thenBy { it.price })
        }
    }

    fun optionsFor(products: List<Product>): FilterOptions {
        if (products.isEmpty()) return FilterOptions.EMPTY
        return FilterOptions(
            brands = products.map { it.brand }.distinct().sorted(),
            minPrice = products.minOf { it.price },
            maxPrice = products.maxOf { it.price },
        )
    }
}
