package io.github.ieswar23.greenbasket.ui.common

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import io.github.ieswar23.greenbasket.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Collects [flow] only while the fragment's view is at least STARTED. */
fun <T> Fragment.collectWithLifecycle(
    flow: Flow<T>,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit,
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(state) {
            flow.collect { action(it) }
        }
    }
}

/** Fade-through for top-level destinations reached from the bottom navigation. */
fun Fragment.useTabTransitions() {
    enterTransition = MaterialFadeThrough()
    exitTransition = MaterialFadeThrough()
    reenterTransition = MaterialFadeThrough()
}

/** Shared X-axis transition for drill-down destinations. */
fun Fragment.useForwardTransitions() {
    enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
    returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
    reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
}

/** Navigates deeper into the hierarchy using a shared-axis transition from the current screen. */
fun Fragment.navigateForward(@IdRes destination: Int, args: Bundle? = null) {
    exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
    reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    findNavController().navigate(destination, args)
}

/** Switches to a bottom-navigation tab the same way NavigationUI does (saving/restoring back stacks). */
fun NavController.navigateToTab(@IdRes tabId: Int) {
    navigate(
        tabId,
        null,
        navOptions {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        },
    )
}

fun Fragment.showMessage(event: UiEvent.Message, anchor: android.view.View? = null) {
    val view = view ?: return
    val text = getString(event.textRes, *event.args.toTypedArray())
    Snackbar.make(view, text, Snackbar.LENGTH_SHORT).apply {
        anchor?.let { anchorView = it }
    }.show()
}

/** Default product-card actions: open details on click, cart/wishlist changes go through [viewModel]. */
fun Fragment.productActions(viewModel: ProductActionsViewModel): ProductActions = object : ProductActions {
    override fun onProductClick(product: io.github.ieswar23.greenbasket.domain.model.Product) {
        navigateForward(R.id.productDetailFragment, ProductArgs.bundle(product.id))
    }

    override fun onIncrement(product: io.github.ieswar23.greenbasket.domain.model.Product) =
        viewModel.increment(product)

    override fun onDecrement(product: io.github.ieswar23.greenbasket.domain.model.Product) =
        viewModel.decrement(product)

    override fun onToggleWishlist(product: io.github.ieswar23.greenbasket.domain.model.Product) =
        viewModel.toggleWishlist(product)
}

/** Tab screens reset their exit transition after returning from a drill-down screen. */
fun Fragment.restoreTabExitTransition() {
    exitTransition = MaterialFadeThrough()
}

/** true = dark status-bar icons (for light backgrounds). */
fun Fragment.setStatusBarAppearance(lightBackground: Boolean) {
    val window = activity?.window ?: return
    androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        .isAppearanceLightStatusBars = lightBackground
}

/** Restores status-bar icon colour to match the current theme. */
fun Fragment.resetStatusBarAppearance() {
    setStatusBarAppearance(lightBackground = !requireContext().isNightMode())
}
