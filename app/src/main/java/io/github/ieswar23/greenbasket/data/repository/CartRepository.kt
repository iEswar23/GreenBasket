package io.github.ieswar23.greenbasket.data.repository

import io.github.ieswar23.greenbasket.domain.model.CartItem
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeCart(): Flow<List<CartItem>>
    fun observeQuantities(): Flow<Map<String, Int>>
    fun observeItemCount(): Flow<Int>

    suspend fun increment(productId: String)
    suspend fun decrement(productId: String)
    suspend fun setQuantity(productId: String, quantity: Int)
    suspend fun remove(productId: String)

    /** Re-inserts a previously removed line with its original position (used by "Undo"). */
    suspend fun restore(item: CartItem)
    suspend fun addAll(lines: Map<String, Int>)
    suspend fun clear()

    companion object {
        const val MAX_QUANTITY_PER_ITEM = 10
    }
}
