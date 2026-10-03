package io.github.ieswar23.greenbasket.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CategoriesResponse(@SerializedName("categories") val categories: List<CategoryDto>?)

data class CategoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("emoji") val emoji: String,
    @SerializedName("tint") val tint: String,
    @SerializedName("sortOrder") val sortOrder: Int,
)

data class ProductsResponse(@SerializedName("products") val products: List<ProductDto>?)

data class NutritionDto(
    @SerializedName("label") val label: String,
    @SerializedName("value") val value: String,
)

data class ProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("brand") val brand: String,
    @SerializedName("categoryId") val categoryId: String,
    @SerializedName("packSize") val packSize: String,
    @SerializedName("price") val price: Int,
    @SerializedName("mrp") val mrp: Int,
    @SerializedName("emoji") val emoji: String,
    @SerializedName("tint") val tint: String,
    @SerializedName("variantGroup") val variantGroup: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("highlights") val highlights: List<String>?,
    @SerializedName("nutrition") val nutrition: List<NutritionDto>?,
    @SerializedName("keywords") val keywords: String?,
    @SerializedName("rating") val rating: Float?,
    @SerializedName("ratingCount") val ratingCount: Int?,
    @SerializedName("inStock") val inStock: Boolean?,
)

data class BannersResponse(@SerializedName("banners") val banners: List<BannerDto>?)

data class BannerDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("emoji") val emoji: String,
    @SerializedName("startColor") val startColor: String,
    @SerializedName("endColor") val endColor: String,
    @SerializedName("cta") val cta: String,
    @SerializedName("categoryId") val categoryId: String,
)
