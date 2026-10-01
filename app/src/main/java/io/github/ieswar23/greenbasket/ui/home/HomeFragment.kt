package io.github.ieswar23.greenbasket.ui.home

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.FragmentHomeBinding
import io.github.ieswar23.greenbasket.ui.common.CategoryArgs
import io.github.ieswar23.greenbasket.ui.common.EmptyStateBinder
import io.github.ieswar23.greenbasket.ui.common.SlotLabels
import io.github.ieswar23.greenbasket.ui.common.UiEvent
import io.github.ieswar23.greenbasket.ui.common.applyStatusBarPadding
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle
import io.github.ieswar23.greenbasket.ui.common.navigateForward
import io.github.ieswar23.greenbasket.ui.common.productActions
import io.github.ieswar23.greenbasket.ui.common.resetStatusBarAppearance
import io.github.ieswar23.greenbasket.ui.common.restoreTabExitTransition
import io.github.ieswar23.greenbasket.ui.common.setStatusBarAppearance
import io.github.ieswar23.greenbasket.ui.common.showMessage
import io.github.ieswar23.greenbasket.ui.common.themeColor
import io.github.ieswar23.greenbasket.ui.common.useTabTransitions
import io.github.ieswar23.greenbasket.ui.profile.AddressBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var bannerAdapter: BannerSectionAdapter
    private lateinit var categoryHeader: SectionHeaderAdapter
    private lateinit var categoryAdapter: CategoryTileAdapter
    private lateinit var dealsHeader: SectionHeaderAdapter
    private lateinit var dealsAdapter: ProductCarouselAdapter
    private lateinit var buyAgainHeader: SectionHeaderAdapter
    private lateinit var buyAgainAdapter: ProductCarouselAdapter
    private lateinit var footerAdapter: FooterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        useTabTransitions()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        restoreTabExitTransition()
        binding.appBar.applyStatusBarPadding()

        setupList()
        binding.searchBar.setOnClickListener { navigateForward(R.id.searchFragment) }
        binding.addressRow.setOnClickListener {
            AddressBottomSheet().show(childFragmentManager, AddressBottomSheet.TAG)
        }
        binding.swipeRefresh.setColorSchemeColors(requireContext().themeColor(androidx.appcompat.R.attr.colorPrimary))
        binding.swipeRefresh.setProgressBackgroundColorSchemeColor(
            requireContext().themeColor(com.google.android.material.R.attr.colorSurfaceContainerHigh),
        )
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        collectWithLifecycle(viewModel.uiState, action = ::render)
        collectWithLifecycle(viewModel.header) { header ->
            header ?: return@collectWithLifecycle
            binding.deliveryEta.text = SlotLabels.headline(requireContext(), header.slot)
            binding.deliveryLabel.setText(
                if (header.slot.isExpress) R.string.home_delivery_label else R.string.home_delivery_scheduled_label,
            )
            binding.addressLabel.text = header.address?.label?.uppercase() ?: getString(R.string.home_add_address)
            binding.addressLine.text = header.address?.shortLine.orEmpty()
        }
        collectWithLifecycle(viewModel.isRefreshing) { binding.swipeRefresh.isRefreshing = it }
        collectWithLifecycle(viewModel.events) { event ->
            if (event is UiEvent.Message) showMessage(event)
        }
        startRotatingSearchHints()
    }

    private fun setupList() {
        val actions = productActions(viewModel)
        val cardWidth = resources.getDimensionPixelSize(R.dimen.product_card_carousel_width)
        bannerAdapter = BannerSectionAdapter(viewLifecycleOwner) { banner ->
            navigateForward(R.id.productListFragment, CategoryArgs.bundle(banner.categoryId))
        }
        categoryHeader = SectionHeaderAdapter(R.string.home_section_categories)
        categoryAdapter = CategoryTileAdapter { category ->
            navigateForward(R.id.productListFragment, CategoryArgs.bundle(category.id))
        }
        dealsHeader = SectionHeaderAdapter(R.string.home_section_deals, R.string.home_section_deals_subtitle)
        dealsAdapter = ProductCarouselAdapter(actions, cardWidth)
        buyAgainHeader = SectionHeaderAdapter(R.string.home_section_buy_again, R.string.home_section_buy_again_subtitle)
        buyAgainAdapter = ProductCarouselAdapter(actions, cardWidth)
        footerAdapter = FooterAdapter()

        val concat = ConcatAdapter(
            ConcatAdapter.Config.Builder().setIsolateViewTypes(false).build(),
            bannerAdapter,
            categoryHeader,
            categoryAdapter,
            dealsHeader,
            dealsAdapter,
            buyAgainHeader,
            buyAgainAdapter,
            footerAdapter,
        )
        val layoutManager = GridLayoutManager(requireContext(), CATEGORY_SPAN)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (concat.getItemViewType(position) == HomeViewTypes.CATEGORY) 1 else CATEGORY_SPAN
        }
        binding.homeList.layoutManager = layoutManager
        binding.homeList.adapter = concat
    }

    private fun render(state: HomeUiState) {
        binding.skeleton.isVisible = state is HomeUiState.Loading
        binding.errorState.root.isVisible = state is HomeUiState.Error
        binding.swipeRefresh.isVisible = state is HomeUiState.Content
        when (state) {
            HomeUiState.Loading -> Unit
            is HomeUiState.Error -> EmptyStateBinder.bind(
                binding = binding.errorState,
                emoji = "📡",
                title = getString(R.string.error_title),
                message = getString(R.string.error_catalog_message),
                actionText = getString(R.string.action_retry),
                onAction = viewModel::refresh,
            )
            is HomeUiState.Content -> {
                bannerAdapter.submit(state.banners)
                categoryHeader.isShown = state.categories.isNotEmpty()
                categoryAdapter.submitList(state.categories)
                dealsAdapter.submit(state.bestDeals)
                dealsHeader.isShown = state.bestDeals.isNotEmpty()
                buyAgainAdapter.submit(state.buyAgain)
                buyAgainHeader.isShown = state.buyAgain.isNotEmpty()
                footerAdapter.isShown = true
            }
        }
    }

    private fun startRotatingSearchHints() {
        val hints = resources.getStringArray(R.array.search_hint_terms)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var index = 0
                while (true) {
                    val hintView = binding.searchHint
                    hintView.text = getString(R.string.search_hint_format, hints[index % hints.size])
                    hintView.alpha = 0f
                    hintView.translationY = 12f
                    hintView.animate().alpha(1f).translationY(0f).setDuration(250).start()
                    delay(HINT_ROTATION_MS)
                    index++
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setStatusBarAppearance(lightBackground = false)
    }

    override fun onPause() {
        resetStatusBarAppearance()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.homeList.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val CATEGORY_SPAN = 4
        const val HINT_ROTATION_MS = 2_600L
    }
}
