package io.github.ieswar23.greenbasket.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.data.preferences.PreferencesRepository
import io.github.ieswar23.greenbasket.domain.DeliverySlotProvider
import io.github.ieswar23.greenbasket.domain.model.DeliverySlot
import io.github.ieswar23.greenbasket.util.TimeProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SlotPickerState(
    val slots: List<DeliverySlot>,
    val selected: DeliverySlot,
)

@HiltViewModel
class DeliverySlotViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val slotProvider: DeliverySlotProvider,
    private val time: TimeProvider,
) : ViewModel() {

    val state: StateFlow<SlotPickerState?> = preferences.deliverySlotId
        .map { id ->
            val now = time.now()
            SlotPickerState(slotProvider.slots(now), slotProvider.resolve(id, now))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun select(slot: DeliverySlot) {
        viewModelScope.launch { preferences.setDeliverySlotId(slot.id) }
    }
}
