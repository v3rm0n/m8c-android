package io.maido.m8client

import android.os.Build
import android.util.Log
import android.view.HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
import android.view.HapticFeedbackConstants.VIRTUAL_KEY
import android.view.HapticFeedbackConstants.VIRTUAL_KEY_RELEASE
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.OnTouchListener

internal class M8TouchListener(
    private val key: M8Key,
) : OnTouchListener {

    companion object {
        private val TAG = M8TouchListener::class.simpleName
        private const val reset = 132.toChar()
        private const val exit = 96.toChar()
        private val directions = setOf(M8Key.UP, M8Key.DOWN, M8Key.LEFT, M8Key.RIGHT)
        private val directionMask = directions.fold(0) { mask, key -> mask or key.code }
        private val pressedButtons = linkedSetOf<M8TouchListener>()
        private var keyState = 0

        private external fun sendClickEvent(event: Char)

        external fun resetScreen()

        external fun exit()

        fun isOptionHeld(): Boolean = keyState and M8Key.OPTION.code != 0
        fun isEditHeld(): Boolean = keyState and M8Key.EDIT.code != 0

        fun tapWithCurrentState(key: M8Key) {
            sendClickEvent((keyState or key.code).toChar())
            sendClickEvent(keyState.toChar())
        }

        private fun currentButtons(): Int {
            val mask = pressedButtons.fold(0) { state, button -> state or button.key.code }
            if (mask and M8Key.SHIFT.code == 0) return mask

            // Keep Shift held while rolling from one direction to the next.
            val direction = pressedButtons.lastOrNull { it.key in directions }?.key?.code ?: 0
            return (mask and directionMask.inv()) or direction
        }
    }

    private val pointers = mutableSetOf<Int>()

    private fun updateButton(pressed: Boolean) {
        if (pressed) pressedButtons.add(this) else pressedButtons.remove(this)
        val newState = currentButtons()
        if (newState == keyState) return
        keyState = newState
        when {
            pressed && keyState.toChar() == reset -> {
                Log.d(TAG, "Sending reset")
                resetScreen()
            }

            pressed && keyState.toChar() == exit -> {
                Log.d(TAG, "Sending exit")
                pressedButtons.clear()
                keyState = 0
                sendClickEvent(0.toChar())
                exit()
            }

            else -> sendClickEvent(keyState.toChar())
        }
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (motionEvent.actionMasked) {
            ACTION_DOWN, ACTION_POINTER_DOWN -> {
                if (motionEvent.actionMasked == ACTION_DOWN) pointers.clear()
                val pointerId = motionEvent.getPointerId(motionEvent.actionIndex)
                if (pointers.add(pointerId) && pointers.size == 1) {
                    updateButton(true)
                    view.performHapticFeedback(VIRTUAL_KEY, FLAG_IGNORE_VIEW_SETTING)
                }
            }

            ACTION_UP, ACTION_POINTER_UP -> {
                val pointerId = motionEvent.getPointerId(motionEvent.actionIndex)
                if (pointers.remove(pointerId) && pointers.isEmpty()) {
                    updateButton(false)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        view.performHapticFeedback(VIRTUAL_KEY_RELEASE, FLAG_IGNORE_VIEW_SETTING)
                    }
                    view.performClick()
                }
            }

            ACTION_CANCEL -> {
                pointers.clear()
                updateButton(false)
            }
        }
        return true
    }
}
