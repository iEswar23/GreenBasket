package io.github.ieswar23.greenbasket.domain

import io.github.ieswar23.greenbasket.domain.model.DeliveryDay
import io.github.ieswar23.greenbasket.domain.model.DeliverySlot
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

/**
 * Builds the delivery slots a customer can pick from, relative to "now".
 *
 * - Express (15 min) delivery runs between [OPEN_HOUR] and [CLOSE_HOUR].
 * - Scheduled one-hour windows start at least one full hour from now, today and tomorrow.
 */
class DeliverySlotProvider @Inject constructor() {

    var timeZone: TimeZone = TimeZone.getDefault()

    fun slots(now: Long): List<DeliverySlot> {
        val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = now }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val result = mutableListOf<DeliverySlot>()

        if (isExpressAvailable(now)) {
            result += DeliverySlot(
                id = DeliverySlot.EXPRESS_ID,
                day = DeliveryDay.TODAY,
                startMillis = now,
                startHour = currentHour,
                endHour = currentHour,
                isExpress = true,
            )
        }
        val firstToday = maxOf(currentHour + 2, OPEN_HOUR)
        for (hour in firstToday until CLOSE_HOUR) {
            result += scheduled(now, DeliveryDay.TODAY, hour)
        }
        for (hour in OPEN_HOUR until CLOSE_HOUR) {
            result += scheduled(now, DeliveryDay.TOMORROW, hour)
        }
        return result
    }

    fun isExpressAvailable(now: Long): Boolean {
        val hour = Calendar.getInstance(timeZone).apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        return hour in OPEN_HOUR until CLOSE_HOUR
    }

    /** Resolves a stored slot id, falling back to the earliest available slot when it has expired. */
    fun resolve(slotId: String?, now: Long): DeliverySlot {
        val all = slots(now)
        return all.firstOrNull { it.id == slotId } ?: all.first()
    }

    private fun scheduled(now: Long, day: DeliveryDay, hour: Int): DeliverySlot {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = now
            if (day == DeliveryDay.TOMORROW) add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val id = "%04d%02d%02d-%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            hour,
        )
        return DeliverySlot(
            id = id,
            day = day,
            startMillis = calendar.timeInMillis,
            startHour = hour,
            endHour = hour + 1,
            isExpress = false,
        )
    }

    companion object {
        const val OPEN_HOUR = 7
        const val CLOSE_HOUR = 23
    }
}
