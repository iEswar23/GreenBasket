package io.github.ieswar23.greenbasket.domain.model

data class NutritionFact(val label: String, val value: String)

data class Product(
    val id: String,
    val name: String,
    val brand: String,
    val categoryId: String,
    val packSize: String,
    val price: Int,
    val mrp: Int,
    val emoji: String,
    val tint: Int,
    val variantGroup: String,
    val description: String,
    val highlights: List<String>,
    val nutrition: List<NutritionFact>,
    val rating: Float,
    val ratingCount: Int,
    /** False when the store has run out; such products can't be added to the cart. */
    val inStock: Boolean = true,
) {
    /** Whole-number discount on MRP, e.g. 18 for "18% OFF". */
    val discountPercent: Int
        get() = if (mrp > price && mrp > 0) ((mrp - price) * 100) / mrp else 0

    val savings: Int get() = (mrp - price).coerceAtLeast(0)
}

/** A product decorated with per-user state, ready to be rendered in a list. */
data class ProductItem(
    val product: Product,
    val quantity: Int = 0,
    val isWishlisted: Boolean = false,
)
