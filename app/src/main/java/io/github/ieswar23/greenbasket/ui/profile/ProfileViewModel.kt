package io.github.ieswar23.greenbasket.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.data.preferences.PreferencesRepository
import io.github.ieswar23.greenbasket.data.repository.AddressRepository
import io.github.ieswar23.greenbasket.data.repository.OrderRepository
import io.github.ieswar23.greenbasket.data.repository.WishlistRepository
import io.github.ieswar23.greenbasket.domain.model.Address
import io.github.ieswar23.greenbasket.domain.model.OrderStatus
import io.github.ieswar23.greenbasket.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val addresses: List<Address> = emptyList(),
    val selectedAddressId: Long? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val orderCount: Int = 0,
    val totalSavings: Int = 0,
    val wishlistCount: Int = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val addressRepository: AddressRepository,
    private val preferences: PreferencesRepository,
    orderRepository: OrderRepository,
    wishlistRepository: WishlistRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        addressRepository.observeAddresses(),
        addressRepository.observeSelected(),
        preferences.themeMode,
        orderRepository.observeOrders(),
        wishlistRepository.observeIds(),
    ) { addresses, selected, theme, orders, wishlist ->
        val completed = orders.filter { it.status != OrderStatus.CANCELLED }
        ProfileUiState(
            addresses = addresses,
            selectedAddressId = selected?.id,
            themeMode = theme,
            orderCount = completed.size,
            totalSavings = completed.sumOf { it.savings },
            wishlistCount = wishlist.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun selectAddress(address: Address) {
        viewModelScope.launch { addressRepository.select(address.id) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }
}
