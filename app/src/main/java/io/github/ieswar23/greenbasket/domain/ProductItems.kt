package io.github.ieswar23.greenbasket.domain

import io.github.ieswar23.greenbasket.domain.model.Product
import io.github.ieswar23.greenbasket.domain.model.ProductItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Decorates a product stream with cart quantities and wishlist state. */
fun Flow<List<Product>>.withUserState(
    quantities: Flow<Map<String, Int>>,
    wishlist: Flow<Set<String>>,
): Flow<List<ProductItem>> = combine(this, quantities, wishlist) { products, cart, liked ->
    products.map { ProductItem(it, cart[it.id] ?: 0, it.id in liked) }
}

fun List<Product>.toItems(quantities: Map<String, Int>, wishlist: Set<String>): List<ProductItem> =
    map { ProductItem(it, quantities[it.id] ?: 0, it.id in wishlist) }
