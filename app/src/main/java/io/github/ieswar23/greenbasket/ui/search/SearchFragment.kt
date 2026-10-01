package io.github.ieswar23.greenbasket.ui.search

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.FragmentSearchBinding
import io.github.ieswar23.greenbasket.domain.model.Product
import io.github.ieswar23.greenbasket.ui.common.EmptyStateBinder
import io.github.ieswar23.greenbasket.ui.common.ProductActions
import io.github.ieswar23.greenbasket.ui.common.ProductAdapter
import io.github.ieswar23.greenbasket.ui.common.SpacingItemDecoration
import io.github.ieswar23.greenbasket.ui.common.UiEvent
import io.github.ieswar23.greenbasket.ui.common.applyNavigationBarPadding
import io.github.ieswar23.greenbasket.ui.common.applyStatusBarPadding
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle
import io.github.ieswar23.greenbasket.ui.common.dp
import io.github.ieswar23.greenbasket.ui.common.productActions
import io.github.ieswar23.greenbasket.ui.common.showMessage
import io.github.ieswar23.greenbasket.ui.common.useForwardTransitions

@AndroidEntryPoint
class SearchFragment : Fragment(R.layout.fragment_search) {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        useForwardTransitions()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)
        binding.searchBarContainer.applyStatusBarPadding()
        binding.resultsList.applyNavigationBarPadding()

        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.clearButton.setOnClickListener { binding.searchInput.text?.clear() }
        binding.clearRecentButton.setOnClickListener { viewModel.clearRecent() }

        if (binding.searchInput.text.toString() != viewModel.query.value) {
            binding.searchInput.setText(viewModel.query.value)
            binding.searchInput.setSelection(binding.searchInput.length())
        }
        binding.searchInput.doAfterTextChanged { text ->
            binding.clearButton.isVisible = !text.isNullOrEmpty()
            viewModel.onQueryChanged(text?.toString().orEmpty())
        }
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.commitQuery()
                hideKeyboard()
                true
            } else {
                false
            }
        }

        val defaultActions = productActions(viewModel)
        val adapter = ProductAdapter(object : ProductActions by defaultActions {
            override fun onProductClick(product: Product) {
                viewModel.commitQuery()
                defaultActions.onProductClick(product)
            }
        })
        binding.resultsList.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.resultsList.addItemDecoration(SpacingItemDecoration(10.dp))
        binding.resultsList.adapter = adapter
        binding.resultsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) hideKeyboard()
            }
        })

        val trending = resources.getStringArray(R.array.search_trending_terms).toList()
        bindSuggestionChips(binding.trendingChips, trending, removable = false)

        collectWithLifecycle(viewModel.recentSearches) { recent ->
            binding.recentHeader.isVisible = recent.isNotEmpty()
            binding.recentChips.isVisible = recent.isNotEmpty()
            bindSuggestionChips(binding.recentChips, recent, removable = true)
        }
        collectWithLifecycle(viewModel.results) { results -> render(results, adapter) }
        collectWithLifecycle(viewModel.events) { event -> if (event is UiEvent.Message) showMessage(event) }

        if (savedInstanceState == null && viewModel.query.value.isEmpty()) {
            binding.searchInput.requestFocus()
            WindowCompat.getInsetsController(requireActivity().window, binding.searchInput).show(WindowInsetsCompat.Type.ime())
        }
    }

    private fun render(results: SearchResults, adapter: ProductAdapter) {
        binding.progress.visibility = if (results is SearchResults.Loading) View.VISIBLE else View.INVISIBLE
        binding.idleContainer.isVisible = results is SearchResults.Idle
        binding.resultsContainer.isVisible = results is SearchResults.Found
        binding.emptyState.root.isVisible = results is SearchResults.Empty
        when (results) {
            is SearchResults.Found -> {
                binding.resultsHeader.text = resources.getQuantityString(
                    R.plurals.search_results_header, results.items.size, results.items.size, results.query,
                )
                adapter.submitList(results.items)
            }
            is SearchResults.Empty -> EmptyStateBinder.bind(
                binding = binding.emptyState,
                emoji = "🧺",
                title = getString(R.string.search_empty_title, results.query),
                message = getString(R.string.search_empty_message),
            )
            else -> Unit
        }
    }

    private fun bindSuggestionChips(group: ChipGroup, terms: List<String>, removable: Boolean) {
        group.removeAllViews()
        terms.forEach { term ->
            val chip = layoutInflater.inflate(R.layout.item_filter_chip, group, false) as Chip
            chip.text = term
            chip.isCheckable = false
            chip.isCloseIconVisible = removable
            chip.setOnClickListener {
                binding.searchInput.setText(term)
                binding.searchInput.setSelection(term.length)
                viewModel.onSuggestionSelected(term)
                hideKeyboard()
            }
            chip.closeIconContentDescription = getString(R.string.cd_remove_recent, term)
            chip.setOnCloseIconClickListener { viewModel.removeRecent(term) }
            group.addView(chip)
        }
    }

    private fun hideKeyboard() {
        WindowCompat.getInsetsController(requireActivity().window, binding.searchInput).hide(WindowInsetsCompat.Type.ime())
        binding.searchInput.clearFocus()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
