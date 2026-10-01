package io.github.ieswar23.greenbasket.screenshots

import android.os.Looper
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.ieswar23.greenbasket.MainActivity
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.data.repository.AddressRepository
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.data.repository.CatalogRepository
import io.github.ieswar23.greenbasket.data.repository.OrderRepository
import io.github.ieswar23.greenbasket.ui.common.CategoryArgs
import io.github.ieswar23.greenbasket.ui.common.OrderArgs
import io.github.ieswar23.greenbasket.ui.common.ProductArgs
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.Duration
import javax.inject.Inject

/**
 * Renders the real app (MainActivity + Navigation + Hilt + Room + the mock Retrofit API) on the JVM with
 * Robolectric native graphics and records README screenshots with Roborazzi.
 *
 * Images are only written by `./gradlew recordRoborazziDebug`; a plain `testDebugUnitTest` run still
 * executes every screen (so crashes are caught) but does not touch `docs/screenshots/`.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(application = HiltTestApplication::class, qualifiers = PHONE, sdk = [34])
class ScreenshotTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var catalogRepository: CatalogRepository
    @Inject lateinit var orderRepository: OrderRepository
    @Inject lateinit var addressRepository: AddressRepository
    @Inject lateinit var cartRepository: CartRepository

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun seedData() {
        hiltRule.inject()
        // Same first-launch work MainViewModel does, done up front so every screen opens with content.
        runBlocking {
            addressRepository.seedIfEmpty()
            catalogRepository.syncIfNeeded()
            orderRepository.importHistoryIfEmpty()
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun home() {
        launch()
        capture("01_home")
    }

    @Test
    fun productListDairy() {
        fillCart(mapOf("dy_fullcream_1l" to 2, "dy_paneer" to 1))
        launch()
        navigate(R.id.productListFragment, CategoryArgs.bundle("dairy_eggs"))
        capture("02_products_dairy")
    }

    @Test
    fun productDetail() {
        launch()
        navigate(R.id.productListFragment, CategoryArgs.bundle("dairy_eggs"))
        navigate(R.id.productDetailFragment, ProductArgs.bundle("dy_curd_400"))
        capture("03_product_detail")
    }

    @Test
    fun cartWithItems() {
        fillCart(
            mapOf(
                "fv_banana" to 2,
                "dy_fullcream_1l" to 2,
                "dy_paneer" to 1,
                "bk_croissant" to 2,
                "sn_dark_choc" to 1,
            ),
        )
        launch()
        selectTab(R.id.cartFragment)
        capture("04_cart")
    }

    @Test
    fun orders() {
        launch()
        selectTab(R.id.ordersFragment)
        capture("05_orders")
    }

    @Test
    fun orderDetail() {
        launch()
        selectTab(R.id.ordersFragment)
        navigate(R.id.orderDetailFragment, OrderArgs.bundle("GB24091873"))
        capture("06_order_detail")
    }

    @Test
    @Config(qualifiers = "+night")
    fun homeDark() {
        launch()
        capture("07_home_dark")
    }

    // region helpers

    private fun fillCart(lines: Map<String, Int>) = runBlocking {
        lines.forEach { (productId, quantity) -> cartRepository.setQuantity(productId, quantity) }
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        settle()
    }

    private fun navController(): NavController {
        lateinit var controller: NavController
        scenario!!.onActivity { activity ->
            val host = activity.supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
            controller = host.navController
        }
        return controller
    }

    private fun navigate(@IdRes destination: Int, args: android.os.Bundle) {
        val controller = navController()
        scenario!!.onActivity { controller.navigate(destination, args) }
        settle()
    }

    private fun selectTab(@IdRes tabId: Int) {
        scenario!!.onActivity { activity ->
            activity.findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = tabId
        }
        settle()
    }

    /**
     * Lets Room/IO work finish on real background threads while draining the (paused) main looper, then
     * advances the fake clock so fragment transitions and item animations run to completion.
     */
    private fun settle() {
        val mainLooper = shadowOf(Looper.getMainLooper())
        repeat(SETTLE_ROUNDS) {
            mainLooper.idle()
            Thread.sleep(SETTLE_SLEEP_MS)
            mainLooper.idleFor(Duration.ofMillis(ANIMATION_STEP_MS))
        }
        mainLooper.idle()
    }

    private fun capture(name: String) {
        scenario!!.onActivity { activity ->
            activity.window.decorView.captureRoboImage(
                filePath = "$SCREENSHOT_DIR/$name.png",
                roborazziOptions = OPTIONS,
            )
        }
    }

    // endregion
}

private const val PHONE = "w411dp-h891dp-xxhdpi"
private const val SCREENSHOT_DIR = "../docs/screenshots"
private const val SETTLE_ROUNDS = 12
private const val SETTLE_SLEEP_MS = 60L
private const val ANIMATION_STEP_MS = 150L

private val OPTIONS = RoborazziOptions(
    recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.45),
)
