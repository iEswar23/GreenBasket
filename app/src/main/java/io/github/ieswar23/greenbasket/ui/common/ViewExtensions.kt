package io.github.ieswar23.greenbasket.ui.common

import android.content.Context
import android.content.res.Configuration
import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.color.MaterialColors

val Int.dp: Int get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()

fun Context.isNightMode(): Boolean =
    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

@ColorInt
fun Context.themeColor(@AttrRes attr: Int): Int = MaterialColors.getColor(this, attr, javaClass.simpleName)

/**
 * Product/category tints are authored as pastel colours. In dark theme they are blended into the
 * surface so the illustration cards stay subtle instead of glowing.
 */
@ColorInt
fun Context.illustrationTint(@ColorInt tint: Int): Int {
    if (!isNightMode()) return tint
    val surface = themeColor(com.google.android.material.R.attr.colorSurfaceContainerHigh)
    return ColorUtils.blendARGB(tint, surface, 0.82f)
}

fun TextView.setStrikeThrough(enabled: Boolean = true) {
    paintFlags = if (enabled) paintFlags or Paint.STRIKE_THRU_TEXT_FLAG else paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
}

/** Adds the status-bar inset on top of the view's original top padding. */
fun View.applyStatusBarPadding() {
    val initialTop = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        view.updatePadding(top = initialTop + top)
        insets
    }
    requestApplyInsetsWhenAttached()
}

/** Adds the navigation-bar inset below the view's original bottom padding. */
fun View.applyNavigationBarPadding() {
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime()).bottom
        view.updatePadding(bottom = initialBottom + bottom)
        insets
    }
    requestApplyInsetsWhenAttached()
}

/** Pads a full-screen view for both the status bar and the navigation bar. */
fun View.applySystemBarsPadding() {
    val initialTop = paddingTop
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(top = initialTop + bars.top, bottom = initialBottom + bars.bottom)
        insets
    }
    requestApplyInsetsWhenAttached()
}

/** Grows a fixed-height view (e.g. a toolbar) by the status-bar inset. */
fun View.applyStatusBarHeight() {
    val initialHeight = layoutParams?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT
    val initialTop = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        view.updatePadding(top = initialTop + top)
        if (initialHeight > 0) view.updateLayoutParams { height = initialHeight + top }
        insets
    }
    requestApplyInsetsWhenAttached()
}

private fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                ViewCompat.requestApplyInsets(v)
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

fun Context.dpToPx(value: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
