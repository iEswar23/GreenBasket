package io.github.ieswar23.greenbasket.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.ItemProductCardBinding
import io.github.ieswar23.greenbasket.domain.model.ProductItem
import io.github.ieswar23.greenbasket.util.asRupees

/**
 * Product cards used by every grid and horizontal carousel in the app.
 *
 * @param fixedWidthPx when set, cards get a fixed width (horizontal carousels); otherwise they fill the grid cell.
 */
class ProductAdapter(
    private val actions: ProductActions,
    private val fixedWidthPx: Int? = null,
) : ListAdapter<ProductItem, ProductAdapter.ProductViewHolder>(Diff) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).product.id.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        fixedWidthPx?.let { binding.root.layoutParams.width = it }
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) = holder.bind(getItem(position))

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_USER_STATE)) {
            holder.bindUserState(getItem(position), animate = true)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class ProductViewHolder(private val binding: ItemProductCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var current: ProductItem? = null

        init {
            binding.root.setOnClickListener { current?.let { actions.onProductClick(it.product) } }
            binding.stepper.onIncrement = { current?.let { actions.onIncrement(it.product) } }
            binding.stepper.onDecrement = { current?.let { actions.onDecrement(it.product) } }
            binding.wishlistButton.setOnClickListener { current?.let { actions.onToggleWishlist(it.product) } }
        }

        fun bind(item: ProductItem) {
            current = item
            val product = item.product
            val context = binding.root.context
            binding.imageBackground.setImageDrawable(
                android.graphics.drawable.ColorDrawable(context.illustrationTint(product.tint)),
            )
            binding.emoji.text = product.emoji
            binding.name.text = product.name
            binding.packSize.text = product.packSize
            binding.price.text = product.price.asRupees()
            binding.mrp.isVisible = product.mrp > product.price
            binding.mrp.text = product.mrp.asRupees()
            binding.mrp.setStrikeThrough()
            binding.discountBadge.isVisible = product.discountPercent > 0
            binding.discountBadge.text = context.getString(R.string.discount_off, product.discountPercent)
            binding.emoji.alpha = if (product.inStock) 1f else SOLD_OUT_ALPHA
            binding.stepper.isAvailable = product.inStock
            binding.root.contentDescription = context.getString(
                R.string.cd_product_card, product.name, product.packSize, product.price.asRupees(),
            )
            bindUserState(item, animate = false)
        }

        fun bindUserState(item: ProductItem, animate: Boolean) {
            current = item
            binding.stepper.setQuantity(item.quantity, animate)
            binding.wishlistButton.setImageResource(
                if (item.isWishlisted) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
            )
            binding.wishlistButton.isSelected = item.isWishlisted
            binding.wishlistButton.contentDescription = binding.root.context.getString(
                if (item.isWishlisted) R.string.cd_remove_wishlist else R.string.cd_add_wishlist,
            )
        }
    }

    private object Diff : DiffUtil.ItemCallback<ProductItem>() {
        override fun areItemsTheSame(oldItem: ProductItem, newItem: ProductItem) =
            oldItem.product.id == newItem.product.id

        override fun areContentsTheSame(oldItem: ProductItem, newItem: ProductItem) = oldItem == newItem

        override fun getChangePayload(oldItem: ProductItem, newItem: ProductItem): Any? =
            if (oldItem.product == newItem.product) PAYLOAD_USER_STATE else null
    }

    private companion object {
        const val PAYLOAD_USER_STATE = "user_state"
        const val SOLD_OUT_ALPHA = 0.45f
    }
}
