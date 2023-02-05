package io.maido.m8client

import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import android.view.View.OnTouchListener
import io.maido.m8client.m8.M8Key

internal class M8TouchListener(
    private val key: M8Key,
    private val onTouch: (code: Char) -> Unit,
) : OnTouchListener {

    companion object {
        private val TAG = M8TouchListener::class.simpleName
        private val modifiers = HashSet<M8Key>()
        fun resetModifiers() = modifiers.clear()

        fun handleTouch(key: M8Key, action: Int, onTouch: (code: Char) -> Unit): Boolean {
            when (action) {
                ACTION_DOWN, ACTION_POINTER_DOWN -> {
                    modifiers.add(key)
                    when (val code = key.getCode(modifiers)) {
                        else -> {
                            Log.d(TAG, "Sending $key as ${code.code}")
                            onTouch(code)
                        }
                    }
                }
                ACTION_UP, ACTION_POINTER_UP -> {
                    modifiers.remove(key)
                    Log.d(TAG, "Key up $key")
                    onTouch(0.toChar())
                }
            }
            return true
        }
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        handleTouch(key, motionEvent.actionMasked, onTouch)
        when (motionEvent.actionMasked) {
            ACTION_UP -> {
                view.performClick()
            }
        }
        return true
    }
}