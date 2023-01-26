package io.maido.m8client

import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.OnTouchListener

internal class M8TouchListener(
    private val key: M8Key,
    private val exitHandler: () -> Unit
) : OnTouchListener {
    private val reset = 132.toChar()
    private val exit = 96.toChar()
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (motionEvent.actionMasked) {
            ACTION_DOWN -> {
                modifiers.add(key)
                when (val code = key.getCode(modifiers)) {
                    reset -> {
                        Log.d(TAG, "Sending reset")
                        resetScreen()
                    }
                    exit -> {
                        Log.d(TAG, "Sending exit")
                        exitHandler()
                    }
                    else -> {
                        Log.d(TAG, "Sending $key as ${code.code}")
                        sendClickEvent(code)
                    }
                }
            }
            ACTION_UP -> {
                modifiers.remove(key)
                Log.d(TAG, "Key up $key")
                sendClickEvent(0.toChar())
                view.performClick()
            }
        }
        return true
    }

    private external fun sendClickEvent(event: Char)

    private external fun resetScreen()

    companion object {
        private val TAG = M8TouchListener::class.simpleName
        private val modifiers = HashSet<M8Key>()
    }
}