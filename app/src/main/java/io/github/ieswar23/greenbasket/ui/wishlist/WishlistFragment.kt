package io.github.ieswar23.greenbasket.ui.wishlist

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.FragmentWishlistBinding
import io.github.ieswar23.greenbasket.ui.common.EmptyStateBinder
import io.github.ieswar23.greenbasket.ui.common.ProductAdapter
import io.github.ieswar23.greenbasket.ui.common.SpacingItemDecoration
import io.github.ieswar23.greenbasket.ui.common.UiEvent
import io.github.ieswar23.greenbasket.ui.common.UiState
import io.github.ieswar23.greenbasket.ui.common.applyStatusBarPadding
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle
import io.github.ieswar23.greenbasket.ui.common.dp
import io.github.ieswar23.greenbasket.ui.common.navigateToTab
import io.github.ieswar23.greenbasket.ui.common.productActions
import io.github.ieswar23.greenbasket.ui.common.showMessage
import io.github.ieswar23.greenbasket.ui.common.useForwardTransitions

@AndroidEntryPoint
class WishlistFragment : Fragment(R.layout.fragment_wishlist) {

    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WishlistViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        useForwardTransitions()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWishlistBinding.bind(view)
        binding.appBar.applyStatusBarPadding()
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val adapter = ProductAdapter(productActions(viewModel))
        binding.wishlistGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.wishlistGrid.addItemDecoration(SpacingItemDecoration(10.dp))
        binding.wishlistGrid.adapter = adapter

        collectWithLifecycle(viewModel.uiState) { state ->
            binding.wishlistGrid.isVisible = state is UiState.Content
            binding.emptyState.root.isVisible = state is UiState.Empty
            when (state) {
                is UiState.Content -> {
                    adapter.submitList(state.data)
                    binding.toolbar.subtitle = resources.getQuantityString(R.plurals.item_count, state.data.size, state.data.size)
                }
                UiState.Empty -> {
                    binding.toolbar.subtitle = null
                    EmptyStateBinder.bind(
                        binding = binding.emptyState,
                        emoji = "💚",
                        title = getString(R.string.wishlist_empty_title),
                        message = getString(R.string.wishlist_empty_message),
                        actionText = getString(R.string.cart_empty_action),
                        onAction = { findNavController().navigateToTab(R.id.homeFragment) },
                    )
                }
                else -> Unit
            }
        }
        collectWithLifecycle(viewModel.events) { event -> if (event is UiEvent.Message) showMessage(event) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
