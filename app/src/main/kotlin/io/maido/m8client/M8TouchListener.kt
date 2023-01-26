package io.maido.m8client

import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.OnTouchListener

internal class M8TouchListener(private val key: M8Key) : OnTouchListener {
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (motionEvent.actionMasked) {
            ACTION_DOWN -> {
                modifiers.add(key)
                val code = key.getCode(modifiers)
                Log.d(TAG, "Sending $key as $code")
                sendClickEvent(code)
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

    companion object {
        private val TAG = M8TouchListener::class.simpleName
        private val modifiers = HashSet<M8Key>()
    }
}