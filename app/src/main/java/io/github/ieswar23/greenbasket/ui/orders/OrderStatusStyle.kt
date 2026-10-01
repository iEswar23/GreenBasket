package io.github.ieswar23.greenbasket.ui.orders

import android.content.res.ColorStateList
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.domain.model.OrderStatus

/** Visual treatment for each order status: label, colours and icon. */
enum class OrderStatusStyle(
    @StringRes val label: Int,
    @StringRes val headline: Int,
    @ColorRes val background: Int,
    @ColorRes val text: Int,
    @DrawableRes val icon: Int,
) {
    PLACED(R.string.status_placed, R.string.status_headline_placed, R.color.status_placed_bg, R.color.status_placed_text, R.drawable.ic_receipt),
    PACKED(R.string.status_packed, R.string.status_headline_packed, R.color.status_packed_bg, R.color.status_packed_text, R.drawable.ic_package),
    OUT_FOR_DELIVERY(R.string.status_on_the_way, R.string.status_headline_on_the_way, R.color.status_shipping_bg, R.color.status_shipping_text, R.drawable.ic_delivery),
    DELIVERED(R.string.status_delivered, R.string.status_headline_delivered, R.color.status_delivered_bg, R.color.status_delivered_text, R.drawable.ic_check),
    CANCELLED(R.string.status_cancelled, R.string.status_headline_cancelled, R.color.status_cancelled_bg, R.color.status_cancelled_text, R.drawable.ic_close),
    ;

    companion object {
        fun of(status: OrderStatus): OrderStatusStyle = when (status) {
            OrderStatus.PLACED -> PLACED
            OrderStatus.PACKED -> PACKED
            OrderStatus.OUT_FOR_DELIVERY -> OUT_FOR_DELIVERY
            OrderStatus.DELIVERED -> DELIVERED
            OrderStatus.CANCELLED -> CANCELLED
        }
    }
}

fun Chip.bindStatus(status: OrderStatus) {
    val style = OrderStatusStyle.of(status)
    setText(style.label)
    chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, style.background))
    setTextColor(ContextCompat.getColor(context, style.text))
    setChipIconResource(style.icon)
    chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(context, style.text))
    isChipIconVisible = true
    chipIconSize = resources.getDimension(R.dimen.space_l)
}
