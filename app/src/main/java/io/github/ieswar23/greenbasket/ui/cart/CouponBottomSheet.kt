package io.github.ieswar23.greenbasket.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.BottomSheetCouponsBinding
import io.github.ieswar23.greenbasket.databinding.ItemCouponBinding
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle
import io.github.ieswar23.greenbasket.util.asRupees

/** Lists running offers and accepts a manually typed code. Results are reported by the cart screen. */
@AndroidEntryPoint
class CouponBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCouponsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CartViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetCouponsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyCodeButton.setOnClickListener { applyTypedCode() }
        binding.codeInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                applyTypedCode()
                true
            } else {
                false
            }
        }
        collectWithLifecycle(viewModel.uiState) { state -> renderCoupons(state.bill.itemTotal, state.bill.appliedCoupon?.code) }
    }

    private fun applyTypedCode() {
        val code = binding.codeInput.text?.toString().orEmpty().trim()
        if (code.isEmpty()) {
            binding.codeInputLayout.error = getString(R.string.coupon_error_empty)
            return
        }
        binding.codeInputLayout.error = null
        viewModel.applyCoupon(code)
        dismiss()
    }

    private fun renderCoupons(itemTotal: Int, appliedCode: String?) {
        binding.couponList.removeAllViews()
        viewModel.coupons.forEach { coupon ->
            val row = ItemCouponBinding.inflate(layoutInflater, binding.couponList, true)
            row.couponCode.text = coupon.code
            row.couponTitle.text = coupon.title
            row.couponDescription.text = coupon.description
            val shortfall = coupon.minOrder - itemTotal
            row.couponHint.isVisible = shortfall > 0
            row.couponHint.text = getString(R.string.coupon_unlock_hint, shortfall.asRupees())
            val isApplied = coupon.code == appliedCode
            row.couponApply.isEnabled = shortfall <= 0 && !isApplied
            row.couponApply.setText(if (isApplied) R.string.coupon_applied else R.string.action_apply)
            row.couponApply.setOnClickListener {
                viewModel.applyCoupon(coupon.code)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "CouponBottomSheet"
    }
}
