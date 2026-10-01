package io.github.ieswar23.greenbasket.domain.model

enum class DeliveryDay { TODAY, TOMORROW }

data class DeliverySlot(
    val id: String,
    val day: DeliveryDay,
    /** Start of the slot in epoch millis. For express delivery this is the time the slot list was built. */
    val startMillis: Long,
    val startHour: Int,
    val endHour: Int,
    val isExpress: Boolean,
) {
    companion object {
        const val EXPRESS_ID = "express"
        const val EXPRESS_MINUTES = 15
    }
}
