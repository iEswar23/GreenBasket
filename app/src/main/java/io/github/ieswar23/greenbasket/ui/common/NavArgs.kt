package io.github.ieswar23.greenbasket.ui.common

import androidx.core.os.bundleOf

/** Argument keys shared by the navigation graph (no Safe Args). */
object ProductArgs {
    const val PRODUCT_ID = "productId"
    fun bundle(productId: String) = bundleOf(PRODUCT_ID to productId)
}

object CategoryArgs {
    const val CATEGORY_ID = "categoryId"
    fun bundle(categoryId: String) = bundleOf(CATEGORY_ID to categoryId)
}

object OrderArgs {
    const val ORDER_ID = "orderId"
    fun bundle(orderId: String) = bundleOf(ORDER_ID to orderId)
}
