package io.maido.m8client

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.usb.UsbDeviceConnection
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.FrameLayout
import org.libsdl.app.SDLActivity

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

    override fun onStart() {
        hideButtonsOnPortrait(resources.configuration.orientation)
        val fileDescriptor = intent.getIntExtra(FILE_DESCRIPTOR, -1)
        val audioDeviceId = intent.getIntExtra(AUDIO_DEVICE, 0)
        val audioDriver = intent.getStringExtra(AUDIO_DRIVER)
        if (audioDriver != null) {
            Log.d(TAG, "Setting audio driver to $audioDriver")
            setAudioDriver(audioDriver)
        }
        val showButtons = intent.getBooleanExtra(SHOW_BUTTONS, true)
        this.showButtons = showButtons
        val buttons = findViewById<View>(R.id.buttons)
        buttons.visibility = if (showButtons) View.VISIBLE else View.INVISIBLE
        Log.d(TAG, "Setting file descriptor to $fileDescriptor and audio device to $audioDeviceId")
        connect(fileDescriptor, audioDeviceId)
        Thread {
            while (true) {
                loop()
            }
        }.start()
        super.onStart()
    }

    override fun setContentView(view: View) {
        val mainLayout = FrameLayout(this)
        mainLayout.addView(view)
        view.setOnTouchListener(object : OnTouchListener {
            private val gestureDetector = GestureDetector(this@M8SDLActivity,
                object : SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        Log.d("TEST", "onDoubleTap")
                        return true
                    }

                    override fun onLongPress(e: MotionEvent) {
                        Log.d("TEST", "onLongPress")
                    }
                })

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (gestureDetector.onTouchEvent(event)) {
                    v.performClick()
                    return true
                }
                return false
            }
        })
        layoutInflater.inflate(R.layout.m8layout, mainLayout, true)
        super.setContentView(mainLayout)
        setButtonListeners()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideButtonsOnPortrait(newConfig.orientation)
    }

    private fun hideButtonsOnPortrait(currentOrientation: Int) {
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            val buttons = findViewById<View>(R.id.buttons)
            buttons.visibility = View.INVISIBLE
        }
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (showButtons) {
                val buttons = findViewById<View>(R.id.buttons)
                buttons.visibility = View.VISIBLE
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
    }

    override fun getMainFunction() = "android_main"

    private external fun connect(fileDescriptor: Int, audioDeviceId: Int)
    private external fun setAudioDriver(audioDriver: String?)
    private external fun loop()
}