package io.github.ieswar23.greenbasket.testing

import io.github.ieswar23.greenbasket.domain.model.CartItem
import io.github.ieswar23.greenbasket.domain.model.Order
import io.github.ieswar23.greenbasket.domain.model.OrderItem
import io.github.ieswar23.greenbasket.domain.model.OrderStatus
import io.github.ieswar23.greenbasket.domain.model.PaymentMethod
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
        inStock: Boolean = true,
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
        inStock = inStock,
    )

    fun line(product: Product, quantity: Int) = CartItem(product, quantity)

    /** A past order; each line is snapshotted at the product's price unless [paidPrices] overrides it. */
    fun order(
        id: String,
        placedAt: Long,
        vararg lines: Pair<Product, Int>,
        status: OrderStatus = OrderStatus.DELIVERED,
        paidPrices: Map<String, Int> = emptyMap(),
    ): Order {
        val items = lines.map { (product, quantity) ->
            OrderItem(
                productId = product.id,
                name = product.name,
                packSize = product.packSize,
                emoji = product.emoji,
                tint = product.tint,
                price = paidPrices[product.id] ?: product.price,
                mrp = product.mrp,
                quantity = quantity,
            )
        }
        val itemTotal = items.sumOf { it.lineTotal }
        return Order(
            id = id,
            placedAt = placedAt,
            status = status,
            items = items,
            slotLabel = "Express · 15 mins",
            isExpress = true,
            slotStartMillis = placedAt,
            addressLabel = "Home",
            addressLine = "Flat 1204, Lotus Residency, Powai",
            paymentMethod = PaymentMethod.UPI,
            itemTotal = itemTotal,
            mrpTotal = items.sumOf { it.mrp * it.quantity },
            deliveryFee = 0,
            handlingFee = 4,
            couponCode = null,
            couponDiscount = 0,
            grandTotal = itemTotal + 4,
        )
    }

    val milk = product("milk", "Toned Milk", price = 54, mrp = 54, brand = "DailyGold")
    val paneer = product("paneer", "Fresh Paneer", price = 89, mrp = 95, brand = "FarmFresh")
    val curd = product("curd", "Fresh Curd", price = 35, mrp = 40, brand = "FarmFresh")
    val ghee = product("ghee", "Pure Cow Ghee", price = 329, mrp = 365, brand = "DailyGold")
    val cheese = product("cheese", "Cheese Slices", price = 145, mrp = 160, brand = "FarmFresh")
    val eggs = product("eggs", "Brown Eggs", price = 69, mrp = 84, brand = "HappyHens")

    /** Catalog order used as "relevance". */
    val dairy = listOf(milk, paneer, curd, ghee, cheese, eggs)
}
