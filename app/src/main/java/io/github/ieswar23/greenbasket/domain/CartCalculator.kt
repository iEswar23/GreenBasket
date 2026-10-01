package io.github.ieswar23.greenbasket.domain

import io.github.ieswar23.greenbasket.domain.model.Bill
import io.github.ieswar23.greenbasket.domain.model.CartItem
import io.github.ieswar23.greenbasket.domain.model.Coupon
import io.github.ieswar23.greenbasket.domain.model.CouponBenefit
import javax.inject.Inject

/**
 * Pure pricing rules for the cart. No Android or I/O dependencies so it can be unit tested in isolation.
 *
 * Rules:
 *  - Item total is the sum of selling price x quantity; MRP savings are the MRP total minus the item total.
 *  - Delivery is free when the item total reaches [FREE_DELIVERY_THRESHOLD], otherwise [DELIVERY_FEE].
 *  - A flat [HANDLING_FEE] is applied to every non-empty cart.
 *  - A coupon only applies when the item total meets its minimum order value; the discount never
 *    exceeds the item total.
 */
class CartCalculator @Inject constructor() {

    fun calculate(items: List<CartItem>, coupon: Coupon? = null): Bill {
        val validItems = items.filter { it.quantity > 0 }
        if (validItems.isEmpty()) return Bill.EMPTY.copy(appliedCoupon = coupon)

        val itemTotal = validItems.sumOf { it.lineTotal }
        val mrpTotal = validItems.sumOf { it.lineMrp }
        val itemCount = validItems.sumOf { it.quantity }

        val freeDelivery = itemTotal >= FREE_DELIVERY_THRESHOLD
        val deliveryFee = if (freeDelivery) 0 else DELIVERY_FEE

        val couponShortfall = coupon?.let { (it.minOrder - itemTotal).coerceAtLeast(0) } ?: 0
        val couponDiscount = if (coupon != null && couponShortfall == 0) discountFor(coupon, itemTotal) else 0

        return Bill(
            itemCount = itemCount,
            itemTotal = itemTotal,
            mrpTotal = maxOf(mrpTotal, itemTotal),
            deliveryFee = deliveryFee,
            handlingFee = HANDLING_FEE,
            couponDiscount = couponDiscount,
            appliedCoupon = coupon,
            couponShortfall = couponShortfall,
            amountToFreeDelivery = if (freeDelivery) 0 else FREE_DELIVERY_THRESHOLD - itemTotal,
            deliveryFeeWaived = if (freeDelivery) DELIVERY_FEE else 0,
        )
    }

    fun discountFor(coupon: Coupon, itemTotal: Int): Int {
        val raw = when (val benefit = coupon.benefit) {
            is CouponBenefit.Flat -> benefit.amount
            is CouponBenefit.Percent -> minOf(itemTotal * benefit.percent / 100, benefit.maxDiscount)
        }
        return raw.coerceIn(0, itemTotal)
    }

    companion object {
        const val FREE_DELIVERY_THRESHOLD = 199
        const val DELIVERY_FEE = 25
        const val HANDLING_FEE = 4
    }
}
