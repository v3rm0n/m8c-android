package io.maido.m8client

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.media.AudioManager
import android.os.Build
import android.os.Process
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import io.maido.m8client.settings.GeneralSettings
import org.libsdl.app.SDLActivity
import kotlin.concurrent.thread


class M8SDLActivity : SDLActivity() {

    companion object {
        private const val TAG = "M8SDLActivity"

        private val USB_DEVICE = M8SDLActivity::class.simpleName + ".USB_DEVICE"

        fun startM8SDLActivity(context: Context, usbDevice: UsbDevice) {
            val sdlActivity = Intent(context, M8SDLActivity::class.java)
            sdlActivity.putExtra(USB_DEVICE, usbDevice)
            context.startActivity(sdlActivity)
        }

    }

    private var showButtons = true
    private var running = true
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
        val generalPreferences = GeneralSettings.getGeneralPreferences(this)
        val audioDriver = generalPreferences.audioDriver
        if (audioDriver != null) {
            setAudioDriver(audioDriver)
        }
        lockOrientation(generalPreferences.lockOrientation)
        running = true
        showButtons = generalPreferences.showButtons
        val buttons = findViewById<View>(R.id.buttons)
        buttons.visibility = if (showButtons) View.VISIBLE else View.GONE
        openUsbConnection(generalPreferences.audioDevice)
        thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            Log.d(TAG, "Starting USB Loop thread")
            while (running) {
                loop()
            }
            Log.d(TAG, "USB Loop thread ended")
        }
        super.onStart()
    }

    private fun getBufferSize(): Int {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val framesPerBuffer: String? =
            am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        return framesPerBuffer?.let { str ->
            Integer.parseInt(str).takeUnless { it == 0 }
        } ?: 256 // Use default
    }

    private fun openUsbConnection(audioDeviceId: Int) {
        val usbDevice = getUsbDevice()
        val usbManager = getSystemService(UsbManager::class.java)!!
        usbConnection = usbManager.openDevice(usbDevice)?.also {
            Log.d(
                TAG,
                "Setting file descriptor to ${it.fileDescriptor} and audio device to $audioDeviceId"
            )
            connect(it.fileDescriptor, audioDeviceId, getBufferSize())
        }
    }

    override fun onStop() {
        super.onStop()
        usbConnection?.close()
        running = false
    }

    override fun onUnhandledMessage(command: Int, param: Any?): Boolean {
        if (command == 0x8001 && param is Int) {
            val r = param shr 16
            val g = (param shr 8) and 0xFF
            val b = param and 0xFF
            Log.d(TAG, "Background color changed to $r $g $b")
            val main = findViewById<ViewGroup>(R.id.main)
            main.setBackgroundColor(Color.rgb(r, g, b))
            return true
        }
        return super.onUnhandledMessage(command, param)
    }

    override fun setContentView(view: View) {
        val mainLayout = FrameLayout(this)
        layoutInflater.inflate(R.layout.m8, mainLayout, true)
        val screen = mainLayout.findViewById<ViewGroup>(R.id.screen)
        screen.addView(view)
        super.setContentView(mainLayout)
        setButtonListeners()
    }

    // Hacky way of intercepting touch events on the SDLSurface so we can use margins as invisible buttons
    override fun onSDLTouch(
        touchDevId: Int, pointerFingerId: Int, action: Int, x: Float, y: Float, p: Float
    ) {
        if (!showButtons) {
            // M8 SDL screen is 640x480
            val ratio = mSurface.height / 480.0
            val marginWidth = ((mSurface.width - (640.0 * ratio)) / 2.0) / mSurface.width
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

    private external fun connect(
        fileDescriptor: Int,
        audioDeviceId: Int,
        bufferSize: Int
    )

    private external fun setAudioDriver(audioDriver: String?)

    private external fun lockOrientation(lock: Boolean)
    private external fun loop()


}