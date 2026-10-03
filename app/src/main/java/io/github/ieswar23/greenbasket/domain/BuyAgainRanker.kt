package io.github.ieswar23.greenbasket.domain

import io.github.ieswar23.greenbasket.domain.model.Order
import io.github.ieswar23.greenbasket.domain.model.OrderStatus
import io.github.ieswar23.greenbasket.domain.model.Product
import io.github.ieswar23.greenbasket.util.TimeProvider
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.pow

/**
 * Ranks the products a customer buys often for the "Buy again" shelf on Home.
 *
 * Rules:
 *  - Every non-cancelled order that contains a product adds a recency weight to its score. The weight halves
 *    every [HALF_LIFE_DAYS] days, so a staple bought every week outranks something bought once last month,
 *    and a recent purchase outranks an equally frequent older one.
 *  - Quantity within an order doesn't count; buying 2 L of milk once is one purchase.
 *  - Products that are out of stock or no longer in the catalog are excluded.
 *  - Ties are broken deterministically: most recently ordered, then most orders, then name, then id.
 *  - At most [MAX_ITEMS] products are returned.
 */
class BuyAgainRanker @Inject constructor(
    private val time: TimeProvider,
) {

    fun rank(orders: List<Order>, catalog: Collection<Product>, limit: Int = MAX_ITEMS): List<Product> {
        if (limit <= 0) return emptyList()
        val now = time.now()
        val products = catalog.associateBy { it.id }
        val stats = mutableMapOf<String, PurchaseStats>()

        orders.filter { it.status != OrderStatus.CANCELLED }.forEach { order ->
            val weight = recencyWeight(now - order.placedAt)
            order.items.map { it.productId }.distinct().forEach { productId ->
                val current = stats[productId] ?: PurchaseStats()
                stats[productId] = PurchaseStats(
                    score = current.score + weight,
                    orderCount = current.orderCount + 1,
                    lastOrderedAt = maxOf(current.lastOrderedAt, order.placedAt),
                )
            }
        }

        return stats.mapNotNull { (productId, stat) ->
            products[productId]?.takeIf { it.inStock }?.let { it to stat }
        }
            .sortedWith(
                compareByDescending<Pair<Product, PurchaseStats>> { it.second.score }
                    .thenByDescending { it.second.lastOrderedAt }
                    .thenByDescending { it.second.orderCount }
                    .thenBy { it.first.name }
                    .thenBy { it.first.id },
            )
            .take(limit)
            .map { it.first }
    }

    private fun recencyWeight(ageMillis: Long): Double {
        val ageDays = ageMillis.coerceAtLeast(0L).toDouble() / DAY_MILLIS
        return 0.5.pow(ageDays / HALF_LIFE_DAYS)
    }

    private data class PurchaseStats(
        val score: Double = 0.0,
        val orderCount: Int = 0,
        val lastOrderedAt: Long = Long.MIN_VALUE,
    )

    companion object {
        const val MAX_ITEMS = 10
        const val HALF_LIFE_DAYS = 14.0
        private val DAY_MILLIS = TimeUnit.DAYS.toMillis(1).toDouble()

        /** Ids of every product that could appear on the shelf, used to look up their current catalog entries. */
        fun candidateIds(orders: List<Order>): Set<String> =
            orders.asSequence()
                .filter { it.status != OrderStatus.CANCELLED }
                .flatMap { order -> order.items.asSequence().map { it.productId } }
                .toSortedSet()
    }
}
