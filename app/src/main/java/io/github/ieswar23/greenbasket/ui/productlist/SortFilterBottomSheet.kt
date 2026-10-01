package io.github.ieswar23.greenbasket.ui.productlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.BottomSheetSortFilterBinding
import io.github.ieswar23.greenbasket.domain.model.FilterOptions
import io.github.ieswar23.greenbasket.domain.model.ProductFilter
import io.github.ieswar23.greenbasket.domain.model.SortOption
import io.github.ieswar23.greenbasket.util.asRupees
import kotlin.math.roundToInt

/**
 * Sort + filter sheet. Edits a local draft and only pushes it to the parent's ViewModel on "Apply".
 */
@AndroidEntryPoint
class SortFilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSortFilterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductListViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetSortFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val state = viewModel.uiState.value
        val options = viewModel.filterOptions.value

        buildSortChips(state.sort)
        buildPriceSlider(options, state.filter)
        buildDiscountChips(state.filter.minDiscount)
        buildBrandChips(options, state.filter.brands)

        binding.clearButton.setOnClickListener {
            viewModel.applySortAndFilter(SortOption.RELEVANCE, ProductFilter.NONE)
            dismiss()
        }
        binding.applyButton.setOnClickListener {
            viewModel.applySortAndFilter(selectedSort(), draftFilter(options))
            dismiss()
        }
    }

    private fun buildSortChips(selected: SortOption) {
        SortOption.entries.forEach { option ->
            binding.sortGroup.addChoiceChip(getString(option.labelRes), option.ordinal, option == selected)
        }
    }

    private fun buildPriceSlider(options: FilterOptions, filter: ProductFilter) {
        val hasRange = options.maxPrice > options.minPrice
        binding.priceSection.isVisible = hasRange
        if (!hasRange) return
        val from = options.minPrice.toFloat()
        val to = options.maxPrice.toFloat()
        binding.priceSlider.valueFrom = from
        binding.priceSlider.valueTo = to
        binding.priceSlider.values = listOf(
            (filter.minPrice?.toFloat() ?: from).coerceIn(from, to),
            (filter.maxPrice?.toFloat() ?: to).coerceIn(from, to),
        )
        binding.priceSlider.setLabelFormatter { value -> value.roundToInt().asRupees() }
        binding.priceSlider.addOnChangeListener { slider, _, _ -> updatePriceLabel(slider.values) }
        updatePriceLabel(binding.priceSlider.values)
    }

    private fun updatePriceLabel(values: List<Float>) {
        binding.priceRangeLabel.text = getString(
            R.string.filter_price_range,
            values[0].roundToInt().asRupees(),
            values[1].roundToInt().asRupees(),
        )
    }

    private fun buildDiscountChips(selected: Int) {
        DISCOUNT_STEPS.forEach { step ->
            val label = if (step == 0) getString(R.string.filter_discount_any) else getString(R.string.filter_discount_min, step)
            binding.discountGroup.addChoiceChip(label, step, step == selected)
        }
    }

    private fun buildBrandChips(options: FilterOptions, selected: Set<String>) {
        options.brands.forEachIndexed { index, brand ->
            val chip = layoutInflater.inflate(R.layout.item_filter_chip, binding.brandGroup, false) as Chip
            chip.id = View.generateViewId()
            chip.tag = brand
            chip.text = brand
            chip.isCheckable = true
            chip.isChecked = brand in selected
            binding.brandGroup.addView(chip, index)
        }
    }

    private fun ChipGroup.addChoiceChip(label: String, value: Int, checked: Boolean) {
        val chip = layoutInflater.inflate(R.layout.item_choice_chip, this, false) as Chip
        chip.id = View.generateViewId()
        chip.tag = value
        chip.text = label
        chip.isCheckable = true
        addView(chip)
        chip.isChecked = checked
    }

    private fun selectedSort(): SortOption {
        val chip = binding.sortGroup.findViewById<Chip>(binding.sortGroup.checkedChipId)
        return SortOption.entries.getOrNull(chip?.tag as? Int ?: 0) ?: SortOption.RELEVANCE
    }

    private fun draftFilter(options: FilterOptions): ProductFilter {
        val discountChip = binding.discountGroup.findViewById<Chip>(binding.discountGroup.checkedChipId)
        val brands = binding.brandGroup.checkedChipIds
            .mapNotNull { id -> binding.brandGroup.findViewById<Chip>(id)?.tag as? String }
            .toSet()
        var minPrice: Int? = null
        var maxPrice: Int? = null
        if (binding.priceSection.isVisible) {
            val values = binding.priceSlider.values
            val low = values[0].roundToInt()
            val high = values[1].roundToInt()
            if (low > options.minPrice) minPrice = low
            if (high < options.maxPrice) maxPrice = high
        }
        return ProductFilter(
            minPrice = minPrice,
            maxPrice = maxPrice,
            minDiscount = discountChip?.tag as? Int ?: 0,
            brands = brands,
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "SortFilterBottomSheet"
    }
}
