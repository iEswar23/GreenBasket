package io.github.ieswar23.greenbasket.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.FragmentCategoriesBinding
import io.github.ieswar23.greenbasket.databinding.ItemCategoryCardBinding
import io.github.ieswar23.greenbasket.domain.model.Category
import io.github.ieswar23.greenbasket.ui.common.CategoryArgs
import io.github.ieswar23.greenbasket.ui.common.SpacingItemDecoration
import io.github.ieswar23.greenbasket.ui.common.UiState
import io.github.ieswar23.greenbasket.ui.common.applyStatusBarPadding
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle
import io.github.ieswar23.greenbasket.ui.common.dp
import io.github.ieswar23.greenbasket.ui.common.illustrationTint
import io.github.ieswar23.greenbasket.ui.common.navigateForward
import io.github.ieswar23.greenbasket.ui.common.restoreTabExitTransition
import io.github.ieswar23.greenbasket.ui.common.useTabTransitions

@AndroidEntryPoint
class CategoriesFragment : Fragment(R.layout.fragment_categories) {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CategoriesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        useTabTransitions()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCategoriesBinding.bind(view)
        restoreTabExitTransition()
        binding.appBar.applyStatusBarPadding()
        binding.toolbar.setOnMenuItemClickListener {
            navigateForward(R.id.searchFragment)
            true
        }

        val adapter = CategoryCardAdapter { category ->
            navigateForward(R.id.productListFragment, CategoryArgs.bundle(category.id))
        }
        binding.categoryList.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.categoryList.addItemDecoration(SpacingItemDecoration(12.dp))
        binding.categoryList.adapter = adapter

        collectWithLifecycle(viewModel.uiState) { state ->
            binding.skeleton.isVisible = state is UiState.Loading
            binding.categoryList.isVisible = state is UiState.Content
            if (state is UiState.Content) adapter.submitList(state.data)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

private class CategoryCardAdapter(
    private val onClick: (Category) -> Unit,
) : ListAdapter<Category, CategoryCardAdapter.Holder>(Diff) {

    inner class Holder(private val binding: ItemCategoryCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category) {
            val context = binding.root.context
            binding.categoryCard.setCardBackgroundColor(context.illustrationTint(category.tint))
            binding.categoryName.text = category.name
            binding.categorySubtitle.text = category.subtitle
            binding.categoryEmoji.text = category.emoji
            binding.categoryCount.text = context.resources.getQuantityString(
                R.plurals.item_count, category.productCount, category.productCount,
            )
            binding.root.setOnClickListener { onClick(category) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemCategoryCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Category, newItem: Category) = oldItem == newItem
    }
}
