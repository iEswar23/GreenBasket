package io.github.ieswar23.greenbasket.domain.model

data class Address(
    val id: Long,
    val label: String,
    val receiverName: String,
    val phone: String,
    val houseDetails: String,
    val area: String,
    val city: String,
    val pincode: String,
) {
    val shortLine: String get() = "$houseDetails, $area"
    val fullLine: String get() = "$houseDetails, $area, $city - $pincode"
}
