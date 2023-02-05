package io.maido.m8client.m8

import android.util.Log
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.nio.ByteBuffer
import java.nio.ByteBuffer.wrap
import java.nio.ByteOrder


class SlipFrameProcessor(
    private val onFrame: (frame: ByteBuffer) -> Unit,
    private val onError: (e: Exception) -> Unit,
) : SerialInputOutputManager.Listener {

    companion object {
        private const val TAG = "Slip"
        private const val END = 0xC0.toByte()
        private const val ESC = 0xDB.toByte()
        private const val ESC_END = 0xDC.toByte()
        private const val ESC_ESC = 0xDD.toByte()
    }

    private var frameBytes = mutableListOf<Byte>()
    private var escaped = false

    override fun onNewData(data: ByteArray) {
        for (byte in data) {
            if (byte == ESC) {
                escaped = true
            } else if (escaped) {
                when (byte) {
                    ESC_END -> frameBytes.add(END)
                    ESC_ESC -> frameBytes.add(ESC)
                    else -> throw RuntimeException("Unknown byte $byte")
                }
                escaped = false
            } else if (byte == END) {
                val byteBuffer =
                    wrap(frameBytes.toByteArray()).also { it.order(ByteOrder.LITTLE_ENDIAN) }
                onFrame(byteBuffer)
                frameBytes.clear()
            } else {
                frameBytes.add(byte)
            }
        }
    }

    override fun onRunError(e: Exception) {
        Log.d(TAG, "Error in slip processing", e)
        onError(e)
    }
}