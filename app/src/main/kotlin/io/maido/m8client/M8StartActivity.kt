package io.maido.m8client

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import io.maido.m8client.M8SDLActivity.Companion.startM8SDLActivity
import io.maido.m8client.M8Util.copyGameControllerDB
import io.maido.m8client.settings.GamepadSettings
import io.maido.m8client.settings.GeneralSettings


class M8StartActivity : AppCompatActivity(R.layout.settings),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    companion object {
        private const val TAG = "M8StartActivity"
    }

    private lateinit var configuration: M8Configuration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        configuration = M8Configuration(this)
        setSupportActionBar(findViewById(R.id.toolbar))
        copyGameControllerDB(this)
        val start = findViewById<Button>(R.id.startButton)
        start.setOnClickListener { connectToM8() }
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

    private fun connectToM8() {
        val generalPreferences = GeneralSettings.getGeneralPreferences(this)
        configuration.copyConfiguration(
            GamepadSettings.getGamepadPreferences(this) + mapOf(
                M8AudioOption.DEVICE_NAME to generalPreferences.audioDevice.toString(),
                M8GraphicsOption.IDLE_MS to generalPreferences.idleMs.toString(),
                M8AudioOption.BUFFER_SIZE to generalPreferences.audioBuffer.toString(),
            )
        )
        startM8SDLActivity(this)
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