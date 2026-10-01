package io.github.ieswar23.greenbasket.ui.common

import android.content.Context
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.domain.model.DeliveryDay
import io.github.ieswar23.greenbasket.domain.model.DeliverySlot

object SlotLabels {

    fun hour(hour24: Int): String {
        val normalized = ((hour24 % 24) + 24) % 24
        val suffix = if (normalized < 12) "AM" else "PM"
        val h = when (val h12 = normalized % 12) {
            0 -> 12
            else -> h12
        }
        return "$h $suffix"
    }

    fun window(slot: DeliverySlot): String = "${hour(slot.startHour)} – ${hour(slot.endHour)}"

    /** "Express · 15 mins", "Today, 6 PM – 7 PM" or "Tomorrow, 7 AM – 8 AM". */
    fun full(context: Context, slot: DeliverySlot): String = when {
        slot.isExpress -> context.getString(R.string.slot_express_full, DeliverySlot.EXPRESS_MINUTES)
        slot.day == DeliveryDay.TODAY -> context.getString(R.string.slot_today_format, window(slot))
        else -> context.getString(R.string.slot_tomorrow_format, window(slot))
    }

    /** Header text on the home screen, e.g. "15 minutes" or "Tomorrow, 7 AM – 8 AM". */
    fun headline(context: Context, slot: DeliverySlot): String =
        if (slot.isExpress) {
            context.getString(R.string.home_delivery_in_minutes, DeliverySlot.EXPRESS_MINUTES)
        } else {
            full(context, slot)
        }
}
