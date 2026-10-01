package io.github.ieswar23.greenbasket.ui.productlist

import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.domain.model.SortOption

val SortOption.labelRes: Int
    get() = when (this) {
        SortOption.RELEVANCE -> R.string.sort_relevance
        SortOption.PRICE_LOW_TO_HIGH -> R.string.sort_price_low_high
        SortOption.PRICE_HIGH_TO_LOW -> R.string.sort_price_high_low
        SortOption.DISCOUNT -> R.string.sort_discount
        SortOption.NAME -> R.string.sort_name
    }

/** Discount thresholds offered in the filter sheet. */
val DISCOUNT_STEPS = listOf(0, 10, 20, 30)
