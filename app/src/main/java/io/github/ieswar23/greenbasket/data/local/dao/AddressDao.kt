package io.github.ieswar23.greenbasket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.ieswar23.greenbasket.data.local.entity.AddressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressDao {

    @Query("SELECT * FROM addresses ORDER BY id")
    fun observeAll(): Flow<List<AddressEntity>>

    @Query("SELECT COUNT(*) FROM addresses")
    suspend fun count(): Int

    @Insert
    suspend fun insert(address: AddressEntity): Long

    @Insert
    suspend fun insertAll(addresses: List<AddressEntity>)

    @Query("DELETE FROM addresses WHERE id = :id")
    suspend fun delete(id: Long)
}
