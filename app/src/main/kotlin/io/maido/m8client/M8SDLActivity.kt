package io.maido.m8client

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDeviceConnection
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.get
import org.libsdl.app.SDLActivity
import org.libsdl.app.SDLSurface


class M8SDLActivity : SDLActivity() {

    companion object {
        private const val TAG = "M8SDLActivity"

        private val FILE_DESCRIPTOR = M8SDLActivity::class.simpleName + ".FILE_DESCRIPTOR"
        private val AUDIO_DEVICE = M8SDLActivity::class.simpleName + ".AUDIO_DEVICE"
        private val SHOW_BUTTONS = M8SDLActivity::class.simpleName + ".SHOW_BUTTONS"
        private val AUDIO_DRIVER = M8SDLActivity::class.simpleName + ".AUDIO_DRIVER"

        fun startM8SDLActivity(
            context: Context,
            connection: UsbDeviceConnection,
            audioDeviceId: Int,
            showButtons: Boolean,
            audioDriver: String?
        ) {
            val sdlActivity = Intent(context, M8SDLActivity::class.java)
            sdlActivity.putExtra(FILE_DESCRIPTOR, connection.fileDescriptor)
            sdlActivity.putExtra(AUDIO_DEVICE, audioDeviceId)
            sdlActivity.putExtra(AUDIO_DRIVER, audioDriver)
            sdlActivity.putExtra(SHOW_BUTTONS, showButtons)
            context.startActivity(sdlActivity)
        }
    }

    private var showButtons = true
    private lateinit var sdlSurface: SDLSurface

    override fun onStart() {
        val fileDescriptor = intent.getIntExtra(FILE_DESCRIPTOR, -1)
        val audioDeviceId = intent.getIntExtra(AUDIO_DEVICE, 0)
        val audioDriver = intent.getStringExtra(AUDIO_DRIVER)
        if (audioDriver != null) {
            Log.d(TAG, "Setting audio driver to $audioDriver")
            setAudioDriver(audioDriver)
        }
        showButtons = intent.getBooleanExtra(SHOW_BUTTONS, true)
        val buttons = findViewById<View>(R.id.buttons)
        buttons.visibility = if (showButtons) View.VISIBLE else View.GONE
        Log.d(TAG, "Setting file descriptor to $fileDescriptor and audio device to $audioDeviceId")
        connect(fileDescriptor, audioDeviceId)
        Thread {
            Log.d(TAG, "Starting USB Loop thread")
            while (true) {
                loop()
            }
        }.start()
        super.onStart()
    }

    override fun setContentView(view: View) {
        val mainLayout = FrameLayout(this)
        mainLayout.addView(view)
        layoutInflater.inflate(R.layout.m8layout, mainLayout, true)
        sdlSurface = (view as ViewGroup)[0] as SDLSurface
        super.setContentView(mainLayout)
        setButtonListeners()
    }

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
            Log.d(TAG, "Action $action pointer $pointerFingerId dev $touchDevId")
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
    private external fun loop()
}