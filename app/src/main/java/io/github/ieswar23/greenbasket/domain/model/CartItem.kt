package io.github.ieswar23.greenbasket.domain.model

data class CartItem(
    val product: Product,
    val quantity: Int,
    /** When the product was first added; used to keep cart ordering stable (e.g. on undo). */
    val addedAt: Long = 0L,
) {
    val lineTotal: Int get() = product.price * quantity
    val lineMrp: Int get() = product.mrp * quantity
}
