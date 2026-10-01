package io.github.ieswar23.greenbasket.data.repository

import io.github.ieswar23.greenbasket.data.local.dao.AddressDao
import io.github.ieswar23.greenbasket.data.local.entity.AddressEntity
import io.github.ieswar23.greenbasket.data.preferences.PreferencesRepository
import io.github.ieswar23.greenbasket.data.toDomain
import io.github.ieswar23.greenbasket.data.toEntity
import io.github.ieswar23.greenbasket.domain.model.Address
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AddressRepository {
    fun observeAddresses(): Flow<List<Address>>

    /** The address orders are delivered to; falls back to the first saved address. */
    fun observeSelected(): Flow<Address?>
    suspend fun select(addressId: Long)
    suspend fun add(address: Address): Long
    suspend fun seedIfEmpty()
}

@Singleton
class RoomAddressRepository @Inject constructor(
    private val dao: AddressDao,
    private val preferences: PreferencesRepository,
) : AddressRepository {

    override fun observeAddresses(): Flow<List<Address>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeSelected(): Flow<Address?> =
        combine(observeAddresses(), preferences.selectedAddressId) { addresses, selectedId ->
            addresses.firstOrNull { it.id == selectedId } ?: addresses.firstOrNull()
        }

    override suspend fun select(addressId: Long) = preferences.setSelectedAddressId(addressId)

    override suspend fun add(address: Address): Long {
        val id = dao.insert(address.toEntity().copy(id = 0))
        preferences.setSelectedAddressId(id)
        return id
    }

    override suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        dao.insertAll(
            listOf(
                AddressEntity(
                    label = "Home",
                    receiverName = "Aarav Mehta",
                    phone = "+91 98200 41736",
                    houseDetails = "Flat 1204, Lotus Residency",
                    area = "Hiranandani Gardens, Powai",
                    city = "Mumbai",
                    pincode = "400076",
                ),
                AddressEntity(
                    label = "Work",
                    receiverName = "Aarav Mehta",
                    phone = "+91 98200 41736",
                    houseDetails = "5th Floor, Tower B, Nirlon Knowledge Park",
                    area = "Goregaon East",
                    city = "Mumbai",
                    pincode = "400063",
                ),
            ),
        )
    }
}
