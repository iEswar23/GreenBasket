package io.github.ieswar23.greenbasket.domain

import io.github.ieswar23.greenbasket.domain.model.Order
import io.github.ieswar23.greenbasket.domain.model.OrderItem
import io.github.ieswar23.greenbasket.domain.model.Product
import javax.inject.Inject

data class ReorderPlan(
    /** Product id to units to add on top of what's already in the cart, in the original order's line order. */
    val cartAdditions: Map<String, Int>,
    /** Order lines that can't be bought right now (out of stock or delisted). */
    val unavailable: List<OrderItem>,
    /** Units dropped because the cart already holds the per-item maximum. */
    val cappedUnits: Int,
) {
    val addedUnits: Int get() = cartAdditions.values.sum()
    val unavailableUnits: Int get() = unavailable.sumOf { it.quantity }
}

/**
 * Turns a past order into cart additions.
 *
 * Rules:
 *  - Lines are matched to the current catalog by product id, so the cart charges today's price.
 *  - Products that are out of stock or missing from the catalog are skipped and reported as unavailable.
 *  - Repeated lines for the same product are merged.
 *  - Quantities merge with what's already in the cart without going over the per-item maximum; anything over
 *    the limit is reported in [ReorderPlan.cappedUnits].
 */
class ReorderPlanner @Inject constructor() {

    fun plan(
        order: Order,
        catalog: Collection<Product>,
        cartQuantities: Map<String, Int>,
        maxQuantityPerItem: Int,
    ): ReorderPlan {
        val products = catalog.associateBy { it.id }
        val additions = linkedMapOf<String, Int>()
        val unavailable = mutableListOf<OrderItem>()
        var cappedUnits = 0

        val merged = order.items
            .filter { it.quantity > 0 }
            .groupBy { it.productId }
            .map { (_, items) -> items.first().copy(quantity = items.sumOf { it.quantity }) }

        merged.forEach { item ->
            val product = products[item.productId]
            if (product == null || !product.inStock) {
                unavailable += item
                return@forEach
            }
            val room = (maxQuantityPerItem - (cartQuantities[item.productId] ?: 0)).coerceAtLeast(0)
            val toAdd = minOf(item.quantity, room)
            cappedUnits += item.quantity - toAdd
            if (toAdd > 0) additions[item.productId] = toAdd
        }
        return ReorderPlan(additions, unavailable, cappedUnits)
    }
}
