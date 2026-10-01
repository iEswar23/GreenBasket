package io.github.ieswar23.greenbasket.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.greenbasket.data.preferences.PreferencesRepository
import io.github.ieswar23.greenbasket.domain.model.DeliveryDay
import io.github.ieswar23.greenbasket.domain.model.DeliverySlot
import io.github.ieswar23.greenbasket.domain.model.OrderStatus
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class DeliverySlotProviderTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val provider = DeliverySlotProvider().apply { timeZone = utc }

    private fun at(hour: Int, minute: Int): Long = Calendar.getInstance(utc).apply {
        clear()
        set(2026, Calendar.OCTOBER, 1, hour, minute)
    }.timeInMillis

    @Test
    fun `daytime offers express first and hourly slots starting two hours out`() {
        val slots = provider.slots(at(10, 20))

        assertThat(slots.first().isExpress).isTrue()
        val today = slots.filter { !it.isExpress && it.day == DeliveryDay.TODAY }
        assertThat(today.first().startHour).isEqualTo(12)
        assertThat(today.last().endHour).isEqualTo(DeliverySlotProvider.CLOSE_HOUR)
        val tomorrow = slots.filter { it.day == DeliveryDay.TOMORROW }
        assertThat(tomorrow).hasSize(DeliverySlotProvider.CLOSE_HOUR - DeliverySlotProvider.OPEN_HOUR)
        assertThat(tomorrow.first().id).isEqualTo("20261002-07")
    }

    @Test
    fun `late at night express is unavailable and a stored express slot falls back to tomorrow morning`() {
        val now = at(23, 30)

        assertThat(provider.isExpressAvailable(now)).isFalse()
        val resolved = provider.resolve(DeliverySlot.EXPRESS_ID, now)
        assertThat(resolved.isExpress).isFalse()
        assertThat(resolved.day).isEqualTo(DeliveryDay.TOMORROW)
        assertThat(resolved.startHour).isEqualTo(DeliverySlotProvider.OPEN_HOUR)
    }

    @Test
    fun `resolve keeps a still-valid scheduled slot`() {
        assertThat(provider.resolve("20261001-18", at(9, 0)).startHour).isEqualTo(18)
    }
}

class OrderStatusResolverTest {

    private val placedAt = 1_000_000L
    private fun minutes(m: Long) = TimeUnit.MINUTES.toMillis(m)

    @Test
    fun `express orders progress with elapsed time`() {
        fun statusAfter(m: Long) = OrderStatusResolver.resolve(OrderStatus.PLACED, placedAt, true, placedAt, placedAt + minutes(m))

        assertThat(statusAfter(1)).isEqualTo(OrderStatus.PLACED)
        assertThat(statusAfter(5)).isEqualTo(OrderStatus.PACKED)
        assertThat(statusAfter(10)).isEqualTo(OrderStatus.OUT_FOR_DELIVERY)
        assertThat(statusAfter(20)).isEqualTo(OrderStatus.DELIVERED)
    }

    @Test
    fun `terminal statuses are never changed`() {
        val later = placedAt + minutes(1)
        assertThat(OrderStatusResolver.resolve(OrderStatus.CANCELLED, placedAt, true, placedAt, later))
            .isEqualTo(OrderStatus.CANCELLED)
        assertThat(OrderStatusResolver.resolve(OrderStatus.DELIVERED, placedAt, false, placedAt + minutes(600), later))
            .isEqualTo(OrderStatus.DELIVERED)
    }

    @Test
    fun `scheduled orders are packed before the slot and delivered after it ends`() {
        val slotStart = placedAt + minutes(240)
        fun statusAt(time: Long) = OrderStatusResolver.resolve(OrderStatus.PLACED, placedAt, false, slotStart, time)

        assertThat(statusAt(placedAt + minutes(10))).isEqualTo(OrderStatus.PLACED)
        assertThat(statusAt(slotStart - minutes(20))).isEqualTo(OrderStatus.PACKED)
        assertThat(statusAt(slotStart + minutes(30))).isEqualTo(OrderStatus.OUT_FOR_DELIVERY)
        assertThat(statusAt(slotStart + minutes(61))).isEqualTo(OrderStatus.DELIVERED)
    }
}

class AddressValidatorTest {

    private val valid = AddressInput(
        label = "Home",
        receiverName = "Priya Nair",
        phone = "98450 12345",
        houseDetails = "B-302, Prestige Lakeside",
        area = "Whitefield",
        city = "Bengaluru",
        pincode = "560066",
    )

    @Test
    fun `complete address has no errors`() {
        assertThat(AddressValidator.validate(valid)).isEmpty()
    }

    @Test
    fun `invalid phone, pincode and missing fields are reported`() {
        val errors = AddressValidator.validate(valid.copy(phone = "12345", pincode = "06006", area = " ", receiverName = ""))

        assertThat(errors).containsExactly(AddressField.PHONE, AddressField.PINCODE, AddressField.AREA, AddressField.NAME)
    }

    @Test
    fun `phone numbers are normalised with or without country code`() {
        assertThat(AddressValidator.normalizePhone("+91 98450-12345")).isEqualTo("+91 98450 12345")
        assertThat(AddressValidator.normalizePhone("09845012345")).isEqualTo("+91 98450 12345")
        assertThat(AddressValidator.normalizePhone("5845012345")).isNull()
    }
}

class RecentSearchesTest {

    @Test
    fun `recent searches are most-recent-first, de-duplicated and capped`() {
        var recent = emptyList<String>()
        (1..10).forEach { recent = PreferencesRepository.mergeRecent(recent, "item $it") }
        recent = PreferencesRepository.mergeRecent(recent, "ITEM 9")

        assertThat(recent).hasSize(PreferencesRepository.MAX_RECENT_SEARCHES)
        assertThat(recent.first()).isEqualTo("ITEM 9")
        assertThat(recent.count { it.equals("item 9", ignoreCase = true) }).isEqualTo(1)
        assertThat(PreferencesRepository.mergeRecent(recent, "   ")).isEqualTo(recent)
    }
}
