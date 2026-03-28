package io.maido.m8client

import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.getBroadcast
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import io.maido.m8client.M8Key.*
import io.maido.m8client.M8Util.isM8
import io.maido.m8client.settings.GeneralSettings
import org.libsdl.app.SDLActivity


class M8SDLActivity : SDLActivity() {

    companion object {
        private const val TAG = "M8SDLActivity"
        private const val ACTION_USB_PERMISSION = "io.maido.m8client.USB_PERMISSION"

        fun startM8SDLActivity(context: Context) {
            val sdlActivity = Intent(context, M8SDLActivity::class.java)
            context.startActivity(sdlActivity)
        }

    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device = getExtraDevice(intent)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null && isM8(device)) {
                            connectToM8(device)
                        } else {
                            Log.d(TAG, "Device was not M8")
                        }
                    } else {
                        Log.d(TAG, "Permission denied for device $device")
                    }
                }
            } else if (ACTION_USB_DEVICE_DETACHED == action) {
                val device = getExtraDevice(intent)
                if (device != null && isM8(device)) {
                    Log.i(TAG, "M8 detached, exiting")
                    M8TouchListener.exit()
                }
            }
        }
    }

    @Suppress("Deprecation")
    private fun getExtraDevice(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }

    private var usbConnection: UsbDeviceConnection? = null
    private var screenMidiListener: M8ScreenTouchListener? = null
    private var useDefaultAudio = false
    private var useDefaultAudioInput = false
    private var audioBuffer = 4096
    private var currentAudioDeviceId = 0
    private var currentAudioInputDeviceId = 0

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            if (useDefaultAudio) updateAudioOutputDevice()
            if (useDefaultAudioInput) updateAudioInputDevice()
        }
        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
            if (useDefaultAudio) updateAudioOutputDevice()
            if (useDefaultAudioInput) updateAudioInputDevice()
        }
    }

    private fun updateAudioOutputDevice() {
        val newDeviceId = GeneralSettings.getBestOutputDeviceId(this)
        if (newDeviceId == currentAudioDeviceId) return
        Log.i(TAG, "Audio output device changed: $currentAudioDeviceId -> $newDeviceId")
        currentAudioDeviceId = newDeviceId
        hintAudioOutputDevice(newDeviceId)
        Thread { restartAudioOutput(audioBuffer) }.start()
    }

    private fun updateAudioInputDevice() {
        val newDeviceId = GeneralSettings.getBestInputDeviceId(this)
        if (newDeviceId == currentAudioInputDeviceId) return
        Log.i(TAG, "Audio input device changed: $currentAudioInputDeviceId -> $newDeviceId")
        currentAudioInputDeviceId = newDeviceId
        hintAudioInputDevice(newDeviceId)
    }

    override fun onStart() {
        Log.i(TAG, "Searching for an M8 device")
        super.onStart()
        if (usbConnection != null) {
            Log.d(TAG, "Already connected to M8, skipping reconnect")
            return
        }
        val usbManager = getSystemService(UsbManager::class.java)
        for (device in usbManager.deviceList.values) {
            if (isM8(device)) {
                connectToM8WithPermission(usbManager, device)
                break
            }
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        usbConnection?.close()
        if (useDefaultAudio || useDefaultAudioInput) {
            getSystemService(AudioManager::class.java).unregisterAudioDeviceCallback(audioDeviceCallback)
        }
    }

    override fun onStop() {
        Log.d(TAG, "onStop()")
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        val generalPreferences = GeneralSettings.getGeneralPreferences(this)
        useDefaultAudio = generalPreferences.useDefaultAudio
        useDefaultAudioInput = generalPreferences.useDefaultAudioInput
        audioBuffer = generalPreferences.audioBuffer
        hintAudioDriver(generalPreferences.audioDriver)
        setAudioGain(generalPreferences.audioGain)
        if (useDefaultAudio) {
            currentAudioDeviceId = GeneralSettings.getBestOutputDeviceId(this)
            hintAudioOutputDevice(currentAudioDeviceId)
        }
        currentAudioInputDeviceId = generalPreferences.audioInputDevice
        hintAudioInputDevice(generalPreferences.audioInputDevice)
        if (useDefaultAudio || useDefaultAudioInput) {
            getSystemService(AudioManager::class.java)
                .registerAudioDeviceCallback(audioDeviceCallback, Handler(Looper.getMainLooper()))
        }
        lockOrientation(
            if (generalPreferences.useNewLayout) "Portrait PortraitUpsideDown"
            else if (generalPreferences.lockOrientation) "LandscapeLeft LandscapeRight" else null
        )
        registerReceiver(usbReceiver, IntentFilter(ACTION_USB_DEVICE_DETACHED))
    }

    private fun connectToM8(device: UsbDevice) {
        val usbManager = getSystemService(UsbManager::class.java)!!
        usbConnection = usbManager.openDevice(device)?.also {
            Log.d(TAG, "Setting file descriptor to ${it.fileDescriptor} ")
            connect(it.fileDescriptor)
        }
    }

    private fun requestM8Permission(usbManager: UsbManager, usbDevice: UsbDevice) {
        val intent = Intent(ACTION_USB_PERMISSION)
        val permissionIntent = getBroadcast(this, 0, intent, FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                usbReceiver,
                IntentFilter(ACTION_USB_PERMISSION),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                usbReceiver,
                IntentFilter(ACTION_USB_PERMISSION)
            )
        }
        usbManager.requestPermission(usbDevice, permissionIntent)
    }

    private fun connectToM8WithPermission(usbManager: UsbManager, usbDevice: UsbDevice) {
        if (usbManager.hasPermission(usbDevice)) {
            Log.i(TAG, "Permission granted!")
            connectToM8(usbDevice)
        } else {
            Log.i(TAG, "Requesting USB device permission")
            requestM8Permission(usbManager, usbDevice)
        }
    }

    private fun routeToMidiListener(ev: MotionEvent, targetView: View, listener: M8ScreenTouchListener) {
        val loc = IntArray(2)
        targetView.getLocationInWindow(loc)
        val left = loc[0].toFloat()
        val top = loc[1].toFloat()
        val right = left + targetView.width
        val bottom = top + targetView.height
        val action = ev.actionMasked
        val pointerIdx = when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val ai = ev.actionIndex
                if (ev.getX(ai) >= left && ev.getX(ai) < right &&
                    ev.getY(ai) >= top && ev.getY(ai) < bottom) ai else -1
            }
            MotionEvent.ACTION_MOVE -> (0 until ev.pointerCount).firstOrNull { i ->
                ev.getX(i) >= left && ev.getX(i) < right &&
                ev.getY(i) >= top && ev.getY(i) < bottom
            } ?: -1
            else -> -1
        }
        if (pointerIdx >= 0) {
            val lx = ev.getX(pointerIdx) - left
            val ly = ev.getY(pointerIdx) - top
            val mappedAction = when (action) {
                MotionEvent.ACTION_POINTER_DOWN -> MotionEvent.ACTION_DOWN
                MotionEvent.ACTION_POINTER_UP -> MotionEvent.ACTION_UP
                else -> action
            }
            val synth = MotionEvent.obtain(ev.downTime, ev.eventTime, mappedAction, lx, ly, ev.metaState)
            listener.onTouch(targetView, synth)
            synth.recycle()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        mSurface?.let { surface ->
            screenMidiListener?.let { routeToMidiListener(ev, surface, it) }
        }
        return super.dispatchTouchEvent(ev)
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "Orientation to portrait, show alternative buttons")
            findViewById<View>(R.id.leftButtonsAlt)?.visibility = View.VISIBLE
            findViewById<View>(R.id.rightButtonsAlt)?.visibility = View.VISIBLE
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "Orientation to portrait, hide alternative buttons")
            findViewById<View>(R.id.leftButtonsAlt)?.visibility = View.GONE
            findViewById<View>(R.id.rightButtonsAlt)?.visibility = View.GONE
        }
        Handler(Looper.getMainLooper()).postDelayed({
            M8TouchListener.resetScreen();
        }, 100)
        super.onConfigurationChanged(newConfig)
    }

    override fun setContentView(view: View) {
        val mainLayout = FrameLayout(this)
        val m8Layout = layoutInflater.inflate(R.layout.m8, mainLayout, true)
        val generalPreferences = GeneralSettings.getGeneralPreferences(this)
        if (generalPreferences.showButtons) {
            val layout =
                if (generalPreferences.useNewLayout) R.layout.buttons_alt else R.layout.buttons
            val buttons = layoutInflater.inflate(layout, mainLayout, false)
            setButtonListeners(buttons)
            mainLayout.addView(buttons)
        } else {
            setButtonListeners(m8Layout)
        }
        val screen = mainLayout.findViewById<ViewGroup>(R.id.screen)
        screen.addView(view)
        if (generalPreferences.touchCcEnabled) {
            screenMidiListener = M8ScreenTouchListener(
                generalPreferences.touchCcChannel - 1,
                generalPreferences.touchCcX,
                generalPreferences.touchCcY,
                ::sendMidiCC
            )
        }
        super.setContentView(mainLayout)
    }

    private fun setButtonListeners(buttons: View) {
        mapOf(
            R.id.up to UP,
            R.id.upAlt to UP,
            R.id.down to DOWN,
            R.id.downAlt to DOWN,
            R.id.left to LEFT,
            R.id.leftAlt to LEFT,
            R.id.right to RIGHT,
            R.id.rightAlt to RIGHT,
            R.id.play to PLAY,
            R.id.playAlt to PLAY,
            R.id.shift to SHIFT,
            R.id.shiftAlt to SHIFT,
            R.id.option to OPTION,
            R.id.optionAlt to OPTION,
            R.id.edit to EDIT,
            R.id.editAlt to EDIT,
        )
            .forEach { (id, key) -> setListener(buttons, id, key) }
    }

    private fun setListener(buttons: View, viewId: Int, key: M8Key) {
        buttons.findViewById<View>(viewId)?.setOnTouchListener(M8TouchListener(key))
    }

    private external fun connect(fileDescriptor: Int)

    private external fun hintAudioDriver(audioDriver: String?)

    private external fun hintAudioOutputDevice(deviceId: Int)

    private external fun restartAudioOutput(bufferSize: Int)

    private external fun hintAudioInputDevice(deviceId: Int)

    private external fun setAudioGain(gain: Float)

    private external fun sendMidiCC(channel: Int, cc: Int, value: Int)

    private external fun lockOrientation(orientation: String?)

}