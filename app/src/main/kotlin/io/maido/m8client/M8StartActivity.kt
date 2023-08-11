package io.maido.m8client

import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.getBroadcast
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import io.maido.m8client.M8SDLActivity.Companion.startM8SDLActivity
import io.maido.m8client.M8Util.copyGameControllerDB
import io.maido.m8client.M8Util.isM8
import io.maido.m8client.settings.GamepadSettings
import io.maido.m8client.settings.GeneralSettings


class M8StartActivity : AppCompatActivity(R.layout.settings),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    companion object {
        private const val ACTION_USB_PERMISSION = "io.maido.m8client.USB_PERMISSION"
        private const val TAG = "M8StartActivity"
    }

    private lateinit var configuration: M8Configuration

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
                Log.d(TAG, "Device was detached!")
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        configuration = M8Configuration(this)
        setSupportActionBar(findViewById(R.id.toolbar))
        registerReceiver(usbReceiver, IntentFilter(ACTION_USB_DEVICE_DETACHED))
        copyGameControllerDB(this)
        val start = findViewById<Button>(R.id.startButton)
        start.setOnClickListener { start() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                preferences.edit {
                    clear()
                }
                recreate()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        unregisterReceiver(usbReceiver)
    }

    private fun start() {
        Log.i(TAG, "Searching for an M8 device")
        val usbManager = getSystemService(UsbManager::class.java)
        for (device in usbManager.deviceList.values) {
            if (isM8(device)) {
                connectToM8WithPermission(usbManager, device)
                break
            }
        }
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

    private fun connectToM8(usbDevice: UsbDevice) {
        val generalPreferences = GeneralSettings.getGeneralPreferences(this)
        configuration.copyConfiguration(
            GamepadSettings.getGamepadPreferences(this) + mapOf(
                M8AudioOption.DEVICE_NAME to generalPreferences.audioDevice.toString(),
                M8GraphicsOption.IDLE_MS to generalPreferences.idleMs.toString(),
            )
        )
        startM8SDLActivity(this, usbDevice)
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat, pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader, pref.fragment!!
        )
        fragment.arguments = pref.extras
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction().replace(R.id.settings_container, fragment)
            .addToBackStack(null).commit()
        return true
    }
}