package io.github.ieswar23.greenbasket.ui.cart

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.FragmentCartBinding
import io.github.ieswar23.greenbasket.domain.model.CartItem
import io.github.ieswar23.greenbasket.ui.common.EmptyStateBinder
import io.github.ieswar23.greenbasket.ui.common.applyStatusBarPadding
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle
import io.github.ieswar23.greenbasket.ui.common.navigateForward
import io.github.ieswar23.greenbasket.ui.common.navigateToTab
import io.github.ieswar23.greenbasket.ui.common.restoreTabExitTransition
import io.github.ieswar23.greenbasket.ui.common.useTabTransitions
import io.github.ieswar23.greenbasket.util.asRupees

@AndroidEntryPoint
class CartFragment : Fragment(R.layout.fragment_cart), CartRowActions {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        useTabTransitions()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCartBinding.bind(view)
        restoreTabExitTransition()
        binding.appBar.applyStatusBarPadding()

        val adapter = CartAdapter(this)
        binding.cartList.layoutManager = LinearLayoutManager(requireContext())
        binding.cartList.adapter = adapter
        ItemTouchHelper(SwipeToRemoveCallback { position ->
            adapter.itemAt(position)?.let(viewModel::remove)
        }).attachToRecyclerView(binding.cartList)

        binding.checkoutButton.setOnClickListener { navigateForward(R.id.checkoutFragment) }

        collectWithLifecycle(viewModel.uiState) { state -> render(state, adapter) }
        collectWithLifecycle(viewModel.events, action = ::handleEvent)
    }

    private fun render(state: CartUiState, adapter: CartAdapter) {
        binding.emptyState.root.isVisible = state.isEmpty
        binding.cartList.isVisible = !state.isEmpty && !state.isLoading
        binding.checkoutBar.isVisible = !state.isEmpty && !state.isLoading
        // Don't lay out placeholder rows (coupon/bill) before the cart has loaded: inserting the items
        // above them afterwards would keep them anchored and open the list scrolled to the bottom.
        if (state.isLoading) return

        if (state.isEmpty) {
            binding.toolbar.subtitle = null
            EmptyStateBinder.bind(
                binding = binding.emptyState,
                emoji = "🛒",
                title = getString(R.string.cart_empty_title),
                message = getString(R.string.cart_empty_message),
                actionText = getString(R.string.cart_empty_action),
                onAction = { findNavController().navigateToTab(R.id.homeFragment) },
            )
            adapter.submitList(emptyList())
            return
        }
        val bill = state.bill
        binding.toolbar.subtitle = resources.getQuantityString(R.plurals.item_count, bill.itemCount, bill.itemCount)
        binding.barTotal.text = getString(R.string.cart_bar_total, bill.grandTotal.asRupees())
        binding.barSavings.isVisible = bill.totalSavings > 0
        binding.barSavings.text = getString(R.string.cart_bar_savings, bill.totalSavings.asRupees())

        val rows = buildList {
            state.slot?.let { add(CartRow.Slot(it, bill.itemCount, bill.amountToFreeDelivery, bill.itemTotal)) }
            state.items.forEach { add(CartRow.Item(it)) }
            add(CartRow.CouponEntry(bill))
            add(CartRow.BillSummary(bill))
            add(CartRow.Policy)
        }
        adapter.submitList(rows)
    }

    private fun handleEvent(event: CartEvent) {
        val root = view ?: return
        val anchor = binding.checkoutBar.takeIf { it.isVisible }
        when (event) {
            is CartEvent.ItemRemoved -> Snackbar
                .make(root, getString(R.string.cart_item_removed, event.item.product.name), Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo) { viewModel.undoRemove(event.item) }
                .setAnchorView(anchor)
                .show()
            is CartEvent.CouponApplied -> Snackbar
                .make(root, getString(R.string.coupon_applied_message, event.code, event.discount.asRupees()), Snackbar.LENGTH_SHORT)
                .setAnchorView(anchor)
                .show()
            is CartEvent.CouponShortfall -> Snackbar
                .make(root, getString(R.string.coupon_shortfall_message, event.shortfall.asRupees(), event.code), Snackbar.LENGTH_LONG)
                .setAnchorView(anchor)
                .show()
            CartEvent.CouponInvalid -> Snackbar
                .make(root, R.string.coupon_invalid, Snackbar.LENGTH_SHORT)
                .setAnchorView(anchor)
                .show()
        }
    }

    override fun onIncrement(item: CartItem) = viewModel.increment(item)

    override fun onDecrement(item: CartItem) = viewModel.decrement(item)

    override fun onChangeSlot() {
        if (childFragmentManager.findFragmentByTag(DeliverySlotBottomSheet.TAG) == null) {
            DeliverySlotBottomSheet().show(childFragmentManager, DeliverySlotBottomSheet.TAG)
        }
    }

    override fun onOpenCoupons() {
        if (childFragmentManager.findFragmentByTag(CouponBottomSheet.TAG) == null) {
            CouponBottomSheet().show(childFragmentManager, CouponBottomSheet.TAG)
        }
    }

    override fun onRemoveCoupon() = viewModel.removeCoupon()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
