package io.github.ieswar23.greenbasket.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.BuildConfig
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.databinding.FragmentProfileBinding
import io.github.ieswar23.greenbasket.domain.model.ThemeMode
import io.github.ieswar23.greenbasket.ui.common.ThemeApplier
import io.github.ieswar23.greenbasket.ui.common.applyStatusBarPadding
import io.github.ieswar23.greenbasket.ui.common.collectWithLifecycle
import io.github.ieswar23.greenbasket.ui.common.navigateForward
import io.github.ieswar23.greenbasket.ui.common.navigateToTab
import androidx.navigation.fragment.findNavController
import io.github.ieswar23.greenbasket.ui.common.restoreTabExitTransition
import io.github.ieswar23.greenbasket.ui.common.useTabTransitions
import io.github.ieswar23.greenbasket.util.asRupees

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        useTabTransitions()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)
        restoreTabExitTransition()
        binding.appBar.applyStatusBarPadding()

        binding.avatarInitials.text = initials(getString(R.string.profile_name))
        binding.versionFooter.text = getString(R.string.profile_version, BuildConfig.VERSION_NAME)
        binding.rowWishlist.setOnClickListener { navigateForward(R.id.wishlistFragment) }
        binding.statWishlist.setOnClickListener { navigateForward(R.id.wishlistFragment) }
        binding.statOrders.setOnClickListener { findNavController().navigateToTab(R.id.ordersFragment) }
        binding.rowTheme.setOnClickListener { showThemeDialog(viewModel.uiState.value.themeMode) }
        binding.rowAbout.setOnClickListener { showAboutDialog() }
        binding.addAddressButton.setOnClickListener {
            AddressBottomSheet.newInstance(startWithForm = true).show(childFragmentManager, AddressBottomSheet.TAG)
        }

        collectWithLifecycle(viewModel.uiState) { state ->
            binding.statOrdersValue.text = state.orderCount.toString()
            binding.statSavingsValue.text = state.totalSavings.asRupees()
            binding.statWishlistValue.text = state.wishlistCount.toString()
            binding.themeValue.setText(themeLabel(state.themeMode))
            AddressRowBinder.bind(binding.addressContainer, state.addresses, state.selectedAddressId, viewModel::selectAddress)
        }
    }

    private fun showThemeDialog(current: ThemeMode) {
        val modes = ThemeMode.entries
        val labels = modes.map { getString(themeLabel(it)) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_appearance)
            .setSingleChoiceItems(labels, modes.indexOf(current)) { dialog, which ->
                val mode = modes[which]
                viewModel.setThemeMode(mode)
                dialog.dismiss()
                ThemeApplier.apply(mode)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.ic_eco)
            .setTitle(R.string.app_name)
            .setMessage(getString(R.string.about_message, BuildConfig.VERSION_NAME))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun themeLabel(mode: ThemeMode): Int = when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

    private fun initials(name: String): String =
        name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
