package io.github.ieswar23.greenbasket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.ieswar23.greenbasket.data.local.entity.ProductEntity
import io.github.ieswar23.greenbasket.data.local.entity.WishlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    @Query("SELECT productId FROM wishlist")
    fun observeIds(): Flow<List<String>>

    @Query(
        """
        SELECT p.* FROM wishlist w INNER JOIN products p ON p.id = w.productId
        ORDER BY w.addedAt DESC
        """,
    )
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist WHERE productId = :productId)")
    suspend fun contains(productId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WishlistEntity)

    @Query("DELETE FROM wishlist WHERE productId = :productId")
    suspend fun delete(productId: String)

    /** @return true when the product is wishlisted after the toggle. */
    @Transaction
    suspend fun toggle(productId: String, now: Long): Boolean {
        return if (contains(productId)) {
            delete(productId)
            false
        } else {
            insert(WishlistEntity(productId, now))
            true
        }
    }
}
