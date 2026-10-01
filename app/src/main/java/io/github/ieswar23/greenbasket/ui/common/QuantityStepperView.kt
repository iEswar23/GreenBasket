package io.github.ieswar23.greenbasket.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.data.repository.CartRepository
import io.github.ieswar23.greenbasket.databinding.ViewQuantityStepperBinding

/**
 * The classic quick-commerce "ADD" button that morphs into a − n + stepper once the item is in the cart.
 */
class QuantityStepperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewQuantityStepperBinding.inflate(LayoutInflater.from(context), this)

    var onIncrement: (() -> Unit)? = null
    var onDecrement: (() -> Unit)? = null

    var maxQuantity: Int = CartRepository.MAX_QUANTITY_PER_ITEM

    var quantity: Int = 0
        private set

    init {
        var large = false
        context.withStyledAttributes(attrs, R.styleable.QuantityStepperView) {
            large = getInt(R.styleable.QuantityStepperView_stepperSize, 0) == 1
        }
        val height = resources.getDimensionPixelSize(if (large) R.dimen.stepper_height_large else R.dimen.stepper_height)
        val width = resources.getDimensionPixelSize(if (large) R.dimen.stepper_width_large else R.dimen.stepper_width)
        listOf(binding.addButton, binding.stepper).forEach { view ->
            view.layoutParams = LayoutParams(width, height)
        }
        if (large) {
            binding.addButton.textSize = 16f
            binding.count.textSize = 17f
        }
        binding.addButton.setOnClickListener { onIncrement?.invoke() }
        binding.plus.setOnClickListener { if (quantity < maxQuantity) onIncrement?.invoke() }
        binding.minus.setOnClickListener { onDecrement?.invoke() }
        render(animate = false)
    }

    fun setQuantity(value: Int, animate: Boolean = true) {
        if (value == quantity && binding.count.text.isNotEmpty()) return
        val previous = quantity
        quantity = value.coerceAtLeast(0)
        render(animate = animate && isAttachedToWindow, increased = quantity > previous)
    }

    private fun render(animate: Boolean, increased: Boolean = true) {
        val inCart = quantity > 0
        binding.count.text = quantity.toString()
        binding.count.contentDescription = context.getString(R.string.cd_quantity, quantity)
        binding.plus.alpha = if (quantity >= maxQuantity) 0.4f else 1f

        if (!animate) {
            binding.addButton.animate().cancel()
            binding.stepper.animate().cancel()
            binding.count.animate().cancel()
            binding.count.translationY = 0f
            binding.count.alpha = 1f
            binding.addButton.isVisible = !inCart
            binding.stepper.isVisible = inCart
            binding.addButton.alpha = 1f
            binding.stepper.alpha = 1f
            return
        }
        if (inCart && !binding.stepper.isVisible) {
            crossFade(show = binding.stepper, hide = binding.addButton)
        } else if (!inCart && binding.stepper.isVisible) {
            crossFade(show = binding.addButton, hide = binding.stepper)
        } else if (inCart) {
            val offset = if (increased) 12f else -12f
            binding.count.translationY = offset
            binding.count.alpha = 0f
            binding.count.animate().translationY(0f).alpha(1f).setDuration(160).start()
        }
    }

    private fun crossFade(show: View, hide: View) {
        show.alpha = 0f
        show.scaleX = 0.92f
        show.isVisible = true
        show.animate().alpha(1f).scaleX(1f).setDuration(180).start()
        hide.animate().alpha(0f).setDuration(120).withEndAction {
            hide.isVisible = false
            hide.alpha = 1f
        }.start()
    }
}
