package io.maido.m8client

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import io.maido.m8client.M8SDLActivity.Companion.startM8SDLActivity
import io.maido.m8client.M8Util.copyGameControllerDB
import io.maido.m8client.M8Util.isM8

class M8StartActivity : AppCompatActivity(R.layout.nodevice) {
    companion object {
        private const val ACTION_USB_PERMISSION = "io.maido.m8client.USB_PERMISSION"
        private const val TAG = "M8StartActivity"
    }

    private var showButtons = true
    private var audioDevice = 0
    private var audioDriver: String? = null

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    val usbManager = getSystemService(USB_SERVICE) as UsbManager
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null && isM8(device)) {
                            connectToM8(usbManager, device)
                        } else {
                            Log.d(TAG, "Device was not M8")
                        }
                    } else {
                        Log.d(TAG, "Permission denied for device $device")
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                Log.d(TAG, "Device was detached!")
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        registerReceiver(usbReceiver, IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED))
        copyGameControllerDB(this)
        val start = findViewById<Button>(R.id.startButton)
        start.setOnClickListener { start() }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        unregisterReceiver(usbReceiver)
        super.onDestroy()
    }

    private fun start() {
        readPreferenceValues();
        Log.i(TAG, "Searching for an M8 device")
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList
        for (device in deviceList.values) {
            if (isM8(device)) {
                connectToM8WithPermission(usbManager, device)
                break
            }
        }
    }

    private fun readPreferenceValues() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        audioDevice = preferences.getString(getString(R.string.audio_device_pref), "0")!!.toInt()
        audioDriver = preferences.getString(getString(R.string.audio_driver_pref), "AAudio")
        showButtons = preferences.getBoolean(getString(R.string.buttons_pref), true)
    }

    private fun connectToM8WithPermission(usbManager: UsbManager, usbDevice: UsbDevice) {
        if (usbManager.hasPermission(usbDevice)) {
            Log.i(TAG, "Permission granted!")
            connectToM8(usbManager, usbDevice)
        } else {
            Log.i(TAG, "Requesting USB device permission")
            val permissionIntent = PendingIntent.getBroadcast(
                this, 0, Intent(
                    ACTION_USB_PERMISSION
                ), PendingIntent.FLAG_IMMUTABLE
            )
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            registerReceiver(usbReceiver, filter)
            usbManager.requestPermission(usbDevice, permissionIntent)
        }
    }

    private fun connectToM8(usbManager: UsbManager, usbDevice: UsbDevice) {
        val connection = usbManager.openDevice(usbDevice)
        if (connection != null) {
            Log.d(
                TAG,
                "Setting device with id: " + usbDevice.deviceId + " and file descriptor: " + connection.fileDescriptor
            )
            startM8SDLActivity(
                this,
                connection,
                audioDevice,
                showButtons,
                audioDriver
            )
        }
    }


}