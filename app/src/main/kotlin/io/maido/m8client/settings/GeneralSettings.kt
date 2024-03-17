package io.maido.m8client.settings

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.core.content.getSystemService
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import io.maido.m8client.BuildConfig
import io.maido.m8client.R

class GeneralSettings : PreferenceFragmentCompat() {

    companion object {
        fun getGeneralPreferences(context: Context): GeneralPreferences {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val audioDevice =
                preferences.getString(context.getString(R.string.audio_device_pref), "0")!!.toInt()
            val audioDriver =
                preferences.getString(context.getString(R.string.audio_driver_pref), "AAudio")
            val showButtons = preferences.getBoolean(context.getString(R.string.buttons_pref), true)
            val lockOrientation =
                preferences.getBoolean(context.getString(R.string.lock_orientation_pref), false)
            val idleMs = preferences.getString(context.getString(R.string.idle_ms_pref), "0")!!
            val audioBuffer =
                preferences.getString(context.getString(R.string.audio_buffer_pref), "4096")!!
            val useNewLayout =
                preferences.getBoolean(context.getString(R.string.new_button_layout_pref), false)
            return GeneralPreferences(
                showButtons,
                lockOrientation,
                useNewLayout,
                audioDevice,
                audioDriver,
                audioBuffer.toInt(),
                idleMs.toInt()
            )
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        addAudioDevicePreferenceValues()
        val version: EditTextPreference? = findPreference(getString(R.string.version_pref))
        version?.title = "Version ${BuildConfig.VERSION_NAME}"
        val pref =
            findPreference<SwitchPreferenceCompat>(getString(R.string.new_button_layout_pref))!!
        setOrientationLockValue(pref.isChecked)
        pref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                setOrientationLockValue(newValue == true)
                return@OnPreferenceChangeListener true
            }
    }

    private fun setOrientationLockValue(newLayoutEnabled: Boolean) {
        findPreference<SwitchPreferenceCompat>(getString(R.string.lock_orientation_pref))?.also {
            it.isEnabled = !newLayoutEnabled
            if (newLayoutEnabled) {
                it.isChecked = false
            }
        }
    }

    private fun addAudioDevicePreferenceValues() {
        val devices = allAudioOutputDevices()
        setListPreferenceData(
            findPreference(getString(R.string.audio_device_pref))!!,
            devices.map { it.productName },
            devices.map { it.id.toString() },
            builtInSpeaker()?.id?.toString(),
        )
    }

    private fun allAudioOutputDevices(): List<AudioDeviceInfo> {
        val audioManager = activity?.getSystemService<AudioManager>()
        val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices
            ?.filter { it.isSink }
            ?.filter { it.type != AudioDeviceInfo.TYPE_BUILTIN_EARPIECE } ?: emptyList()
    }

    private fun builtInSpeaker(): AudioDeviceInfo? {
        val audioManager = activity?.getSystemService<AudioManager>()
        val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices?.firstOrNull { device ->
            device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }
    }

    private fun setListPreferenceData(
        lp: ListPreference,
        entries: List<CharSequence>,
        values: List<CharSequence>,
        default: CharSequence?
    ) {
        lp.entries = entries.toTypedArray()
        default?.map { lp.setDefaultValue(it) }
        lp.entryValues = values.toTypedArray()
    }

}

data class GeneralPreferences(
    val showButtons: Boolean = true,
    val lockOrientation: Boolean = false,
    val useNewLayout: Boolean = false,
    val audioDevice: Int = 0,
    val audioDriver: String? = null,
    val audioBuffer: Int = 4096,
    val idleMs: Int = 0
)