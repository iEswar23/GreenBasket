package io.github.ieswar23.greenbasket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.ieswar23.greenbasket.data.local.entity.CartItemEntity
import io.github.ieswar23.greenbasket.data.local.entity.CartLine
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    @Query(
        """
        SELECT p.*, c.quantity AS quantity, c.addedAt AS addedAt FROM cart_items c
        INNER JOIN products p ON p.id = c.productId
        ORDER BY c.addedAt
        """,
    )
    fun observeCart(): Flow<List<CartLine>>

    @Query("SELECT * FROM cart_items")
    fun observeCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE productId = :productId")
    suspend fun get(productId: String): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun delete(productId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clear()

    @Transaction
    suspend fun changeQuantity(productId: String, delta: Int, maxQuantity: Int, now: Long) {
        val existing = get(productId)
        val newQuantity = ((existing?.quantity ?: 0) + delta).coerceAtMost(maxQuantity)
        if (newQuantity <= 0) {
            delete(productId)
        } else {
            upsert(CartItemEntity(productId, newQuantity, existing?.addedAt ?: now))
        }
    }
}
