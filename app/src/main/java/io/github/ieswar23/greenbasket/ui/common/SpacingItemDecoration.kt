package io.github.ieswar23.greenbasket.ui.common

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Even gutters for grids and spacing between items of horizontal / vertical lists. */
class SpacingItemDecoration(
    private val spacing: Int,
    private val includeEdge: Boolean = true,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        when (val lm = parent.layoutManager) {
            is GridLayoutManager -> {
                val spanCount = lm.spanCount
                val column = position % spanCount
                if (includeEdge) {
                    outRect.left = spacing - column * spacing / spanCount
                    outRect.right = (column + 1) * spacing / spanCount
                    if (position < spanCount) outRect.top = spacing
                    outRect.bottom = spacing
                } else {
                    outRect.left = column * spacing / spanCount
                    outRect.right = spacing - (column + 1) * spacing / spanCount
                    if (position >= spanCount) outRect.top = spacing
                }
            }
            is LinearLayoutManager -> {
                val count = parent.adapter?.itemCount ?: 0
                if (lm.orientation == RecyclerView.HORIZONTAL) {
                    outRect.left = if (position == 0 && includeEdge) spacing else spacing / 2
                    outRect.right = if (position == count - 1 && includeEdge) spacing else spacing / 2
                } else {
                    outRect.top = if (position == 0 && includeEdge) spacing else spacing / 2
                    outRect.bottom = if (position == count - 1 && includeEdge) spacing else spacing / 2
                }
            }
        }
    }
}
