package io.github.ieswar23.greenbasket.ui.checkout

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.FragmentCheckoutBinding
import io.github.ieswar23.greenbasket.databinding.ItemSummaryLineBinding
import io.github.ieswar23.greenbasket.domain.model.CartItem
import io.github.ieswar23.greenbasket.domain.model.PaymentMethod
import io.github.ieswar23.greenbasket.ui.cart.DeliverySlotBottomSheet
import io.github.ieswar23.greenbasket.ui.common.BillBinder
import io.github.ieswar23.greenbasket.ui.common.OrderArgs
import io.github.ieswar23.greenbasket.ui.common.SlotLabels
import io.github.ieswar23.greenbasket.ui.common.applyNavigationBarPadding
import io.github.ieswar23.greenbasket.ui.common.applyStatusBarPadding
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle
import io.github.ieswar23.greenbasket.ui.common.illustrationTint
import io.github.ieswar23.greenbasket.ui.common.useForwardTransitions
import io.github.ieswar23.greenbasket.ui.profile.AddressBottomSheet
import io.github.ieswar23.greenbasket.util.asRupees

@AndroidEntryPoint
class CheckoutFragment : Fragment(R.layout.fragment_checkout) {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CheckoutViewModel by viewModels()

    private var renderedItems: List<CartItem>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        useForwardTransitions()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCheckoutBinding.bind(view)
        renderedItems = null
        binding.appBar.applyStatusBarPadding()
        binding.placeOrderContent.applyNavigationBarPadding()
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.changeAddressButton.setOnClickListener {
            AddressBottomSheet().show(childFragmentManager, AddressBottomSheet.TAG)
        }
        binding.changeSlotButton.setOnClickListener {
            DeliverySlotBottomSheet().show(childFragmentManager, DeliverySlotBottomSheet.TAG)
        }
        binding.paymentGroup.setOnCheckedChangeListener { _, checkedId ->
            val method = when (checkedId) {
                R.id.payCard -> PaymentMethod.CARD
                R.id.payCash -> PaymentMethod.CASH
                else -> PaymentMethod.UPI
            }
            if (method != viewModel.uiState.value.paymentMethod) viewModel.selectPayment(method)
        }
        binding.placeOrderButton.setOnClickListener {
            val slot = viewModel.uiState.value.slot ?: return@setOnClickListener
            viewModel.placeOrder(SlotLabels.full(requireContext(), slot))
        }

        collectWithLifecycle(viewModel.uiState, action = ::render)
        collectWithLifecycle(viewModel.events) { event ->
            when (event) {
                is CheckoutEvent.OrderPlaced -> findNavController().navigate(
                    R.id.orderPlacedFragment,
                    OrderArgs.bundle(event.orderId),
                    navOptions { popUpTo(R.id.cartFragment) { inclusive = false } },
                )
                is CheckoutEvent.Failed -> Snackbar
                    .make(binding.root, event.message ?: getString(R.string.checkout_failed), Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.placeOrderBar)
                    .show()
            }
        }
    }

    private fun render(state: CheckoutUiState) {
        if (state.isLoading) return
        val context = requireContext()
        state.address?.let { address ->
            binding.addressTitle.text = getString(R.string.checkout_delivering_to, address.label)
            binding.addressLine.text = getString(R.string.checkout_address_line, address.receiverName, address.fullLine)
        } ?: run {
            binding.addressTitle.setText(R.string.checkout_no_address)
            binding.addressLine.text = null
        }
        state.slot?.let { slot ->
            binding.slotLabel.text = SlotLabels.full(context, slot)
            binding.slotIcon.setImageResource(if (slot.isExpress) R.drawable.ic_bolt else R.drawable.ic_schedule)
        }
        val checkedId = when (state.paymentMethod) {
            PaymentMethod.UPI -> R.id.payUpi
            PaymentMethod.CARD -> R.id.payCard
            PaymentMethod.CASH -> R.id.payCash
        }
        if (binding.paymentGroup.checkedRadioButtonId != checkedId) binding.paymentGroup.check(checkedId)

        if (renderedItems != state.items) {
            renderedItems = state.items
            binding.summaryItems.removeAllViews()
            state.items.forEach { item ->
                val line = ItemSummaryLineBinding.inflate(layoutInflater, binding.summaryItems, true)
                line.lineEmoji.text = item.product.emoji
                line.lineEmoji.background.mutate().setTint(context.illustrationTint(item.product.tint))
                line.lineName.text = item.product.name
                line.lineDetail.text = getString(R.string.checkout_line_detail, item.product.packSize, item.quantity)
                line.lineTotal.text = item.lineTotal.asRupees()
            }
        }
        BillBinder.bind(binding.bill, state.bill)

        binding.placeOrderButton.isEnabled = state.canPlaceOrder
        binding.placeOrderButton.text = if (state.isPlacing) "" else getString(R.string.checkout_place_order, state.bill.grandTotal.asRupees())
        binding.placingProgress.isVisible = state.isPlacing
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
