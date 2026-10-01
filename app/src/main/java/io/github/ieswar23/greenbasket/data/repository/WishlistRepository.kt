package io.github.ieswar23.greenbasket.data.repository

import io.github.ieswar23.greenbasket.data.local.dao.WishlistDao
import io.github.ieswar23.greenbasket.data.toDomain
import io.github.ieswar23.greenbasket.domain.model.Product
import io.github.ieswar23.greenbasket.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface WishlistRepository {
    fun observeIds(): Flow<Set<String>>
    fun observeProducts(): Flow<List<Product>>

    /** @return true when the product is now in the wishlist. */
    suspend fun toggle(productId: String): Boolean
}

@Singleton
class RoomWishlistRepository @Inject constructor(
    private val dao: WishlistDao,
    private val time: TimeProvider,
) : WishlistRepository {
    override fun observeIds(): Flow<Set<String>> = dao.observeIds().map { it.toSet() }
    override fun observeProducts(): Flow<List<Product>> = dao.observeProducts().map { rows -> rows.map { it.toDomain() } }
    override suspend fun toggle(productId: String): Boolean = dao.toggle(productId, time.now())
}
