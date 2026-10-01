package io.github.ieswar23.greenbasket.domain

import io.github.ieswar23.greenbasket.domain.model.OrderStatus
import java.util.concurrent.TimeUnit

/**
 * Simulates order progress for orders placed in the app, based purely on elapsed time.
 * Terminal statuses (delivered / cancelled) are never changed.
 */
object OrderStatusResolver {

    fun resolve(
        stored: OrderStatus,
        placedAt: Long,
        isExpress: Boolean,
        slotStartMillis: Long,
        now: Long,
    ): OrderStatus {
        if (stored == OrderStatus.DELIVERED || stored == OrderStatus.CANCELLED) return stored
        return if (isExpress) {
            val elapsed = now - placedAt
            when {
                elapsed < minutes(3) -> OrderStatus.PLACED
                elapsed < minutes(7) -> OrderStatus.PACKED
                elapsed < minutes(15) -> OrderStatus.OUT_FOR_DELIVERY
                else -> OrderStatus.DELIVERED
            }
        } else {
            when {
                now < slotStartMillis - minutes(45) -> OrderStatus.PLACED
                now < slotStartMillis -> OrderStatus.PACKED
                now < slotStartMillis + minutes(60) -> OrderStatus.OUT_FOR_DELIVERY
                else -> OrderStatus.DELIVERED
            }
        }
    }

    /** Ordered steps shown in the tracking timeline. */
    val timeline = listOf(
        OrderStatus.PLACED,
        OrderStatus.PACKED,
        OrderStatus.OUT_FOR_DELIVERY,
        OrderStatus.DELIVERED,
    )

    private fun minutes(value: Long) = TimeUnit.MINUTES.toMillis(value)
}
