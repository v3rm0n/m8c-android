package io.maido.m8client

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.view.allViews
import kotlin.math.roundToInt

internal class M8ButtonLayout(
    context: Context,
    private val alternateLayout: Boolean,
    private val buttonScale: Int,
) : FrameLayout(context) {

    private var buttons = emptyList<ImageButton>()

    fun setButtons(overlay: View) {
        buttons = overlay.allViews.filterIsInstance<ImageButton>().toList()
        addView(overlay)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (buttonScale != 100 && buttons.isNotEmpty()) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = MeasureSpec.getSize(heightMeasureSpec)
            val size = buttonSize(width, height)
            for (button in buttons) {
                val buttonWidth = size + button.paddingLeft + button.paddingRight
                val buttonHeight = size + button.paddingTop + button.paddingBottom
                val params = button.layoutParams
                if (params.width != buttonWidth || params.height != buttonHeight) {
                    params.width = buttonWidth
                    params.height = buttonHeight
                    button.scaleType = ImageView.ScaleType.FIT_CENTER
                    var child: View? = button
                    while (child != null && child !== this) {
                        child.forceLayout()
                        child = child.parent as? View
                    }
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun buttonSize(width: Int, height: Int): Int {
        val density = resources.displayMetrics.density
        val gap10 = (10 * density).roundToInt()
        val gap20 = (20 * density).roundToInt()
        val gap50 = (50 * density).roundToInt()
        val requested = (buttons.first().drawable.intrinsicWidth * buttonScale / 100f).roundToInt()

        // The layouts use fixed gaps around the squares. Fit the squares to the
        // window while retaining those gaps, including after rotation or resizing.
        val widthLimit: Int
        val heightLimit: Int
        if (alternateLayout) {
            widthLimit = (width - 2 * gap10 - 4 * gap20) / 3
            heightLimit = (height - (100 * density).roundToInt() - 5 * gap20) / 4
        } else {
            widthLimit = (width - 2 * gap10 - 2 * gap20) / 5
            heightLimit = (height - 3 * gap50) / 2
        }
        return minOf(requested, widthLimit, heightLimit).coerceAtLeast(1)
    }
}
