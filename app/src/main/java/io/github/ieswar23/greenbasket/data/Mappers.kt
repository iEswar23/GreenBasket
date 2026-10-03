package io.github.ieswar23.greenbasket.data

import android.graphics.Color
import io.github.ieswar23.greenbasket.data.local.entity.AddressEntity
import io.github.ieswar23.greenbasket.data.local.entity.BannerEntity
import io.github.ieswar23.greenbasket.data.local.entity.CategoryEntity
import io.github.ieswar23.greenbasket.data.local.entity.CategoryWithCount
import io.github.ieswar23.greenbasket.data.local.entity.OrderItemEntity
import io.github.ieswar23.greenbasket.data.local.entity.OrderWithItems
import io.github.ieswar23.greenbasket.data.local.entity.ProductEntity
import io.github.ieswar23.greenbasket.data.remote.dto.BannerDto
import io.github.ieswar23.greenbasket.data.remote.dto.CategoryDto
import io.github.ieswar23.greenbasket.data.remote.dto.ProductDto
import io.github.ieswar23.greenbasket.domain.OrderStatusResolver
import io.github.ieswar23.greenbasket.domain.model.Address
import io.github.ieswar23.greenbasket.domain.model.Banner
import io.github.ieswar23.greenbasket.domain.model.Category
import io.github.ieswar23.greenbasket.domain.model.NutritionFact
import io.github.ieswar23.greenbasket.domain.model.Order
import io.github.ieswar23.greenbasket.domain.model.OrderItem
import io.github.ieswar23.greenbasket.domain.model.OrderStatus
import io.github.ieswar23.greenbasket.domain.model.PaymentMethod
import io.github.ieswar23.greenbasket.domain.model.Product

private const val FALLBACK_TINT = 0xFFE8F5E9.toInt()
private const val NUTRITION_SEPARATOR = "="

fun parseColor(hex: String, fallback: Int = FALLBACK_TINT): Int =
    runCatching { Color.parseColor(hex) }.getOrDefault(fallback)

fun CategoryDto.toEntity() = CategoryEntity(
    id = id,
    name = name,
    subtitle = subtitle.orEmpty(),
    emoji = emoji,
    tint = tint,
    sortOrder = sortOrder,
)

fun ProductDto.toEntity(position: Int) = ProductEntity(
    id = id,
    name = name,
    brand = brand,
    categoryId = categoryId,
    packSize = packSize,
    price = price,
    mrp = maxOf(mrp, price),
    emoji = emoji,
    tint = tint,
    variantGroup = variantGroup ?: id,
    description = description.orEmpty(),
    highlights = highlights.orEmpty(),
    nutrition = nutrition.orEmpty().map { it.label + NUTRITION_SEPARATOR + it.value },
    keywords = keywords.orEmpty(),
    rating = rating ?: 4.2f,
    ratingCount = ratingCount ?: 0,
    position = position,
    inStock = inStock ?: true,
)

fun BannerDto.toEntity(position: Int) = BannerEntity(
    id = id,
    title = title,
    subtitle = subtitle,
    emoji = emoji,
    startColor = startColor,
    endColor = endColor,
    cta = cta,
    categoryId = categoryId,
    position = position,
)

fun CategoryWithCount.toDomain() = Category(
    id = id,
    name = name,
    subtitle = subtitle,
    emoji = emoji,
    tint = parseColor(tint),
    productCount = productCount,
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    subtitle = subtitle,
    emoji = emoji,
    tint = parseColor(tint),
)

fun ProductEntity.toDomain() = Product(
    id = id,
    name = name,
    brand = brand,
    categoryId = categoryId,
    packSize = packSize,
    price = price,
    mrp = mrp,
    emoji = emoji,
    tint = parseColor(tint),
    variantGroup = variantGroup,
    description = description,
    highlights = highlights,
    nutrition = nutrition.mapNotNull { entry ->
        val parts = entry.split(NUTRITION_SEPARATOR, limit = 2)
        if (parts.size == 2) NutritionFact(parts[0], parts[1]) else null
    },
    rating = rating,
    ratingCount = ratingCount,
    inStock = inStock,
)

fun BannerEntity.toDomain() = Banner(
    id = id,
    title = title,
    subtitle = subtitle,
    emoji = emoji,
    startColor = parseColor(startColor),
    endColor = parseColor(endColor),
    cta = cta,
    categoryId = categoryId,
)

fun AddressEntity.toDomain() = Address(
    id = id,
    label = label,
    receiverName = receiverName,
    phone = phone,
    houseDetails = houseDetails,
    area = area,
    city = city,
    pincode = pincode,
)

fun Address.toEntity() = AddressEntity(
    id = id,
    label = label,
    receiverName = receiverName,
    phone = phone,
    houseDetails = houseDetails,
    area = area,
    city = city,
    pincode = pincode,
)

fun Product.toOrderItemEntity(orderId: String, quantity: Int) = OrderItemEntity(
    orderId = orderId,
    productId = id,
    name = name,
    packSize = packSize,
    emoji = emoji,
    tint = String.format("#%06X", 0xFFFFFF and tint),
    price = price,
    mrp = mrp,
    quantity = quantity,
)

fun OrderWithItems.toDomain(now: Long): Order {
    val stored = runCatching { OrderStatus.valueOf(order.status) }.getOrDefault(OrderStatus.PLACED)
    return Order(
        id = order.id,
        placedAt = order.placedAt,
        status = OrderStatusResolver.resolve(stored, order.placedAt, order.isExpress, order.slotStartMillis, now),
        items = items.map {
            OrderItem(
                productId = it.productId,
                name = it.name,
                packSize = it.packSize,
                emoji = it.emoji,
                tint = parseColor(it.tint),
                price = it.price,
                mrp = it.mrp,
                quantity = it.quantity,
            )
        },
        slotLabel = order.slotLabel,
        isExpress = order.isExpress,
        slotStartMillis = order.slotStartMillis,
        addressLabel = order.addressLabel,
        addressLine = order.addressLine,
        paymentMethod = runCatching { PaymentMethod.valueOf(order.paymentMethod) }.getOrDefault(PaymentMethod.UPI),
        itemTotal = order.itemTotal,
        mrpTotal = order.mrpTotal,
        deliveryFee = order.deliveryFee,
        handlingFee = order.handlingFee,
        couponCode = order.couponCode,
        couponDiscount = order.couponDiscount,
        grandTotal = order.grandTotal,
    )
}
