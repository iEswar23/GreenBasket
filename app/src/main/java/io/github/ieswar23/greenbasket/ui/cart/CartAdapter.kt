package io.github.ieswar23.greenbasket.ui.cart

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.ItemCartBillBinding
import io.github.ieswar23.greenbasket.databinding.ItemCartCouponBinding
import io.github.ieswar23.greenbasket.databinding.ItemCartNoteBinding
import io.github.ieswar23.greenbasket.databinding.ItemCartProductBinding
import io.github.ieswar23.greenbasket.databinding.ItemCartSlotBinding
import io.github.ieswar23.greenbasket.domain.CartCalculator
import io.github.ieswar23.greenbasket.domain.CouponCatalog
import io.github.ieswar23.greenbasket.domain.model.Bill
import io.github.ieswar23.greenbasket.domain.model.CartItem
import io.github.ieswar23.greenbasket.domain.model.DeliverySlot
import io.github.ieswar23.greenbasket.ui.common.BillBinder
import io.github.ieswar23.greenbasket.ui.common.SlotLabels
import io.github.ieswar23.greenbasket.ui.common.illustrationTint
import io.github.ieswar23.greenbasket.ui.common.setStrikeThrough
import io.github.ieswar23.greenbasket.util.asRupees

/** Heterogeneous rows rendered by the cart screen. */
sealed interface CartRow {
    val key: String

    data class Slot(val slot: DeliverySlot, val itemCount: Int, val amountToFreeDelivery: Int, val itemTotal: Int) : CartRow {
        override val key = "slot"
    }

    data class Item(val item: CartItem) : CartRow {
        override val key = "item_${item.product.id}"
    }

    data class CouponEntry(val bill: Bill) : CartRow {
        override val key = "coupon"
    }

    data class BillSummary(val bill: Bill) : CartRow {
        override val key = "bill"
    }

    data object Policy : CartRow {
        override val key = "policy"
    }
}

interface CartRowActions {
    fun onIncrement(item: CartItem)
    fun onDecrement(item: CartItem)
    fun onChangeSlot()
    fun onOpenCoupons()
    fun onRemoveCoupon()
}

class CartAdapter(
    private val actions: CartRowActions,
) : ListAdapter<CartRow, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is CartRow.Slot -> TYPE_SLOT
        is CartRow.Item -> TYPE_ITEM
        is CartRow.CouponEntry -> TYPE_COUPON
        is CartRow.BillSummary -> TYPE_BILL
        CartRow.Policy -> TYPE_POLICY
    }

    fun itemAt(position: Int): CartItem? = (currentList.getOrNull(position) as? CartRow.Item)?.item

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SLOT -> SlotHolder(ItemCartSlotBinding.inflate(inflater, parent, false))
            TYPE_ITEM -> ItemHolder(ItemCartProductBinding.inflate(inflater, parent, false))
            TYPE_COUPON -> CouponHolder(ItemCartCouponBinding.inflate(inflater, parent, false))
            TYPE_BILL -> BillHolder(ItemCartBillBinding.inflate(inflater, parent, false))
            else -> PolicyHolder(ItemCartNoteBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is CartRow.Slot -> (holder as SlotHolder).bind(row)
            is CartRow.Item -> (holder as ItemHolder).bind(row.item, animate = false)
            is CartRow.CouponEntry -> (holder as CouponHolder).bind(row.bill)
            is CartRow.BillSummary -> (holder as BillHolder).bind(row.bill)
            CartRow.Policy -> Unit
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        val row = getItem(position)
        if (payloads.contains(PAYLOAD_QUANTITY) && row is CartRow.Item && holder is ItemHolder) {
            holder.bind(row.item, animate = true)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    inner class SlotHolder(private val binding: ItemCartSlotBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.changeSlotButton.setOnClickListener { actions.onChangeSlot() }
        }

        fun bind(row: CartRow.Slot) {
            val context = binding.root.context
            binding.slotIcon.setImageResource(if (row.slot.isExpress) R.drawable.ic_bolt else R.drawable.ic_schedule)
            binding.slotTitle.text = if (row.slot.isExpress) {
                context.getString(R.string.cart_slot_express, DeliverySlot.EXPRESS_MINUTES)
            } else {
                SlotLabels.full(context, row.slot)
            }
            binding.slotSubtitle.text = context.resources.getQuantityString(
                R.plurals.cart_shipment_items, row.itemCount, row.itemCount,
            )
            val unlocked = row.amountToFreeDelivery == 0
            binding.freeDeliveryText.text = if (unlocked) {
                context.getString(R.string.cart_free_delivery_unlocked)
            } else {
                context.getString(R.string.cart_free_delivery_progress, row.amountToFreeDelivery.asRupees())
            }
            binding.freeDeliveryProgress.max = CartCalculator.FREE_DELIVERY_THRESHOLD
            binding.freeDeliveryProgress.setProgressCompat(
                row.itemTotal.coerceAtMost(CartCalculator.FREE_DELIVERY_THRESHOLD),
                true,
            )
        }
    }

    inner class ItemHolder(private val binding: ItemCartProductBinding) : RecyclerView.ViewHolder(binding.root) {
        private var current: CartItem? = null

        init {
            binding.itemStepper.onIncrement = { current?.let(actions::onIncrement) }
            binding.itemStepper.onDecrement = { current?.let(actions::onDecrement) }
        }

        fun bind(item: CartItem, animate: Boolean) {
            current = item
            val product = item.product
            val context = binding.root.context
            binding.thumbBackground.setImageDrawable(ColorDrawable(context.illustrationTint(product.tint)))
            binding.thumbEmoji.text = product.emoji
            binding.itemName.text = product.name
            binding.itemPack.text = product.packSize
            binding.itemPrice.text = item.lineTotal.asRupees()
            binding.itemMrp.isVisible = item.lineMrp > item.lineTotal
            binding.itemMrp.text = item.lineMrp.asRupees()
            binding.itemMrp.setStrikeThrough()
            binding.itemStepper.setQuantity(item.quantity, animate)
        }
    }

    inner class CouponHolder(private val binding: ItemCartCouponBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.couponRoot.setOnClickListener { actions.onOpenCoupons() }
        }

        fun bind(bill: Bill) {
            val context = binding.root.context
            val coupon = bill.appliedCoupon
            when {
                coupon == null -> {
                    binding.couponTitle.setText(R.string.coupon_apply)
                    binding.couponSubtitle.text = context.resources.getQuantityString(
                        R.plurals.coupon_offers_available, CouponCatalog.all.size, CouponCatalog.all.size,
                    )
                    binding.couponAction.setText(R.string.action_view)
                    binding.couponAction.setOnClickListener { actions.onOpenCoupons() }
                }
                bill.couponDiscount > 0 -> {
                    binding.couponTitle.text = context.getString(R.string.coupon_applied_title, coupon.code)
                    binding.couponSubtitle.text = context.getString(R.string.coupon_applied_saving, bill.couponDiscount.asRupees())
                    binding.couponAction.setText(R.string.action_remove)
                    binding.couponAction.setOnClickListener { actions.onRemoveCoupon() }
                }
                else -> {
                    binding.couponTitle.text = context.getString(R.string.coupon_applied_title, coupon.code)
                    binding.couponSubtitle.text = context.getString(R.string.coupon_shortfall, bill.couponShortfall.asRupees())
                    binding.couponAction.setText(R.string.action_remove)
                    binding.couponAction.setOnClickListener { actions.onRemoveCoupon() }
                }
            }
        }
    }

    class BillHolder(private val binding: ItemCartBillBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(bill: Bill) = BillBinder.bind(binding.bill, bill)
    }

    class PolicyHolder(binding: ItemCartNoteBinding) : RecyclerView.ViewHolder(binding.root)

    private object Diff : DiffUtil.ItemCallback<CartRow>() {
        override fun areItemsTheSame(oldItem: CartRow, newItem: CartRow) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: CartRow, newItem: CartRow) = oldItem == newItem
        override fun getChangePayload(oldItem: CartRow, newItem: CartRow): Any? =
            if (oldItem is CartRow.Item && newItem is CartRow.Item && oldItem.item.product == newItem.item.product) {
                PAYLOAD_QUANTITY
            } else {
                null
            }
    }

    companion object {
        const val TYPE_SLOT = 1
        const val TYPE_ITEM = 2
        const val TYPE_COUPON = 3
        const val TYPE_BILL = 4
        const val TYPE_POLICY = 5
        private const val PAYLOAD_QUANTITY = "quantity"
    }
}
