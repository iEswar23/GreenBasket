package io.github.ieswar23.greenbasket.data.repository

import io.github.ieswar23.greenbasket.domain.model.Banner
import io.github.ieswar23.greenbasket.domain.model.Category
import io.github.ieswar23.greenbasket.domain.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data object Synced : SyncState
    data class Failed(val message: String) : SyncState
}

interface CatalogRepository {
    val syncState: StateFlow<SyncState>

    /** Fetches the catalog only when the local cache is empty. */
    suspend fun syncIfNeeded()

    /** Forces a refresh from the API and caches the result in Room. */
    suspend fun refresh(): Result<Unit>

    fun observeCategories(): Flow<List<Category>>
    suspend fun getCategory(id: String): Category?
    fun observeBanners(): Flow<List<Banner>>
    fun observeProducts(categoryId: String): Flow<List<Product>>
    fun observeProduct(id: String): Flow<Product?>
    fun observeVariants(variantGroup: String): Flow<List<Product>>
    fun observeSimilar(product: Product, limit: Int = 10): Flow<List<Product>>
    fun observeBestDeals(limit: Int = 12): Flow<List<Product>>
    fun observeBuyAgain(limit: Int = 12): Flow<List<Product>>
    fun search(query: String): Flow<List<Product>>
}
