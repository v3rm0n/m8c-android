package io.maido.m8client.m8

import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.core.graphics.withScale
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.math.roundToInt


class M8Device(
    private val port: UsbSerialPort,
    onError: (e: Exception) -> Unit,
) : SurfaceHolder.Callback {

    companion object {
        private const val TAG = "M8Device"
    }

    private val m8Screen = M8Screen()

    private val slipFrameQueue = SlipFrameProcessor(m8Screen::draw, onError)
    private val usbIoManager = SerialInputOutputManager(port, slipFrameQueue)

    private var surface: Surface? = null
    private var xRatio = 4
    private var yRatio = 4

    init {
        Log.d(TAG, "Starting rendering")
        usbIoManager.start()
        thread {
            while (true) {
                surface?.lockHardwareCanvas()?.withScale(xRatio.toFloat(), yRatio.toFloat()) {
                    drawBitmap(
                        m8Screen.screen,
                        0F,
                        0F,
                        null
                    )
                    surface?.unlockCanvasAndPost(this)
                }
            }
        }
    }

    fun sendCommand(cmd: Char) {
        Log.d(TAG, "Sending command ${cmd.hex}")
        port.write(byteArrayOf("C".b, cmd.b), 5)
    }

    private fun enableDisplay() {
        Log.d(TAG, "Enable display")
        port.write(byteArrayOf("E".b), 5)
        sleep(5)
        resetDisplay()
    }

    private fun resetDisplay() {
        Log.d(TAG, "Reset display");
        port.write(byteArrayOf("E".b, "R".b), 5)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "Surface created")
        surface = holder.surface
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "Surface changed $width x $height")
        surface = holder.surface
        this.xRatio = (width / 320F).roundToInt()
        this.yRatio = (height / 240F).roundToInt()
        enableDisplay()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "Surface destroyed")
        surface = null
    }

}