package io.github.ieswar23.greenbasket.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subtitle: String,
    val emoji: String,
    val tint: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "products",
    indices = [Index("categoryId"), Index("variantGroup")],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String,
    val categoryId: String,
    val packSize: String,
    val price: Int,
    val mrp: Int,
    val emoji: String,
    val tint: String,
    val variantGroup: String,
    val description: String,
    val highlights: List<String>,
    /** Nutrition facts encoded as "label=value" pairs. */
    val nutrition: List<String>,
    val keywords: String,
    val rating: Float,
    val ratingCount: Int,
    /** Position in the remote catalog, used for "relevance" ordering. */
    val position: Int,
    @ColumnInfo(defaultValue = "1") val inStock: Boolean = true,
)

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val startColor: String,
    val endColor: String,
    val cta: String,
    val categoryId: String,
    val position: Int,
)

/** Category row joined with the number of products it contains. */
data class CategoryWithCount(
    val id: String,
    val name: String,
    val subtitle: String,
    val emoji: String,
    val tint: String,
    val productCount: Int,
)
