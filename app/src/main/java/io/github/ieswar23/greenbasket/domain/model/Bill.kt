package io.github.ieswar23.greenbasket.domain.model

/**
 * Immutable price breakdown for a cart. All amounts are whole rupees.
 */
data class Bill(
    val itemCount: Int,
    val itemTotal: Int,
    val mrpTotal: Int,
    val deliveryFee: Int,
    val handlingFee: Int,
    val couponDiscount: Int,
    val appliedCoupon: Coupon?,
    /** Amount still needed for the coupon to become applicable (0 when applicable or no coupon). */
    val couponShortfall: Int,
    /** Amount still needed to unlock free delivery (0 once unlocked). */
    val amountToFreeDelivery: Int,
    /** Delivery fee the customer would have paid without the free-delivery threshold. */
    val deliveryFeeWaived: Int,
) {
    val mrpSavings: Int get() = (mrpTotal - itemTotal).coerceAtLeast(0)
    val totalSavings: Int get() = mrpSavings + couponDiscount + deliveryFeeWaived
    val grandTotal: Int get() = (itemTotal + deliveryFee + handlingFee - couponDiscount).coerceAtLeast(0)
    val isEmpty: Boolean get() = itemCount == 0

    companion object {
        val EMPTY = Bill(0, 0, 0, 0, 0, 0, null, 0, 0, 0)
    }
}
