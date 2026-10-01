package io.github.ieswar23.greenbasket.ui.orders

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.FragmentOrderDetailBinding
import io.github.ieswar23.greenbasket.databinding.ItemSummaryLineBinding
import io.github.ieswar23.greenbasket.databinding.ItemTimelineStepBinding
import io.github.ieswar23.greenbasket.domain.OrderStatusResolver
import io.github.ieswar23.greenbasket.domain.model.Order
import io.github.ieswar23.greenbasket.domain.model.OrderStatus
import io.github.ieswar23.greenbasket.domain.model.PaymentMethod
import io.github.ieswar23.greenbasket.ui.common.BillBinder
import io.github.ieswar23.greenbasket.ui.common.applyNavigationBarPadding
import io.github.ieswar23.greenbasket.ui.common.applyStatusBarPadding
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle
import io.github.ieswar23.greenbasket.ui.common.illustrationTint
import io.github.ieswar23.greenbasket.ui.common.navigateToTab
import io.github.ieswar23.greenbasket.ui.common.themeColor
import io.github.ieswar23.greenbasket.ui.common.useForwardTransitions
import io.github.ieswar23.greenbasket.util.DateFormats
import io.github.ieswar23.greenbasket.util.asRupees

@AndroidEntryPoint
class OrderDetailFragment : Fragment(R.layout.fragment_order_detail) {

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OrderDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        useForwardTransitions()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOrderDetailBinding.bind(view)
        binding.appBar.applyStatusBarPadding()
        binding.reorderContent.applyNavigationBarPadding()
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.reorderButton.setOnClickListener { viewModel.reorder() }

        collectWithLifecycle(viewModel.order) { order -> order?.let(::render) }
        collectWithLifecycle(viewModel.reordered) { count ->
            Snackbar.make(
                binding.root,
                resources.getQuantityString(R.plurals.reorder_added, count, count),
                Snackbar.LENGTH_LONG,
            ).setAction(R.string.action_view_cart) {
                findNavController().navigateToTab(R.id.cartFragment)
            }.setAnchorView(binding.reorderBar).show()
        }
    }

    private fun render(order: Order) {
        val context = requireContext()
        val style = OrderStatusStyle.of(order.status)
        binding.toolbar.title = getString(R.string.order_id_format, order.id)
        binding.statusHeadline.setText(style.headline)
        binding.statusChip.bindStatus(order.status)
        binding.placedOn.text = getString(R.string.order_placed_on, DateFormats.orderDate(order.placedAt))
        renderTimeline(order.status)

        binding.itemsTitle.text = resources.getQuantityString(R.plurals.order_items_title, order.itemCount, order.itemCount)
        binding.itemsContainer.removeAllViews()
        order.items.forEach { item ->
            val line = ItemSummaryLineBinding.inflate(layoutInflater, binding.itemsContainer, true)
            line.lineEmoji.text = item.emoji
            line.lineEmoji.background.mutate().setTint(context.illustrationTint(item.tint))
            line.lineName.text = item.name
            line.lineDetail.text = getString(R.string.checkout_line_detail, item.packSize, item.quantity)
            line.lineTotal.text = item.lineTotal.asRupees()
        }
        BillBinder.bind(binding.bill, order)

        binding.deliveryAddress.text = getString(R.string.placed_address, order.addressLabel, order.addressLine)
        binding.deliverySlot.text = order.slotLabel
        binding.paymentMethod.setText(
            when (order.paymentMethod) {
                PaymentMethod.UPI -> R.string.order_paid_upi
                PaymentMethod.CARD -> R.string.order_paid_card
                PaymentMethod.CASH -> R.string.order_paid_cash
            },
        )
    }

    private fun renderTimeline(status: OrderStatus) {
        binding.timeline.isVisible = status != OrderStatus.CANCELLED
        if (status == OrderStatus.CANCELLED) return
        val steps = OrderStatusResolver.timeline
        val currentIndex = steps.indexOf(status)
        val context = requireContext()
        val active = context.themeColor(androidx.appcompat.R.attr.colorPrimary)
        val onActive = context.themeColor(com.google.android.material.R.attr.colorOnPrimary)
        val inactive = context.themeColor(com.google.android.material.R.attr.colorSurfaceContainerHighest)
        val onInactive = context.themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        val inactiveLine = context.themeColor(com.google.android.material.R.attr.colorOutlineVariant)

        binding.timeline.removeAllViews()
        steps.forEachIndexed { index, step ->
            val view = ItemTimelineStepBinding.inflate(layoutInflater, binding.timeline, true)
            val reached = index <= currentIndex
            val stepStyle = OrderStatusStyle.of(step)
            view.stepDot.backgroundTintList = ColorStateList.valueOf(if (reached) active else inactive)
            view.stepIcon.setImageResource(if (index < currentIndex) R.drawable.ic_check else stepStyle.icon)
            view.stepIcon.imageTintList = ColorStateList.valueOf(if (reached) onActive else onInactive)
            view.stepLabel.setText(stepStyle.label)
            view.stepLabel.setTextColor(if (reached) active else onInactive)
            view.stepLineStart.visibility = if (index > 0) View.VISIBLE else View.INVISIBLE
            view.stepLineEnd.visibility = if (index < steps.lastIndex) View.VISIBLE else View.INVISIBLE
            view.stepLineStart.setBackgroundColor(if (reached) active else inactiveLine)
            view.stepLineEnd.setBackgroundColor(if (index < currentIndex) active else inactiveLine)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
