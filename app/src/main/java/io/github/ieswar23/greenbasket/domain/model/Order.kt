package io.github.ieswar23.greenbasket.domain.model

enum class OrderStatus { PLACED, PACKED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED }

enum class PaymentMethod { UPI, CARD, CASH }

data class OrderItem(
    val productId: String,
    val name: String,
    val packSize: String,
    val emoji: String,
    val tint: Int,
    val price: Int,
    val mrp: Int,
    val quantity: Int,
) {
    val lineTotal: Int get() = price * quantity
}

data class Order(
    val id: String,
    val placedAt: Long,
    val status: OrderStatus,
    val items: List<OrderItem>,
    val slotLabel: String,
    val isExpress: Boolean,
    val slotStartMillis: Long,
    val addressLabel: String,
    val addressLine: String,
    val paymentMethod: PaymentMethod,
    val itemTotal: Int,
    val mrpTotal: Int,
    val deliveryFee: Int,
    val handlingFee: Int,
    val couponCode: String?,
    val couponDiscount: Int,
    val grandTotal: Int,
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
    val savings: Int get() = (mrpTotal - itemTotal).coerceAtLeast(0) + couponDiscount
}
