package io.github.ieswar23.greenbasket.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.BottomSheetAddressBinding
import io.github.ieswar23.greenbasket.domain.AddressField
import io.github.ieswar23.greenbasket.domain.AddressInput
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle

/** Pick the delivery address or add a new one. Used from Home, Checkout and Profile. */
@AndroidEntryPoint
class AddressBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddressBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddressViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddressBinding.inflate(inflater, container, false)
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
        val startWithForm = arguments?.getBoolean(ARG_START_WITH_FORM) == true
        setFormVisible(startWithForm)

        binding.showFormButton.setOnClickListener { setFormVisible(true) }
        binding.saveAddressButton.setOnClickListener { viewModel.save(readInput()) }

        collectWithLifecycle(viewModel.state) { state ->
            AddressRowBinder.bind(binding.addressList, state.addresses, state.selectedId) { address ->
                viewModel.select(address)
                dismiss()
            }
        }
        collectWithLifecycle(viewModel.formErrors, action = ::renderErrors)
        collectWithLifecycle(viewModel.saved) { dismiss() }
    }

    private fun setFormVisible(visible: Boolean) {
        binding.addressForm.isVisible = visible
        binding.showFormButton.isVisible = !visible
        binding.addressList.isVisible = !visible
        binding.sheetTitle.setText(if (visible) R.string.address_form_title else R.string.address_sheet_title)
        if (visible) binding.nameInput.requestFocus()
    }

    private fun readInput(): AddressInput {
        val label = when (binding.labelGroup.checkedChipId) {
            R.id.labelWork -> getString(R.string.address_label_work)
            R.id.labelOther -> getString(R.string.address_label_other)
            else -> getString(R.string.address_label_home)
        }
        return AddressInput(
            label = label,
            receiverName = binding.nameInput.text?.toString().orEmpty(),
            phone = binding.phoneInput.text?.toString().orEmpty(),
            houseDetails = binding.houseInput.text?.toString().orEmpty(),
            area = binding.areaInput.text?.toString().orEmpty(),
            city = binding.cityInput.text?.toString().orEmpty(),
            pincode = binding.pincodeInput.text?.toString().orEmpty(),
        )
    }

    private fun renderErrors(errors: Set<AddressField>) {
        binding.nameLayout.error = if (AddressField.NAME in errors) getString(R.string.address_error_name) else null
        binding.phoneLayout.error = if (AddressField.PHONE in errors) getString(R.string.address_error_phone) else null
        binding.houseLayout.error = if (AddressField.HOUSE in errors) getString(R.string.address_error_required) else null
        binding.areaLayout.error = if (AddressField.AREA in errors) getString(R.string.address_error_required) else null
        binding.cityLayout.error = if (AddressField.CITY in errors) getString(R.string.address_error_required) else null
        binding.pincodeLayout.error = if (AddressField.PINCODE in errors) getString(R.string.address_error_pincode) else null
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "AddressBottomSheet"
        private const val ARG_START_WITH_FORM = "startWithForm"

        fun newInstance(startWithForm: Boolean) = AddressBottomSheet().apply {
            arguments = bundleOf(ARG_START_WITH_FORM to startWithForm)
        }
    }
}
