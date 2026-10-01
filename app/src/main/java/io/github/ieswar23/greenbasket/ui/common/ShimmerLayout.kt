package io.github.ieswar23.greenbasket.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import com.google.android.material.R as MaterialR

/**
 * Lightweight skeleton shimmer: draws its children (grey placeholder blocks) and sweeps a soft highlight
 * gradient across them using SRC_ATOP so only the placeholders light up.
 */
class ShimmerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }
    private val shaderMatrix = Matrix()
    private var progress = 0f
    private var shader: LinearGradient? = null

    // Nullable because View callbacks (e.g. onVisibilityChanged) can run before property initializers.
    private var animator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1300
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0) return
        val highlight = ColorUtils.setAlphaComponent(
            context.themeColor(MaterialR.attr.colorSurfaceContainerLowest),
            if (context.isNightMode()) 70 else 190,
        )
        val bandWidth = w * 0.45f
        shader = LinearGradient(
            0f, 0f, bandWidth, 0f,
            intArrayOf(0x00FFFFFF and highlight, highlight, 0x00FFFFFF and highlight),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        shimmerPaint.shader = shader
    }

    override fun dispatchDraw(canvas: Canvas) {
        val save = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        super.dispatchDraw(canvas)
        if (shader != null && animator?.isRunning == true) {
            val bandWidth = width * 0.45f
            val dx = -bandWidth + (width + bandWidth * 2) * progress
            shaderMatrix.setTranslate(dx, 0f)
            shaderMatrix.postSkew(-0.3f, 0f)
            shader?.setLocalMatrix(shaderMatrix)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shimmerPaint)
        }
        canvas.restoreToCount(save)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimation()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateAnimation()
    }

    private fun updateAnimation() {
        val anim = animator ?: return
        if (isAttachedToWindow && isShown) {
            if (!anim.isStarted) anim.start()
        } else {
            anim.cancel()
        }
    }
}
