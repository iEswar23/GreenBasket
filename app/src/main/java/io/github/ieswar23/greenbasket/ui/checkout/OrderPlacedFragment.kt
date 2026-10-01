package io.github.ieswar23.greenbasket.ui.checkout

import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.FragmentOrderPlacedBinding
import io.github.ieswar23.greenbasket.domain.model.Order
import io.github.ieswar23.greenbasket.ui.common.OrderArgs
import io.github.ieswar23.greenbasket.ui.common.applySystemBarsPadding
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle
import io.github.ieswar23.greenbasket.ui.common.navigateToTab
import io.github.ieswar23.greenbasket.ui.orders.OrderDetailViewModel
import io.github.ieswar23.greenbasket.util.asRupees
import com.google.android.material.transition.MaterialFadeThrough

/** Celebration screen with an animated checkmark (AnimatedVectorDrawable). */
@AndroidEntryPoint
class OrderPlacedFragment : Fragment(R.layout.fragment_order_placed) {

    private var _binding: FragmentOrderPlacedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OrderDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOrderPlacedBinding.bind(view)
        binding.placedRoot.applySystemBarsPadding()

        if (savedInstanceState == null) playCelebration() else showFinalState()

        binding.trackOrderButton.setOnClickListener {
            val orderId = requireArguments().getString(OrderArgs.ORDER_ID) ?: return@setOnClickListener
            findNavController().navigate(
                R.id.orderDetailFragment,
                OrderArgs.bundle(orderId),
                navOptions { popUpTo(R.id.orderPlacedFragment) { inclusive = true } },
            )
        }
        binding.continueButton.setOnClickListener { continueShopping() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = continueShopping()
        })

        collectWithLifecycle(viewModel.order) { order -> order?.let(::bindOrder) }
    }

    private fun continueShopping() {
        val navController = findNavController()
        navController.popBackStack(R.id.cartFragment, inclusive = false)
        navController.navigateToTab(R.id.homeFragment)
    }

    private fun playCelebration() {
        binding.checkContainer.scaleX = 0f
        binding.checkContainer.scaleY = 0f
        binding.checkContainer.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(450)
            .setInterpolator(OvershootInterpolator(1.6f))
            .withEndAction { (binding.checkmark.drawable as? Animatable)?.start() }
            .start()
        listOf(binding.placedTitle, binding.placedEta, binding.placedCard).forEachIndexed { index, target ->
            target.alpha = 0f
            target.translationY = 40f
            target.animate().alpha(1f).translationY(0f).setStartDelay(350L + index * 90L).setDuration(350).start()
        }
    }

    private fun showFinalState() {
        (binding.checkmark.drawable as? Animatable)?.start()
    }

    private fun bindOrder(order: Order) {
        binding.placedOrderId.text = getString(R.string.order_id_format, order.id)
        binding.placedItems.text = resources.getQuantityString(R.plurals.item_count, order.itemCount, order.itemCount)
        binding.placedTotal.text = order.grandTotal.asRupees()
        binding.placedEta.text = if (order.isExpress) {
            getString(R.string.placed_eta_express)
        } else {
            getString(R.string.placed_eta_scheduled, order.slotLabel)
        }
        binding.placedAddress.text = getString(R.string.placed_address, order.addressLabel, order.addressLine)
        binding.placedSavings.isVisible = order.savings > 0
        binding.placedSavings.text = getString(R.string.placed_savings, order.savings.asRupees())
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
