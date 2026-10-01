package io.github.ieswar23.greenbasket.data.preferences

import io.github.ieswar23.greenbasket.domain.model.PaymentMethod
import io.github.ieswar23.greenbasket.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val recentSearches: Flow<List<String>>
    val themeMode: Flow<ThemeMode>
    val selectedAddressId: Flow<Long?>
    val deliverySlotId: Flow<String?>
    val couponCode: Flow<String?>
    val paymentMethod: Flow<PaymentMethod>

    suspend fun addRecentSearch(query: String)
    suspend fun removeRecentSearch(query: String)
    suspend fun clearRecentSearches()
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setSelectedAddressId(id: Long)
    suspend fun setDeliverySlotId(id: String)
    suspend fun setCouponCode(code: String?)
    suspend fun setPaymentMethod(method: PaymentMethod)

    companion object {
        const val MAX_RECENT_SEARCHES = 8

        /** Most-recent-first, case-insensitive de-duplication, capped at [MAX_RECENT_SEARCHES]. */
        fun mergeRecent(existing: List<String>, query: String): List<String> {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return existing
            return (listOf(trimmed) + existing.filterNot { it.equals(trimmed, ignoreCase = true) })
                .take(MAX_RECENT_SEARCHES)
        }
    }
}
