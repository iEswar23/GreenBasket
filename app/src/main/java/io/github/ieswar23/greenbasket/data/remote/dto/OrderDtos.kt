package io.github.ieswar23.greenbasket.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OrderHistoryResponse(@SerializedName("orders") val orders: List<OrderHistoryDto>?)

data class OrderHistoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("placedDaysAgo") val placedDaysAgo: Int,
    @SerializedName("hour") val hour: Int,
    @SerializedName("minute") val minute: Int,
    @SerializedName("status") val status: String,
    @SerializedName("items") val items: List<OrderLineDto>,
    @SerializedName("slotLabel") val slotLabel: String,
    @SerializedName("paymentMethod") val paymentMethod: String,
    @SerializedName("couponCode") val couponCode: String?,
)

data class OrderLineDto(
    @SerializedName("productId") val productId: String,
    @SerializedName("quantity") val quantity: Int,
)

data class PlaceOrderRequest(
    @SerializedName("items") val items: List<OrderLineDto>,
    @SerializedName("slotId") val slotId: String,
    @SerializedName("addressId") val addressId: Long,
    @SerializedName("paymentMethod") val paymentMethod: String,
    @SerializedName("couponCode") val couponCode: String?,
    @SerializedName("amount") val amount: Int,
)

data class PlaceOrderResponse(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("status") val status: String,
    @SerializedName("etaMinutes") val etaMinutes: Int,
)
