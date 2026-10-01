package io.github.ieswar23.greenbasket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.data.repository.AddressRepository
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.data.repository.CatalogRepository
import io.github.ieswar23.greenbasket.data.repository.OrderRepository
import io.github.ieswar23.greenbasket.data.repository.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val orderRepository: OrderRepository,
    private val addressRepository: AddressRepository,
    cartRepository: CartRepository,
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)

    /** Keeps the splash screen up until local seed data (addresses) is in place. */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    val cartCount: StateFlow<Int> = cartRepository.observeItemCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            addressRepository.seedIfEmpty()
            _isReady.value = true
            catalogRepository.syncIfNeeded()
            if (catalogRepository.syncState.value == SyncState.Synced) {
                orderRepository.importHistoryIfEmpty()
            }
        }
    }
}
