package io.github.ieswar23.greenbasket.ui.productlist

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.FragmentProductListBinding
import io.github.ieswar23.greenbasket.domain.model.ProductFilter
import io.github.ieswar23.greenbasket.ui.common.EmptyStateBinder
import io.github.ieswar23.greenbasket.ui.common.ProductAdapter
import io.github.ieswar23.greenbasket.ui.common.SpacingItemDecoration
import io.github.ieswar23.greenbasket.ui.common.UiEvent
import io.github.ieswar23.greenbasket.ui.common.UiState
import io.github.ieswar23.greenbasket.ui.common.applyStatusBarPadding
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle
import io.github.ieswar23.greenbasket.ui.common.dp
import io.github.ieswar23.greenbasket.ui.common.navigateForward
import io.github.ieswar23.greenbasket.ui.common.productActions
import io.github.ieswar23.greenbasket.ui.common.showMessage
import io.github.ieswar23.greenbasket.ui.common.themeColor
import io.github.ieswar23.greenbasket.ui.common.useForwardTransitions

@AndroidEntryPoint
class ProductListFragment : Fragment(R.layout.fragment_product_list) {

    private var _binding: FragmentProductListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        useForwardTransitions()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductListBinding.bind(view)
        binding.appBar.applyStatusBarPadding()
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.setOnMenuItemClickListener {
            navigateForward(R.id.searchFragment)
            true
        }

        val adapter = ProductAdapter(productActions(viewModel))
        binding.productGrid.layoutManager = GridLayoutManager(requireContext(), SPAN_COUNT)
        binding.productGrid.addItemDecoration(SpacingItemDecoration(10.dp))
        binding.productGrid.adapter = adapter

        binding.sortChip.setOnClickListener { openSheet() }
        binding.sortChip.setOnCloseIconClickListener { openSheet() }
        binding.filterChip.setOnClickListener { openSheet() }
        binding.dealsChip.setOnClickListener {
            val current = viewModel.uiState.value.filter
            val minDiscount = if (current.minDiscount >= QUICK_DEAL_DISCOUNT) 0 else QUICK_DEAL_DISCOUNT
            viewModel.setFilter(current.copy(minDiscount = minDiscount))
        }

        binding.swipeRefresh.setColorSchemeColors(requireContext().themeColor(androidx.appcompat.R.attr.colorPrimary))
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        collectWithLifecycle(viewModel.category) { category ->
            binding.toolbar.title = category?.name ?: getString(R.string.title_products)
        }
        collectWithLifecycle(viewModel.isRefreshing) { binding.swipeRefresh.isRefreshing = it }
        collectWithLifecycle(viewModel.events) { event -> if (event is UiEvent.Message) showMessage(event) }
        collectWithLifecycle(viewModel.uiState) { state -> render(state, adapter) }
    }

    private fun render(state: ProductListUiState, adapter: ProductAdapter) {
        val content = state.content
        binding.skeleton.isVisible = content is UiState.Loading
        binding.swipeRefresh.isVisible = content is UiState.Content
        binding.emptyState.root.isVisible = content is UiState.Empty
        binding.toolbar.subtitle = if (content is UiState.Content) {
            resources.getQuantityString(R.plurals.item_count, state.resultCount, state.resultCount)
        } else {
            null
        }
        binding.sortChip.text = getString(state.sort.labelRes)
        val activeFilters = state.filter.activeCount
        binding.filterChip.text = if (activeFilters > 0) {
            getString(R.string.filter_title_count, activeFilters)
        } else {
            getString(R.string.filter_title)
        }
        binding.dealsChip.isChecked = state.filter.minDiscount >= QUICK_DEAL_DISCOUNT

        when (content) {
            is UiState.Content -> adapter.submitList(content.data)
            UiState.Empty -> EmptyStateBinder.bind(
                binding = binding.emptyState,
                emoji = "🔍",
                title = getString(R.string.filter_empty_title),
                message = getString(R.string.filter_empty_message),
                actionText = getString(R.string.action_clear_filters),
                onAction = { viewModel.applySortAndFilter(state.sort, ProductFilter.NONE) },
            )
            else -> Unit
        }
    }

    private fun openSheet() {
        if (childFragmentManager.findFragmentByTag(SortFilterBottomSheet.TAG) == null) {
            SortFilterBottomSheet().show(childFragmentManager, SortFilterBottomSheet.TAG)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val SPAN_COUNT = 2
        const val QUICK_DEAL_DISCOUNT = 20
    }
}
