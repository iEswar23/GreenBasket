package io.github.ieswar23.greenbasket.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.BottomSheetDeliverySlotBinding
import io.github.ieswar23.greenbasket.domain.model.DeliveryDay
import io.github.ieswar23.greenbasket.domain.model.DeliverySlot
import io.github.ieswar23.greenbasket.ui.common.SlotLabels
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle

/** Lets the customer choose express delivery or a one-hour window today / tomorrow. */
@AndroidEntryPoint
class DeliverySlotBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDeliverySlotBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DeliverySlotViewModel by viewModels()

    private var slots: List<DeliverySlot> = emptyList()
    private var draft: DeliverySlot? = null
    private var day: DeliveryDay = DeliveryDay.TODAY

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetDeliverySlotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.expressCard.setOnClickListener {
            slots.firstOrNull { it.isExpress }?.let { express ->
                draft = express
                renderSelection()
            }
        }
        binding.dayToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            day = if (checkedId == R.id.tomorrowButton) DeliveryDay.TOMORROW else DeliveryDay.TODAY
            renderSlotChips()
        }
        binding.confirmSlotButton.setOnClickListener {
            draft?.let(viewModel::select)
            dismiss()
        }

        collectWithLifecycle(viewModel.state) { state ->
            state ?: return@collectWithLifecycle
            val firstLoad = slots.isEmpty()
            slots = state.slots
            if (firstLoad) {
                draft = state.selected
                day = state.selected.day
                binding.dayToggle.check(if (day == DeliveryDay.TOMORROW) R.id.tomorrowButton else R.id.todayButton)
            }
            val expressAvailable = slots.any { it.isExpress }
            binding.expressCard.isEnabled = expressAvailable
            binding.expressCard.alpha = if (expressAvailable) 1f else 0.5f
            binding.expressSubtitle.setText(
                if (expressAvailable) R.string.slot_express_subtitle else R.string.slot_express_unavailable,
            )
            renderSlotChips()
        }
    }

    private fun renderSlotChips() {
        val daySlots = slots.filter { !it.isExpress && it.day == day }
        binding.noSlotsText.isVisible = daySlots.isEmpty()
        binding.slotChips.removeAllViews()
        daySlots.forEach { slot ->
            val chip = layoutInflater.inflate(R.layout.item_choice_chip, binding.slotChips, false) as Chip
            chip.id = View.generateViewId()
            chip.text = SlotLabels.window(slot)
            chip.isCheckable = true
            chip.isChecked = draft?.id == slot.id
            chip.setOnClickListener {
                draft = slot
                renderSelection()
            }
            binding.slotChips.addView(chip)
        }
        renderSelection()
    }

    private fun renderSelection() {
        val selected = draft
        binding.expressCard.isChecked = selected?.isExpress == true
        for (i in 0 until binding.slotChips.childCount) {
            val chip = binding.slotChips.getChildAt(i) as Chip
            val slot = slots.filter { !it.isExpress && it.day == day }.getOrNull(i)
            chip.isChecked = slot != null && slot.id == selected?.id
        }
        binding.confirmSlotButton.text = selected?.let {
            getString(R.string.slot_confirm_with_label, SlotLabels.full(requireContext(), it))
        } ?: getString(R.string.slot_confirm)
        binding.confirmSlotButton.isEnabled = selected != null
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "DeliverySlotBottomSheet"
    }
}
