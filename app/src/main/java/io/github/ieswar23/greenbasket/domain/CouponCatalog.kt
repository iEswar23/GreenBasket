package io.github.ieswar23.greenbasket.domain

import io.github.ieswar23.greenbasket.domain.model.Coupon
import io.github.ieswar23.greenbasket.domain.model.CouponBenefit

/** Offers currently running in the app. */
object CouponCatalog {

    val all: List<Coupon> = listOf(
        Coupon(
            code = "FRESH50",
            title = "Flat ₹50 off",
            description = "On orders above ₹399",
            minOrder = 399,
            benefit = CouponBenefit.Flat(50),
        ),
        Coupon(
            code = "GREEN10",
            title = "10% off up to ₹100",
            description = "On orders above ₹299",
            minOrder = 299,
            benefit = CouponBenefit.Percent(percent = 10, maxDiscount = 100),
        ),
        Coupon(
            code = "BASKET20",
            title = "20% off up to ₹150",
            description = "On orders above ₹699",
            minOrder = 699,
            benefit = CouponBenefit.Percent(percent = 20, maxDiscount = 150),
        ),
        Coupon(
            code = "SNACK25",
            title = "Flat ₹25 off",
            description = "On orders above ₹149",
            minOrder = 149,
            benefit = CouponBenefit.Flat(25),
        ),
    )

    fun find(code: String?): Coupon? {
        if (code.isNullOrBlank()) return null
        val normalized = code.trim().uppercase()
        return all.firstOrNull { it.code == normalized }
    }
}
