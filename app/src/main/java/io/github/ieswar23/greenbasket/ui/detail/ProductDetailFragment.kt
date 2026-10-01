package io.github.ieswar23.greenbasket.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.FragmentProductDetailBinding
import io.github.ieswar23.greenbasket.databinding.ItemNutritionRowBinding
import io.github.ieswar23.greenbasket.domain.model.Product
import io.github.ieswar23.greenbasket.ui.common.ProductAdapter
import io.github.ieswar23.greenbasket.ui.common.SlotLabels
import io.github.ieswar23.greenbasket.ui.common.SpacingItemDecoration
import io.github.ieswar23.greenbasket.ui.common.UiEvent
import io.github.ieswar23.greenbasket.ui.common.applyNavigationBarPadding
import io.github.ieswar23.greenbasket.ui.common.applyStatusBarHeight
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle
import io.github.ieswar23.greenbasket.ui.common.dp
import io.github.ieswar23.greenbasket.ui.common.illustrationTint
import io.github.ieswar23.greenbasket.ui.common.navigateToTab
import io.github.ieswar23.greenbasket.ui.common.productActions
import io.github.ieswar23.greenbasket.ui.common.setStrikeThrough
import io.github.ieswar23.greenbasket.ui.common.showMessage
import io.github.ieswar23.greenbasket.ui.common.useForwardTransitions
import io.github.ieswar23.greenbasket.util.asRupees
import java.text.NumberFormat

@AndroidEntryPoint
class ProductDetailFragment : Fragment(R.layout.fragment_product_detail) {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductDetailViewModel by viewModels()

    private var renderedProductId: String? = null
    private lateinit var similarAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        useForwardTransitions()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductDetailBinding.bind(view)
        renderedProductId = null
        binding.toolbar.applyStatusBarHeight()
        binding.bottomBarContent.applyNavigationBarPadding()
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        similarAdapter = ProductAdapter(productActions(viewModel), resources.getDimensionPixelSize(R.dimen.product_card_carousel_width))
        binding.similarList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding.similarList.addItemDecoration(SpacingItemDecoration(8.dp))
        binding.similarList.adapter = similarAdapter

        binding.viewCartRow.setOnClickListener { findNavController().navigateToTab(R.id.cartFragment) }

        collectWithLifecycle(viewModel.uiState, action = ::render)
        collectWithLifecycle(viewModel.events) { event ->
            if (event is UiEvent.Message) showMessage(event, anchor = binding.bottomBar)
        }
    }

    private fun render(state: ProductDetailUiState) {
        when (state) {
            ProductDetailUiState.Loading -> Unit
            ProductDetailUiState.NotFound -> renderNotFound()
            is ProductDetailUiState.Content -> renderContent(state)
        }
    }

    private fun renderNotFound() {
        binding.contentScroll.isVisible = false
        binding.bottomBar.isVisible = false
        Snackbar.make(binding.root, R.string.detail_not_found, Snackbar.LENGTH_LONG).show()
    }

    private fun renderContent(state: ProductDetailUiState.Content) {
        val product = state.product
        val productChanged = renderedProductId != product.id
        if (productChanged) {
            bindStaticProduct(product)
            renderedProductId = product.id
        }
        bindVariants(state.variants, product.id)
        similarAdapter.submitList(state.similar)
        binding.similarTitle.isVisible = state.similar.isNotEmpty()
        binding.similarList.isVisible = state.similar.isNotEmpty()

        binding.deliveryInfo.text = getString(R.string.detail_delivery_by, SlotLabels.full(requireContext(), state.slot))

        val wishlistItem = binding.toolbar.menu.findItem(R.id.action_wishlist)
        wishlistItem.setIcon(if (state.isWishlisted) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
        wishlistItem.title = getString(if (state.isWishlisted) R.string.cd_remove_wishlist else R.string.cd_add_wishlist)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_wishlist -> viewModel.toggleWishlist(product).let { true }
                R.id.action_share -> share(product).let { true }
                else -> false
            }
        }

        binding.barStepper.onIncrement = { viewModel.increment(product) }
        binding.barStepper.onDecrement = { viewModel.decrement(product) }
        binding.barStepper.setQuantity(state.quantity, animate = !productChanged)

        binding.viewCartRow.isVisible = state.cartItemCount > 0
        binding.viewCartText.text = resources.getQuantityString(
            R.plurals.cart_items_in_cart, state.cartItemCount, state.cartItemCount,
        )
    }

    private fun bindStaticProduct(product: Product) {
        val context = requireContext()
        binding.hero.setBackgroundColor(context.illustrationTint(product.tint))
        binding.collapsingToolbar.setContentScrimColor(
            MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceContainerLowest),
        )
        binding.collapsingToolbar.title = product.name
        binding.heroEmoji.text = product.emoji
        binding.heroEmoji.scaleX = 0.85f
        binding.heroEmoji.scaleY = 0.85f
        binding.heroEmoji.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
        binding.heroDiscount.isVisible = product.discountPercent > 0
        binding.heroDiscount.text = getString(R.string.discount_off, product.discountPercent)

        binding.brand.text = product.brand
        binding.name.text = product.name
        binding.rating.text = product.rating.toString()
        binding.ratingCount.text = getString(
            R.string.detail_rating_count,
            NumberFormat.getIntegerInstance().format(product.ratingCount.toLong()),
        )
        binding.price.text = product.price.asRupees()
        binding.mrp.isVisible = product.mrp > product.price
        binding.mrp.text = getString(R.string.detail_mrp, product.mrp.asRupees())
        binding.mrp.setStrikeThrough()
        binding.discount.isVisible = product.discountPercent > 0
        binding.discount.text = getString(R.string.discount_off, product.discountPercent)

        binding.barPack.text = product.packSize
        binding.barPrice.text = product.price.asRupees()
        binding.barMrp.isVisible = product.mrp > product.price
        binding.barMrp.text = product.mrp.asRupees()
        binding.barMrp.setStrikeThrough()

        binding.description.text = product.description
        binding.highlightsContainer.removeAllViews()
        product.highlights.forEach { highlight ->
            val row = layoutInflater.inflate(R.layout.item_highlight, binding.highlightsContainer, false) as TextView
            row.text = highlight
            binding.highlightsContainer.addView(row)
        }

        binding.nutritionSection.isVisible = product.nutrition.isNotEmpty()
        binding.nutritionTable.removeAllViews()
        product.nutrition.forEach { fact ->
            val row = ItemNutritionRowBinding.inflate(layoutInflater, binding.nutritionTable, true)
            row.nutritionLabel.text = fact.label
            row.nutritionValue.text = fact.value
        }
    }

    private fun bindVariants(variants: List<Product>, selectedId: String) {
        val showVariants = variants.size > 1
        binding.variantsTitle.isVisible = showVariants
        binding.variantGroup.isVisible = showVariants
        if (!showVariants) return
        val existing = (0 until binding.variantGroup.childCount).map { binding.variantGroup.getChildAt(it).tag }
        if (existing != variants.map { it.id }) {
            binding.variantGroup.removeAllViews()
            variants.forEach { variant ->
                val chip = layoutInflater.inflate(R.layout.item_choice_chip, binding.variantGroup, false) as Chip
                chip.id = View.generateViewId()
                chip.tag = variant.id
                chip.text = getString(R.string.detail_variant_chip, variant.packSize, variant.price.asRupees())
                chip.isCheckable = true
                chip.setOnClickListener { viewModel.selectVariant(variant.id) }
                binding.variantGroup.addView(chip)
            }
        }
        for (i in 0 until binding.variantGroup.childCount) {
            val chip = binding.variantGroup.getChildAt(i) as Chip
            chip.isChecked = chip.tag == selectedId
        }
    }

    private fun share(product: Product) {
        val text = getString(R.string.share_product_text, product.name, product.packSize, product.price.asRupees())
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
