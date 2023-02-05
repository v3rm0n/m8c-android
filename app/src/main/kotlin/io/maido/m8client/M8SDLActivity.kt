package io.maido.m8client

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.get
import org.libsdl.app.SDLActivity
import org.libsdl.app.SDLSurface
import kotlin.concurrent.thread


class M8SDLActivity : SDLActivity() {

    companion object {
        private const val TAG = "M8SDLActivity"

        private val USB_DEVICE = M8SDLActivity::class.simpleName + ".USB_DEVICE"
        private val AUDIO_DEVICE = M8SDLActivity::class.simpleName + ".AUDIO_DEVICE"
        private val SHOW_BUTTONS = M8SDLActivity::class.simpleName + ".SHOW_BUTTONS"
        private val AUDIO_DRIVER = M8SDLActivity::class.simpleName + ".AUDIO_DRIVER"
        private val LOCK_ORIENTATION = M8SDLActivity::class.simpleName + ".LOCK_ORIENTATION"

        fun startM8SDLActivity(
            context: Context,
            usbDevice: UsbDevice,
            audioDeviceId: Int,
            showButtons: Boolean,
            audioDriver: String?,
            lockOrientation: Boolean,
        ) {
            val sdlActivity = Intent(context, M8SDLActivity::class.java)
            sdlActivity.putExtra(USB_DEVICE, usbDevice)
            sdlActivity.putExtra(AUDIO_DEVICE, audioDeviceId)
            sdlActivity.putExtra(AUDIO_DRIVER, audioDriver)
            sdlActivity.putExtra(SHOW_BUTTONS, showButtons)
            sdlActivity.putExtra(LOCK_ORIENTATION, lockOrientation)
            context.startActivity(sdlActivity)
        }

    }

    private var showButtons = true
    private var running = true
    private lateinit var sdlSurface: SDLSurface
    private var usbConnection: UsbDeviceConnection? = null

    @Suppress("Deprecation")
    private fun getUsbDevice(): UsbDevice {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(USB_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(USB_DEVICE)
        } ?: throw IllegalStateException("No device!")
    }

    override fun onStart() {
        val audioDriver = intent.getStringExtra(AUDIO_DRIVER)
        if (audioDriver != null) {
            Log.d(TAG, "Setting audio driver to $audioDriver")
            setAudioDriver(audioDriver)
        }
        val lock = intent.getBooleanExtra(LOCK_ORIENTATION, false)
        lockOrientation(lock)
        running = true
        showButtons = intent.getBooleanExtra(SHOW_BUTTONS, true)
        val buttons = findViewById<View>(R.id.buttons)
        buttons.visibility = if (showButtons) View.VISIBLE else View.GONE
        openUsbConnection()
        thread {
            Log.d(TAG, "Starting USB Loop thread")
            while (running) {
                loop()
            }
            Log.d(TAG, "USB Loop thread ended")
        }
        super.onStart()
    }

    private fun openUsbConnection() {
        val audioDeviceId = intent.getIntExtra(AUDIO_DEVICE, 0)
        val usbDevice = getUsbDevice()
        val usbManager = getSystemService(UsbManager::class.java)!!
        usbConnection = usbManager.openDevice(usbDevice)?.also {
            Log.d(
                TAG,
                "Setting file descriptor to ${it.fileDescriptor} and audio device to $audioDeviceId"
            )
            connect(it.fileDescriptor, audioDeviceId)
        }
    }

    override fun onStop() {
        super.onStop()
        usbConnection?.close()
        running = false
    }

    override fun setContentView(view: View) {
        val mainLayout = FrameLayout(this)
        mainLayout.addView(view)
        layoutInflater.inflate(R.layout.m8, mainLayout, true)
        sdlSurface = (view as ViewGroup)[0] as SDLSurface
        super.setContentView(mainLayout)
        setButtonListeners()
    }

    // Hacky way of intercepting touch events on the SDLSurface so we can use margins as invisible buttons
    override fun onSDLTouch(
        touchDevId: Int,
        pointerFingerId: Int,
        action: Int,
        x: Float,
        y: Float,
        p: Float
    ) {
        if (!showButtons) {
            // M8 SDL screen is 640x480
            val ratio = sdlSurface.height / 480.0
            val marginWidth = ((sdlSurface.width - (640.0 * ratio)) / 2.0) / sdlSurface.width
            val isOnLeftMargin = x < marginWidth
            val isDown = isOnLeftMargin && y > 2.0 / 3.0
            val isUp = isOnLeftMargin && y < 1.0 / 3.0
            val isLeft = isOnLeftMargin && !isDown && !isUp && x < marginWidth / 2.0
            val isRight = isOnLeftMargin && !isDown && !isUp && !isLeft
            val isOnRightMargin = x > (1.0 - marginWidth)
            val isShift = isOnRightMargin && y > 0.5 && x < (1.0 - marginWidth / 2.0)
            val isPlay = isOnRightMargin && y > 0.5 && !isShift
            val isOption = isOnRightMargin && y <= 0.5 && x < (1.0 - marginWidth / 2.0)
            val isEdit = isOnRightMargin && y <= 0.5 && !isOption
            if (isDown) {
                M8TouchListener.handleTouch(M8Key.DOWN, action)
            } else if (isUp) {
                M8TouchListener.handleTouch(M8Key.UP, action)
            } else if (isLeft) {
                M8TouchListener.handleTouch(M8Key.LEFT, action)
            } else if (isRight) {
                M8TouchListener.handleTouch(M8Key.RIGHT, action)
            } else if (isShift) {
                M8TouchListener.handleTouch(M8Key.SHIFT, action)
            } else if (isPlay) {
                M8TouchListener.handleTouch(M8Key.PLAY, action)
            } else if (isOption) {
                M8TouchListener.handleTouch(M8Key.OPTION, action)
            } else if (isEdit) {
                M8TouchListener.handleTouch(M8Key.EDIT, action)
            }
        }
    }


    private fun setButtonListeners() {
        val up = findViewById<View>(R.id.up)
        up.setOnTouchListener(M8TouchListener(M8Key.UP))
        val down = findViewById<View>(R.id.down)
        down.setOnTouchListener(M8TouchListener(M8Key.DOWN))
        val left = findViewById<View>(R.id.left)
        left.setOnTouchListener(M8TouchListener(M8Key.LEFT))
        val right = findViewById<View>(R.id.right)
        right.setOnTouchListener(M8TouchListener(M8Key.RIGHT))
        val play = findViewById<View>(R.id.play)
        play.setOnTouchListener(M8TouchListener(M8Key.PLAY))
        val shift = findViewById<View>(R.id.shift)
        shift.setOnTouchListener(M8TouchListener(M8Key.SHIFT))
        val option = findViewById<View>(R.id.option)
        option.setOnTouchListener(M8TouchListener(M8Key.OPTION))
        val edit = findViewById<View>(R.id.edit)
        edit.setOnTouchListener(M8TouchListener(M8Key.EDIT))
        M8TouchListener.resetModifiers()
    }

    override fun getMainFunction() = "android_main"

    private external fun connect(fileDescriptor: Int, audioDeviceId: Int)
    private external fun setAudioDriver(audioDriver: String?)

    private external fun lockOrientation(lock: Boolean)
    private external fun loop()
}