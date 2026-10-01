package io.github.ieswar23.greenbasket.ui.common

import androidx.core.view.isVisible
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.LayoutBillDetailsBinding
import io.github.ieswar23.greenbasket.domain.model.Bill
import io.github.ieswar23.greenbasket.domain.model.Order
import io.github.ieswar23.greenbasket.util.asDiscount
import io.github.ieswar23.greenbasket.util.asRupees

/** Binds the reusable bill breakdown card (cart, checkout and order details). */
object BillBinder {

    fun bind(binding: LayoutBillDetailsBinding, bill: Bill) {
        bindRows(
            binding = binding,
            itemTotal = bill.itemTotal,
            mrpTotal = bill.mrpTotal,
            deliveryFee = bill.deliveryFee,
            deliveryWaived = bill.deliveryFeeWaived,
            handlingFee = bill.handlingFee,
            couponCode = bill.appliedCoupon?.code?.takeIf { bill.couponDiscount > 0 },
            couponDiscount = bill.couponDiscount,
            grandTotal = bill.grandTotal,
            savings = bill.totalSavings,
        )
    }

    fun bind(binding: LayoutBillDetailsBinding, order: Order) {
        bindRows(
            binding = binding,
            itemTotal = order.itemTotal,
            mrpTotal = order.mrpTotal,
            deliveryFee = order.deliveryFee,
            deliveryWaived = 0,
            handlingFee = order.handlingFee,
            couponCode = order.couponCode,
            couponDiscount = order.couponDiscount,
            grandTotal = order.grandTotal,
            savings = order.savings,
        )
    }

    private fun bindRows(
        binding: LayoutBillDetailsBinding,
        itemTotal: Int,
        mrpTotal: Int,
        deliveryFee: Int,
        deliveryWaived: Int,
        handlingFee: Int,
        couponCode: String?,
        couponDiscount: Int,
        grandTotal: Int,
        savings: Int,
    ) {
        val context = binding.root.context
        binding.itemTotalValue.text = itemTotal.asRupees()
        binding.itemMrpValue.isVisible = mrpTotal > itemTotal
        binding.itemMrpValue.text = mrpTotal.asRupees()
        binding.itemMrpValue.setStrikeThrough()

        if (deliveryFee == 0) {
            binding.deliveryValue.text = context.getString(R.string.bill_free)
            binding.deliveryValue.setTextColor(context.getColor(R.color.price_green))
            binding.deliveryStrike.isVisible = deliveryWaived > 0
            binding.deliveryStrike.text = deliveryWaived.asRupees()
            binding.deliveryStrike.setStrikeThrough()
        } else {
            binding.deliveryValue.text = deliveryFee.asRupees()
            binding.deliveryValue.setTextColor(context.themeColor(com.google.android.material.R.attr.colorOnSurface))
            binding.deliveryStrike.isVisible = false
        }
        binding.handlingValue.text = handlingFee.asRupees()

        val hasCoupon = couponDiscount > 0
        binding.couponRow.isVisible = hasCoupon
        binding.couponLabel.text = context.getString(R.string.bill_coupon_discount, couponCode.orEmpty())
        binding.couponValue.text = couponDiscount.asDiscount()

        binding.grandTotalValue.text = grandTotal.asRupees()
        binding.savingsBanner.isVisible = savings > 0
        binding.savingsBanner.text = context.getString(R.string.bill_total_savings, savings.asRupees())
    }
}
