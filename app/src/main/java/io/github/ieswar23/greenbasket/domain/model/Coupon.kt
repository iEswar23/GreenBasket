package io.github.ieswar23.greenbasket.domain.model

sealed interface CouponBenefit {
    data class Flat(val amount: Int) : CouponBenefit
    data class Percent(val percent: Int, val maxDiscount: Int) : CouponBenefit
}

data class Coupon(
    val code: String,
    val title: String,
    val description: String,
    val minOrder: Int,
    val benefit: CouponBenefit,
)
