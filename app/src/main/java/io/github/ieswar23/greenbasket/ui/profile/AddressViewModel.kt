package io.github.ieswar23.greenbasket.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.greenbasket.data.repository.AddressRepository
import io.github.ieswar23.greenbasket.domain.AddressField
import io.github.ieswar23.greenbasket.domain.AddressInput
import io.github.ieswar23.greenbasket.domain.AddressValidator
import io.github.ieswar23.greenbasket.domain.model.Address
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddressBookState(
    val addresses: List<Address> = emptyList(),
    val selectedId: Long? = null,
)

@HiltViewModel
class AddressViewModel @Inject constructor(
    private val addressRepository: AddressRepository,
) : ViewModel() {

    val state: StateFlow<AddressBookState> = combine(
        addressRepository.observeAddresses(),
        addressRepository.observeSelected(),
    ) { addresses, selected -> AddressBookState(addresses, selected?.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddressBookState())

    private val _formErrors = MutableStateFlow<Set<AddressField>>(emptySet())
    val formErrors: StateFlow<Set<AddressField>> = _formErrors.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    fun select(address: Address) {
        viewModelScope.launch { addressRepository.select(address.id) }
    }

    fun save(input: AddressInput) {
        val errors = AddressValidator.validate(input)
        _formErrors.value = errors
        if (errors.isNotEmpty()) return
        viewModelScope.launch {
            addressRepository.add(
                Address(
                    id = 0,
                    label = input.label,
                    receiverName = input.receiverName.trim(),
                    phone = AddressValidator.normalizePhone(input.phone).orEmpty(),
                    houseDetails = input.houseDetails.trim(),
                    area = input.area.trim(),
                    city = input.city.trim(),
                    pincode = input.pincode.trim(),
                ),
            )
            _saved.send(Unit)
        }
    }
}
