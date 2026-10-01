package io.github.ieswar23.greenbasket.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.ieswar23.greenbasket.domain.model.PaymentMethod
import io.github.ieswar23.greenbasket.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    private val data: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    override val recentSearches: Flow<List<String>> = data.map { prefs ->
        prefs[Keys.RECENT_SEARCHES]?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()
    }.distinctUntilChanged()

    override val themeMode: Flow<ThemeMode> = data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }.distinctUntilChanged()

    override val selectedAddressId: Flow<Long?> = data.map { it[Keys.ADDRESS_ID] }.distinctUntilChanged()

    override val deliverySlotId: Flow<String?> = data.map { it[Keys.SLOT_ID] }.distinctUntilChanged()

    override val couponCode: Flow<String?> = data.map { it[Keys.COUPON] }.distinctUntilChanged()

    override val paymentMethod: Flow<PaymentMethod> = data.map { prefs ->
        prefs[Keys.PAYMENT]?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() } ?: PaymentMethod.UPI
    }.distinctUntilChanged()

    override suspend fun addRecentSearch(query: String) {
        dataStore.edit { prefs ->
            val existing = prefs[Keys.RECENT_SEARCHES]?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()
            prefs[Keys.RECENT_SEARCHES] = PreferencesRepository.mergeRecent(existing, query).joinToString(SEPARATOR)
        }
    }

    override suspend fun removeRecentSearch(query: String) {
        dataStore.edit { prefs ->
            val existing = prefs[Keys.RECENT_SEARCHES]?.split(SEPARATOR).orEmpty()
            prefs[Keys.RECENT_SEARCHES] = existing.filterNot { it == query }.joinToString(SEPARATOR)
        }
    }

    override suspend fun clearRecentSearches() {
        dataStore.edit { it.remove(Keys.RECENT_SEARCHES) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setSelectedAddressId(id: Long) {
        dataStore.edit { it[Keys.ADDRESS_ID] = id }
    }

    override suspend fun setDeliverySlotId(id: String) {
        dataStore.edit { it[Keys.SLOT_ID] = id }
    }

    override suspend fun setCouponCode(code: String?) {
        dataStore.edit { prefs ->
            if (code.isNullOrBlank()) prefs.remove(Keys.COUPON) else prefs[Keys.COUPON] = code
        }
    }

    override suspend fun setPaymentMethod(method: PaymentMethod) {
        dataStore.edit { it[Keys.PAYMENT] = method.name }
    }

    private object Keys {
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ADDRESS_ID = longPreferencesKey("selected_address_id")
        val SLOT_ID = stringPreferencesKey("delivery_slot_id")
        val COUPON = stringPreferencesKey("coupon_code")
        val PAYMENT = stringPreferencesKey("payment_method")
    }

    private companion object {
        const val SEPARATOR = "\u001F"
    }
}
