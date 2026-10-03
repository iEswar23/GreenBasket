package io.github.ieswar23.greenbasket.data.repository

import io.github.ieswar23.greenbasket.data.local.dao.CatalogDao
import io.github.ieswar23.greenbasket.data.remote.GroceryApi
import io.github.ieswar23.greenbasket.data.toDomain
import io.github.ieswar23.greenbasket.data.toEntity
import io.github.ieswar23.greenbasket.di.IoDispatcher
import io.github.ieswar23.greenbasket.domain.model.Banner
import io.github.ieswar23.greenbasket.domain.model.Category
import io.github.ieswar23.greenbasket.domain.model.Product
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room is the single source of truth; the API is only used to (re)fill the cache.
 */
@Singleton
class OfflineFirstCatalogRepository @Inject constructor(
    private val api: GroceryApi,
    private val dao: CatalogDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CatalogRepository {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val syncMutex = Mutex()

    override suspend fun syncIfNeeded() {
        if (dao.productCount() > 0) {
            _syncState.value = SyncState.Synced
            return
        }
        refresh()
    }

    override suspend fun refresh(): Result<Unit> = syncMutex.withLock {
        _syncState.value = SyncState.Syncing
        try {
            withContext(ioDispatcher) {
                coroutineScope {
                    val categories = async { api.categories().categories.orEmpty() }
                    val products = async { api.products().products.orEmpty() }
                    val banners = async { api.banners().banners.orEmpty() }
                    dao.replaceCatalog(
                        categories = categories.await().map { it.toEntity() },
                        products = products.await().mapIndexed { index, dto -> dto.toEntity(index) },
                        banners = banners.await().mapIndexed { index, dto -> dto.toEntity(index) },
                    )
                }
            }
            _syncState.value = SyncState.Synced
            Result.success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            _syncState.value = SyncState.Failed(error.message ?: error.javaClass.simpleName)
            Result.failure(error)
        }
    }

    override fun observeCategories(): Flow<List<Category>> =
        dao.observeCategories().map { rows -> rows.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getCategory(id: String): Category? = dao.getCategory(id)?.toDomain()

    override fun observeBanners(): Flow<List<Banner>> =
        dao.observeBanners().map { rows -> rows.map { it.toDomain() } }.flowOn(ioDispatcher)

    override fun observeProducts(categoryId: String): Flow<List<Product>> =
        dao.observeProductsInCategory(categoryId).mapProducts()

    override fun observeProduct(id: String): Flow<Product?> =
        dao.observeProduct(id).map { it?.toDomain() }.flowOn(ioDispatcher)

    override fun observeVariants(variantGroup: String): Flow<List<Product>> =
        dao.observeVariants(variantGroup).mapProducts()

    override fun observeSimilar(product: Product, limit: Int): Flow<List<Product>> =
        dao.observeSimilar(product.categoryId, product.variantGroup, limit).mapProducts()

    override fun observeBestDeals(limit: Int): Flow<List<Product>> =
        dao.observeBestDeals(limit).mapProducts()

    override fun observeProductsByIds(ids: Collection<String>): Flow<List<Product>> =
        dao.observeProductsByIds(ids.toList()).mapProducts()

    override suspend fun getProducts(ids: Collection<String>): List<Product> = withContext(ioDispatcher) {
        dao.getProducts(ids.toList()).map { it.toDomain() }
    }

    override fun search(query: String): Flow<List<Product>> =
        dao.search(query.trim()).mapProducts()

    private fun Flow<List<io.github.ieswar23.greenbasket.data.local.entity.ProductEntity>>.mapProducts() =
        map { rows -> rows.map { it.toDomain() } }.flowOn(ioDispatcher)
}
