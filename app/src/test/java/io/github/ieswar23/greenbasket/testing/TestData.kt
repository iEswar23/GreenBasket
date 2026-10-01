package io.github.ieswar23.greenbasket.testing

import io.github.ieswar23.greenbasket.domain.model.CartItem
import io.github.ieswar23.greenbasket.domain.model.Product

object TestData {

    fun product(
        id: String,
        name: String = id.replaceFirstChar { it.uppercase() },
        price: Int = 50,
        mrp: Int = price,
        brand: String = "FarmFresh",
        categoryId: String = "dairy_eggs",
        variantGroup: String = id,
    ) = Product(
        id = id,
        name = name,
        brand = brand,
        categoryId = categoryId,
        packSize = "500 g",
        price = price,
        mrp = mrp,
        emoji = "🥛",
        tint = 0,
        variantGroup = variantGroup,
        description = "",
        highlights = emptyList(),
        nutrition = emptyList(),
        rating = 4.5f,
        ratingCount = 100,
    )

    fun line(product: Product, quantity: Int) = CartItem(product, quantity)

    val milk = product("milk", "Toned Milk", price = 54, mrp = 54, brand = "DailyGold")
    val paneer = product("paneer", "Fresh Paneer", price = 89, mrp = 95, brand = "FarmFresh")
    val curd = product("curd", "Fresh Curd", price = 35, mrp = 40, brand = "FarmFresh")
    val ghee = product("ghee", "Pure Cow Ghee", price = 329, mrp = 365, brand = "DailyGold")
    val cheese = product("cheese", "Cheese Slices", price = 145, mrp = 160, brand = "FarmFresh")
    val eggs = product("eggs", "Brown Eggs", price = 69, mrp = 84, brand = "HappyHens")

    /** Catalog order used as "relevance". */
    val dairy = listOf(milk, paneer, curd, ghee, cheese, eggs)
}
