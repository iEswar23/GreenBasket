package io.github.ieswar23.greenbasket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.ieswar23.greenbasket.data.local.entity.BannerEntity
import io.github.ieswar23.greenbasket.data.local.entity.CategoryEntity
import io.github.ieswar23.greenbasket.data.local.entity.CategoryWithCount
import io.github.ieswar23.greenbasket.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {

    @Query(
        """
        SELECT c.id, c.name, c.subtitle, c.emoji, c.tint, COUNT(p.id) AS productCount
        FROM categories c LEFT JOIN products p ON p.categoryId = c.id
        GROUP BY c.id ORDER BY c.sortOrder
        """,
    )
    fun observeCategories(): Flow<List<CategoryWithCount>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategory(id: String): CategoryEntity?

    @Query("SELECT * FROM banners ORDER BY position")
    fun observeBanners(): Flow<List<BannerEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY position")
    fun observeProductsInCategory(categoryId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeProduct(id: String): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id IN (:ids)")
    suspend fun getProducts(ids: List<String>): List<ProductEntity>

    @Query("SELECT * FROM products WHERE variantGroup = :group ORDER BY price")
    fun observeVariants(group: String): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * FROM products
        WHERE categoryId = :categoryId AND variantGroup != :excludeGroup
        ORDER BY (mrp - price) * 100 / mrp DESC, position LIMIT :limit
        """,
    )
    fun observeSimilar(categoryId: String, excludeGroup: String, limit: Int): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * FROM products WHERE mrp > price
        ORDER BY (mrp - price) * 100 / mrp DESC, ratingCount DESC LIMIT :limit
        """,
    )
    fun observeBestDeals(limit: Int): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id IN (:ids) ORDER BY position")
    fun observeProductsByIds(ids: List<String>): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * FROM products
        WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%'
        ORDER BY CASE WHEN name LIKE :query || '%' THEN 0 WHEN name LIKE '%' || :query || '%' THEN 1 ELSE 2 END, position
        """,
    )
    fun search(query: String): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun productCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(items: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(items: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBanners(items: List<BannerEntity>)

    @Query("DELETE FROM banners")
    suspend fun clearBanners()

    @Transaction
    suspend fun replaceCatalog(
        categories: List<CategoryEntity>,
        products: List<ProductEntity>,
        banners: List<BannerEntity>,
    ) {
        upsertCategories(categories)
        upsertProducts(products)
        clearBanners()
        upsertBanners(banners)
    }
}
