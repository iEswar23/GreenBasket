package io.github.ieswar23.greenbasket.ui.home

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.ItemBannerBinding
import io.github.ieswar23.greenbasket.databinding.ItemCategoryTileBinding
import io.github.ieswar23.greenbasket.databinding.ItemHomeBannersBinding
import io.github.ieswar23.greenbasket.databinding.ItemHomeFooterBinding
import io.github.ieswar23.greenbasket.databinding.ItemProductCarouselBinding
import io.github.ieswar23.greenbasket.databinding.ItemSectionHeaderBinding
import io.github.ieswar23.greenbasket.domain.model.Banner
import io.github.ieswar23.greenbasket.domain.model.Category
import io.github.ieswar23.greenbasket.domain.model.ProductItem
import io.github.ieswar23.greenbasket.ui.common.ProductActions
import io.github.ieswar23.greenbasket.ui.common.ProductAdapter
import io.github.ieswar23.greenbasket.ui.common.SpacingItemDecoration
import io.github.ieswar23.greenbasket.ui.common.dp
import io.github.ieswar23.greenbasket.ui.common.illustrationTint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * View types are shared across the home ConcatAdapter (isolateViewTypes = false) so the
 * GridLayoutManager span lookup can tell category tiles apart from full-width sections.
 */
object HomeViewTypes {
    const val BANNERS = 101
    const val HEADER = 102
    const val CATEGORY = 103
    const val CAROUSEL = 104
    const val FOOTER = 105
}

/** Base for adapters that render a single, optionally hidden, row. */
abstract class SingleRowAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    var isShown: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) notifyItemInserted(0) else notifyItemRemoved(0)
        }

    override fun getItemCount(): Int = if (isShown) 1 else 0
}

class SectionHeaderAdapter(
    private val title: Int,
    private val subtitle: Int? = null,
) : SingleRowAdapter<SectionHeaderAdapter.Holder>() {

    class Holder(val binding: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = HomeViewTypes.HEADER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemSectionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.sectionTitle.setText(title)
        holder.binding.sectionSubtitle.isVisible = subtitle != null
        subtitle?.let { holder.binding.sectionSubtitle.setText(it) }
    }
}

class FooterAdapter : SingleRowAdapter<FooterAdapter.Holder>() {
    class Holder(binding: ItemHomeFooterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = HomeViewTypes.FOOTER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemHomeFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = Unit
}

class CategoryTileAdapter(
    private val onClick: (Category) -> Unit,
) : ListAdapter<Category, CategoryTileAdapter.Holder>(CategoryDiff) {

    inner class Holder(private val binding: ItemCategoryTileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category) {
            val context = binding.root.context
            binding.tileCard.setCardBackgroundColor(context.illustrationTint(category.tint))
            binding.tileEmoji.text = category.emoji
            binding.tileName.text = category.name
            binding.root.contentDescription = category.name
            binding.root.setOnClickListener { onClick(category) }
        }
    }

    override fun getItemViewType(position: Int) = HomeViewTypes.CATEGORY

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemCategoryTileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object CategoryDiff : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Category, newItem: Category) = oldItem == newItem
    }
}

/** A full-width row hosting a horizontally scrolling list of product cards. */
class ProductCarouselAdapter(
    actions: ProductActions,
    cardWidthPx: Int,
) : SingleRowAdapter<ProductCarouselAdapter.Holder>() {

    private val productAdapter = ProductAdapter(actions, fixedWidthPx = cardWidthPx)

    class Holder(val binding: ItemProductCarouselBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.carousel.layoutManager =
                LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false)
            binding.carousel.addItemDecoration(SpacingItemDecoration(8.dp, includeEdge = false))
            binding.carousel.itemAnimator = null
        }
    }

    fun submit(items: List<ProductItem>) {
        productAdapter.submitList(items)
        isShown = items.isNotEmpty()
    }

    override fun getItemViewType(position: Int) = HomeViewTypes.CAROUSEL

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemProductCarouselBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        if (holder.binding.carousel.adapter !== productAdapter) {
            holder.binding.carousel.adapter = productAdapter
        }
    }
}

/** Auto-scrolling offer banners with a dot indicator. */
class BannerSectionAdapter(
    private val lifecycleOwner: LifecycleOwner,
    onBannerClick: (Banner) -> Unit,
) : SingleRowAdapter<BannerSectionAdapter.Holder>() {

    private val pagerAdapter = BannerPagerAdapter(onBannerClick)

    fun submit(banners: List<Banner>) {
        pagerAdapter.submitList(banners)
        isShown = banners.isNotEmpty()
    }

    inner class Holder(val binding: ItemHomeBannersBinding) : RecyclerView.ViewHolder(binding.root) {
        private var autoScrollJob: Job? = null

        init {
            binding.bannerPager.adapter = pagerAdapter
            binding.bannerPager.offscreenPageLimit = 1
            TabLayoutMediator(binding.bannerIndicator, binding.bannerPager) { tab, position ->
                tab.contentDescription = binding.root.context.getString(R.string.cd_banner_page, position + 1)
            }.attach()
            binding.bannerPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    // Restart the countdown whenever the user interacts with the pager.
                    if (state == ViewPager2.SCROLL_STATE_DRAGGING) startAutoScroll()
                }
            })
        }

        fun startAutoScroll() {
            autoScrollJob?.cancel()
            autoScrollJob = lifecycleOwner.lifecycleScope.launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    while (isActive) {
                        delay(AUTO_SCROLL_MS)
                        val count = pagerAdapter.itemCount
                        if (count > 1 && binding.bannerPager.scrollState == ViewPager2.SCROLL_STATE_IDLE) {
                            binding.bannerPager.setCurrentItem((binding.bannerPager.currentItem + 1) % count, true)
                        }
                    }
                }
            }
        }

        fun stopAutoScroll() {
            autoScrollJob?.cancel()
            autoScrollJob = null
        }
    }

    override fun getItemViewType(position: Int) = HomeViewTypes.BANNERS

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemHomeBannersBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = Unit

    override fun onViewAttachedToWindow(holder: Holder) = holder.startAutoScroll()

    override fun onViewDetachedFromWindow(holder: Holder) = holder.stopAutoScroll()

    private companion object {
        const val AUTO_SCROLL_MS = 3_500L
    }
}

class BannerPagerAdapter(
    private val onClick: (Banner) -> Unit,
) : ListAdapter<Banner, BannerPagerAdapter.Holder>(BannerDiff) {

    inner class Holder(private val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(banner: Banner) {
            binding.bannerRoot.background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(banner.startColor, banner.endColor),
            )
            binding.bannerTitle.text = banner.title
            binding.bannerSubtitle.text = banner.subtitle
            binding.bannerEmoji.text = banner.emoji
            binding.bannerCta.text = banner.cta
            binding.root.contentDescription = "${banner.title}. ${banner.subtitle}"
            binding.root.setOnClickListener { onClick(banner) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object BannerDiff : DiffUtil.ItemCallback<Banner>() {
        override fun areItemsTheSame(oldItem: Banner, newItem: Banner) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Banner, newItem: Banner) = oldItem == newItem
    }
}
