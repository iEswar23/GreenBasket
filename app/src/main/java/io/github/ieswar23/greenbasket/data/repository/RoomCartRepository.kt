package io.github.ieswar23.greenbasket.data.repository

import io.github.ieswar23.greenbasket.data.local.dao.CartDao
import io.github.ieswar23.greenbasket.data.local.entity.CartItemEntity
import io.github.ieswar23.greenbasket.data.toDomain
import io.github.ieswar23.greenbasket.domain.model.CartItem
import io.github.ieswar23.greenbasket.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCartRepository @Inject constructor(
    private val dao: CartDao,
    private val time: TimeProvider,
) : CartRepository {

    override fun observeCart(): Flow<List<CartItem>> =
        dao.observeCart().map { lines -> lines.map { CartItem(it.product.toDomain(), it.quantity, it.addedAt) } }

    override fun observeQuantities(): Flow<Map<String, Int>> =
        dao.observeCartItems().map { rows -> rows.associate { it.productId to it.quantity } }.distinctUntilChanged()

    override fun observeItemCount(): Flow<Int> =
        dao.observeCartItems().map { rows -> rows.sumOf { it.quantity } }.distinctUntilChanged()

    override suspend fun increment(productId: String) =
        dao.changeQuantity(productId, +1, CartRepository.MAX_QUANTITY_PER_ITEM, time.now())

    override suspend fun decrement(productId: String) =
        dao.changeQuantity(productId, -1, CartRepository.MAX_QUANTITY_PER_ITEM, time.now())

    override suspend fun setQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) {
            dao.delete(productId)
        } else {
            val existing = dao.get(productId)
            dao.upsert(
                CartItemEntity(
                    productId = productId,
                    quantity = quantity.coerceAtMost(CartRepository.MAX_QUANTITY_PER_ITEM),
                    addedAt = existing?.addedAt ?: time.now(),
                ),
            )
        }
    }

    override suspend fun remove(productId: String) = dao.delete(productId)

    override suspend fun restore(item: CartItem) {
        dao.upsert(
            CartItemEntity(
                productId = item.product.id,
                quantity = item.quantity.coerceIn(1, CartRepository.MAX_QUANTITY_PER_ITEM),
                addedAt = if (item.addedAt > 0) item.addedAt else time.now(),
            ),
        )
    }

    override suspend fun addAll(lines: Map<String, Int>) {
        lines.forEach { (productId, quantity) ->
            dao.changeQuantity(productId, quantity, CartRepository.MAX_QUANTITY_PER_ITEM, time.now())
        }
    }

    override suspend fun clear() = dao.clear()
}
