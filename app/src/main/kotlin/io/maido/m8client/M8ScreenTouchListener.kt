package io.maido.m8client

import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class M8ScreenTouchListener(
    private val channel: Int,
    private val ccX: Int,
    private val ccY: Int,
    private val sendMidiCC: (channel: Int, cc: Int, value: Int) -> Unit
) : View.OnTouchListener {

    private var lastCcX = -1
    private var lastCcY = -1
    private var downX = 0f
    private var downY = 0f
    private var lastDragX = 0f
    private var lastDragY = 0f
    private var dragAccumulator = 0f
    // null = undecided, true = vertical, false = horizontal
    private var axisLocked: Boolean? = null

    companion object {
        private const val AXIS_LOCK_THRESHOLD_PX = 10f
        private const val PIXELS_PER_TAP = 1f
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                lastDragX = event.x
                lastDragY = event.y
                dragAccumulator = 0f
                axisLocked = null
            }
            MotionEvent.ACTION_MOVE -> {
                when {
                    M8TouchListener.isEditHeld() -> {
                        // Axis-lock first, then fire LEFT/RIGHT (horizontal) or UP/DOWN (vertical)
                        val dx = abs(event.x - downX)
                        val dy = abs(event.y - downY)
                        if (axisLocked == null && (dx > AXIS_LOCK_THRESHOLD_PX || dy > AXIS_LOCK_THRESHOLD_PX)) {
                            axisLocked = dy >= dx
                            lastDragX = event.x
                            lastDragY = event.y
                            dragAccumulator = 0f
                        }
                        if (axisLocked == false) {
                            dragAccumulator += event.x - lastDragX
                            lastDragX = event.x
                            val taps = (dragAccumulator / PIXELS_PER_TAP).toInt()
                            if (taps != 0) {
                                dragAccumulator -= taps * PIXELS_PER_TAP
                                if (taps > 0) repeat(taps) { M8TouchListener.tapWithCurrentState(M8Key.RIGHT) }
                                else repeat(-taps) { M8TouchListener.tapWithCurrentState(M8Key.LEFT) }
                            }
                        } else if (axisLocked == true) {
                            dragAccumulator += event.y - lastDragY
                            lastDragY = event.y
                            val taps = (dragAccumulator / PIXELS_PER_TAP).toInt()
                            if (taps != 0) {
                                dragAccumulator -= taps * PIXELS_PER_TAP
                                if (taps > 0) repeat(taps) { M8TouchListener.tapWithCurrentState(M8Key.DOWN) }
                                else repeat(-taps) { M8TouchListener.tapWithCurrentState(M8Key.UP) }
                            }
                        }
                    }
                    M8TouchListener.isOptionHeld() -> {
                        // MIDI learn mode: lock to dominant axis so M8 assigns the right CC
                        val newX = ((1f - event.y.coerceIn(0f, view.height.toFloat()) / view.height) * 127).toInt()
                        val newY = (event.x.coerceIn(0f, view.width.toFloat()) / view.width * 127).toInt()
                        val dx = abs(event.x - downX)
                        val dy = abs(event.y - downY)
                        if (axisLocked == null && (dx > AXIS_LOCK_THRESHOLD_PX || dy > AXIS_LOCK_THRESHOLD_PX)) {
                            axisLocked = dy >= dx
                        }
                        if (axisLocked == true  && newX != lastCcX) { lastCcX = newX; sendMidiCC(channel, ccX, newX) }
                        if (axisLocked == false && newY != lastCcY) { lastCcY = newY; sendMidiCC(channel, ccY, newY) }
                    }
                    else -> {
                        // Normal use: send both CCs simultaneously
                        val newX = ((1f - event.y.coerceIn(0f, view.height.toFloat()) / view.height) * 127).toInt()
                        val newY = (event.x.coerceIn(0f, view.width.toFloat()) / view.width * 127).toInt()
                        if (newX != lastCcX) { lastCcX = newX; sendMidiCC(channel, ccX, newX) }
                        if (newY != lastCcY) { lastCcY = newY; sendMidiCC(channel, ccY, newY) }
                    }
                }
            }
        }
        return false
    }
}
