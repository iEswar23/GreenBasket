package io.github.ieswar23.greenbasket.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.FragmentOrdersBinding
import io.github.ieswar23.greenbasket.databinding.ItemOrderBinding
import io.github.ieswar23.greenbasket.domain.model.Order
import io.github.ieswar23.greenbasket.ui.common.EmptyStateBinder
import io.github.ieswar23.greenbasket.ui.common.OrderArgs
import io.github.ieswar23.greenbasket.ui.common.UiState
import io.github.ieswar23.greenbasket.ui.common.applyStatusBarPadding
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle
import io.github.ieswar23.greenbasket.ui.common.illustrationTint
import io.github.ieswar23.greenbasket.ui.common.navigateForward
import io.github.ieswar23.greenbasket.ui.common.navigateToTab
import io.github.ieswar23.greenbasket.ui.common.restoreTabExitTransition
import io.github.ieswar23.greenbasket.ui.common.useTabTransitions
import io.github.ieswar23.greenbasket.util.DateFormats
import io.github.ieswar23.greenbasket.util.asRupees

@AndroidEntryPoint
class OrdersFragment : Fragment(R.layout.fragment_orders) {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OrdersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        useTabTransitions()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOrdersBinding.bind(view)
        restoreTabExitTransition()
        binding.appBar.applyStatusBarPadding()

        val adapter = OrdersAdapter { order -> navigateForward(R.id.orderDetailFragment, OrderArgs.bundle(order.id)) }
        binding.orderList.layoutManager = LinearLayoutManager(requireContext())
        binding.orderList.adapter = adapter

        collectWithLifecycle(viewModel.uiState) { state ->
            binding.orderList.isVisible = state is UiState.Content
            binding.emptyState.root.isVisible = state is UiState.Empty
            when (state) {
                is UiState.Content -> {
                    adapter.submitList(state.data)
                    binding.toolbar.subtitle = resources.getQuantityString(R.plurals.order_count, state.data.size, state.data.size)
                }
                UiState.Empty -> EmptyStateBinder.bind(
                    binding = binding.emptyState,
                    emoji = "🧾",
                    title = getString(R.string.orders_empty_title),
                    message = getString(R.string.orders_empty_message),
                    actionText = getString(R.string.cart_empty_action),
                    onAction = { findNavController().navigateToTab(R.id.homeFragment) },
                )
                else -> Unit
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

private class OrdersAdapter(
    private val onClick: (Order) -> Unit,
) : ListAdapter<Order, OrdersAdapter.Holder>(Diff) {

    inner class Holder(private val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: Order) {
            val context = binding.root.context
            binding.orderId.text = context.getString(R.string.order_id_format, order.id)
            binding.orderDate.text = DateFormats.orderDate(order.placedAt)
            binding.statusChip.bindStatus(order.status)
            binding.orderSummary.text = context.getString(
                R.string.orders_summary,
                context.resources.getQuantityString(R.plurals.item_count, order.itemCount, order.itemCount),
                order.grandTotal.asRupees(),
            )
            binding.emojiStrip.removeAllViews()
            val inflater = LayoutInflater.from(context)
            order.items.take(MAX_THUMBS).forEach { item ->
                val thumb = inflater.inflate(R.layout.view_emoji_thumb, binding.emojiStrip, false) as TextView
                thumb.text = item.emoji
                thumb.background.mutate().setTint(context.illustrationTint(item.tint))
                binding.emojiStrip.addView(thumb)
            }
            val extra = order.items.size - MAX_THUMBS
            if (extra > 0) {
                val more = inflater.inflate(R.layout.view_emoji_thumb, binding.emojiStrip, false) as TextView
                more.textSize = 13f
                more.text = context.getString(R.string.orders_more_items, extra)
                binding.emojiStrip.addView(more)
            }
            binding.root.setOnClickListener { onClick(order) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Order, newItem: Order) = oldItem == newItem
    }

    private companion object {
        const val MAX_THUMBS = 5
    }
}
