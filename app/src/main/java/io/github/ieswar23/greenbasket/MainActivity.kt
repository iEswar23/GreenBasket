package io.github.ieswar23.greenbasket

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.greenbasket.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: MainViewModel by viewModels()

    /** Destinations that take the full screen (they have their own bottom action bars). */
    private val fullScreenDestinations = setOf(
        R.id.productDetailFragment,
        R.id.searchFragment,
        R.id.checkoutFragment,
        R.id.orderPlacedFragment,
        R.id.orderDetailFragment,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !viewModel.isReady.value }
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)
        binding.bottomNav.setOnItemReselectedListener { item ->
            // Re-selecting a tab pops back to its root, like most shopping apps.
            navController.popBackStack(item.itemId, inclusive = false)
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.isVisible = destination.id !in fullScreenDestinations
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cartCount.collect(::renderCartBadge)
            }
        }
    }

    private fun renderCartBadge(count: Int) {
        val badge = binding.bottomNav.getOrCreateBadge(R.id.cartFragment)
        badge.isVisible = count > 0
        badge.number = count
        badge.backgroundColor = getColor(R.color.accent_yellow)
        badge.badgeTextColor = getColor(R.color.on_accent_yellow)
    }
}
