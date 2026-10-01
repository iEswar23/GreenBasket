package io.github.ieswar23.greenbasket.ui.common

import io.github.ieswar23.greenbasket.domain.model.Product

interface ProductActions {
    fun onProductClick(product: Product)
    fun onIncrement(product: Product)
    fun onDecrement(product: Product)
    fun onToggleWishlist(product: Product)
}
