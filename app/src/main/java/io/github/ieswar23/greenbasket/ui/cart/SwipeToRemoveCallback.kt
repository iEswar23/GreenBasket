package io.github.ieswar23.greenbasket.ui.cart

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import io.github.ieswar23.greenbasket.R
import io.github.ieswar23.greenbasket.ui.common.dp

/** Swipe-left to remove a cart line, drawing a red "delete" affordance behind the card. */
class SwipeToRemoveCallback(
    private val onSwiped: (position: Int) -> Unit,
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
        if (viewHolder.itemViewType == CartAdapter.TYPE_ITEM) super.getSwipeDirs(recyclerView, viewHolder) else 0

    override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onSwiped(viewHolder.bindingAdapterPosition)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean,
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
            val item = viewHolder.itemView
            val context = recyclerView.context
            paint.color = ContextCompat.getColor(context, R.color.swipe_delete_bg)
            rect.set(item.right + dX, item.top.toFloat() + item.paddingTop, item.right.toFloat(), item.bottom.toFloat())
            val radius = 16.dp.toFloat()
            c.drawRoundRect(rect, radius, radius, paint)

            ContextCompat.getDrawable(context, R.drawable.ic_delete)?.let { icon ->
                icon.setTint(android.graphics.Color.WHITE)
                val size = 24.dp
                val marginTop = item.top + (item.height - size) / 2
                val right = item.right - 24.dp
                if (rect.width() > size + 32.dp) {
                    icon.setBounds(right - size, marginTop, right, marginTop + size)
                    icon.draw(c)
                }
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
