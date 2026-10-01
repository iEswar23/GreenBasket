package io.github.ieswar23.greenbasket.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.greenbasket.domain.model.Bill
import io.github.ieswar23.greenbasket.domain.model.Coupon
import io.github.ieswar23.greenbasket.domain.model.CouponBenefit
import io.github.ieswar23.greenbasket.testing.TestData
import io.github.ieswar23.greenbasket.testing.TestData.line
import org.junit.Test

class CartCalculatorTest {

    private val calculator = CartCalculator()

    private val flat50 = Coupon("FLAT50", "Flat ₹50 off", "", minOrder = 399, benefit = CouponBenefit.Flat(50))
    private val tenPercent = Coupon("TEN", "10% off", "", minOrder = 299, benefit = CouponBenefit.Percent(10, maxDiscount = 100))

    @Test
    fun `empty cart has no fees and zero total`() {
        val bill = calculator.calculate(emptyList())

        assertThat(bill).isEqualTo(Bill.EMPTY)
        assertThat(bill.grandTotal).isEqualTo(0)
        assertThat(bill.isEmpty).isTrue()
    }

    @Test
    fun `item total and MRP savings are summed per line`() {
        val bill = calculator.calculate(
            listOf(line(TestData.paneer, 2), line(TestData.curd, 1)), // 89x2 + 35 = 213; MRP 95x2 + 40 = 230
        )

        assertThat(bill.itemCount).isEqualTo(3)
        assertThat(bill.itemTotal).isEqualTo(213)
        assertThat(bill.mrpTotal).isEqualTo(230)
        assertThat(bill.mrpSavings).isEqualTo(17)
    }

    @Test
    fun `delivery fee is charged below the free delivery threshold`() {
        val bill = calculator.calculate(listOf(line(TestData.milk, 2))) // ₹108

        assertThat(bill.deliveryFee).isEqualTo(CartCalculator.DELIVERY_FEE)
        assertThat(bill.amountToFreeDelivery).isEqualTo(CartCalculator.FREE_DELIVERY_THRESHOLD - 108)
        assertThat(bill.deliveryFeeWaived).isEqualTo(0)
    }

    @Test
    fun `delivery becomes free exactly at the threshold and counts as savings`() {
        val product = TestData.product("basket", price = 199, mrp = 199)
        val bill = calculator.calculate(listOf(line(product, 1)))

        assertThat(bill.deliveryFee).isEqualTo(0)
        assertThat(bill.amountToFreeDelivery).isEqualTo(0)
        assertThat(bill.deliveryFeeWaived).isEqualTo(CartCalculator.DELIVERY_FEE)
        assertThat(bill.totalSavings).isEqualTo(CartCalculator.DELIVERY_FEE)
    }

    @Test
    fun `grand total adds delivery and handling fees`() {
        val bill = calculator.calculate(listOf(line(TestData.curd, 2))) // ₹70

        assertThat(bill.handlingFee).isEqualTo(CartCalculator.HANDLING_FEE)
        assertThat(bill.grandTotal).isEqualTo(70 + CartCalculator.DELIVERY_FEE + CartCalculator.HANDLING_FEE)
    }

    @Test
    fun `flat coupon applies once minimum order is met`() {
        val bill = calculator.calculate(listOf(line(TestData.ghee, 1), line(TestData.paneer, 1)), flat50) // ₹418

        assertThat(bill.couponShortfall).isEqualTo(0)
        assertThat(bill.couponDiscount).isEqualTo(50)
        assertThat(bill.grandTotal).isEqualTo(418 + CartCalculator.HANDLING_FEE - 50)
        assertThat(bill.totalSavings).isEqualTo(bill.mrpSavings + 50 + CartCalculator.DELIVERY_FEE)
    }

    @Test
    fun `coupon below minimum order is kept but gives no discount and reports the shortfall`() {
        val bill = calculator.calculate(listOf(line(TestData.ghee, 1)), flat50) // ₹329

        assertThat(bill.appliedCoupon).isEqualTo(flat50)
        assertThat(bill.couponDiscount).isEqualTo(0)
        assertThat(bill.couponShortfall).isEqualTo(399 - 329)
    }

    @Test
    fun `percentage coupon is capped at its maximum discount`() {
        val small = calculator.calculate(listOf(line(TestData.ghee, 1)), tenPercent) // 10% of 329 = 32
        val large = calculator.calculate(listOf(line(TestData.ghee, 4)), tenPercent) // 10% of 1316 = 131 -> capped 100

        assertThat(small.couponDiscount).isEqualTo(32)
        assertThat(large.couponDiscount).isEqualTo(100)
    }

    @Test
    fun `coupon discount never exceeds the item total`() {
        val huge = Coupon("HUGE", "", "", minOrder = 0, benefit = CouponBenefit.Flat(1_000))
        val bill = calculator.calculate(listOf(line(TestData.milk, 1)), huge)

        assertThat(bill.couponDiscount).isEqualTo(54)
        assertThat(bill.grandTotal).isAtLeast(0)
    }

    @Test
    fun `lines with zero quantity are ignored`() {
        val bill = calculator.calculate(listOf(line(TestData.milk, 0), line(TestData.curd, 1)))

        assertThat(bill.itemCount).isEqualTo(1)
        assertThat(bill.itemTotal).isEqualTo(35)
    }
}
