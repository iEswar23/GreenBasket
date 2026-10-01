package io.github.ieswar23.greenbasket.domain

/** Raw form input for a new delivery address. */
data class AddressInput(
    val label: String,
    val receiverName: String,
    val phone: String,
    val houseDetails: String,
    val area: String,
    val city: String,
    val pincode: String,
)

enum class AddressField { NAME, PHONE, HOUSE, AREA, CITY, PINCODE }

object AddressValidator {

    /** Returns the set of invalid fields; empty when the address can be saved. */
    fun validate(input: AddressInput): Set<AddressField> = buildSet {
        if (input.receiverName.trim().length < 2) add(AddressField.NAME)
        if (normalizePhone(input.phone) == null) add(AddressField.PHONE)
        if (input.houseDetails.isBlank()) add(AddressField.HOUSE)
        if (input.area.isBlank()) add(AddressField.AREA)
        if (input.city.isBlank()) add(AddressField.CITY)
        if (!PINCODE.matches(input.pincode.trim())) add(AddressField.PINCODE)
    }

    /** Accepts 10-digit Indian mobile numbers with an optional +91 / 0 prefix and formats them. */
    fun normalizePhone(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        val local = when {
            digits.length == 12 && digits.startsWith("91") -> digits.drop(2)
            digits.length == 11 && digits.startsWith("0") -> digits.drop(1)
            else -> digits
        }
        if (local.length != 10 || local.first() !in '6'..'9') return null
        return "+91 ${local.take(5)} ${local.drop(5)}"
    }

    private val PINCODE = Regex("^[1-9][0-9]{5}$")
}
