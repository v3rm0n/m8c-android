package io.maido.m8client.settings

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
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
        fun getBestInputDeviceId(context: Context): Int {
            val audioManager = context.getSystemService<AudioManager>() ?: return 0
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) ?: return 0
            val candidates = devices.filter { it.isSource }
            return candidates.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
            }?.id
                ?: candidates.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                }?.id
                ?: candidates.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }?.id
                ?: candidates.firstOrNull()?.id
                ?: 0
        }

        fun getBestOutputDeviceId(context: Context): Int {
            val audioManager = context.getSystemService<AudioManager>() ?: return 0
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: return 0
            val candidates = devices.filter { d ->
                d.isSink &&
                d.type != AudioDeviceInfo.TYPE_BUILTIN_EARPIECE &&
                !(d.type == AudioDeviceInfo.TYPE_USB_DEVICE &&
                  d.productName.contains("M8", ignoreCase = true))
            }
            return candidates.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
            }?.id
                ?: candidates.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                }?.id
                ?: candidates.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }?.id
                ?: candidates.firstOrNull()?.id
                ?: 0
        }

        fun getGeneralPreferences(context: Context): GeneralPreferences {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val useDefaultAudio =
                preferences.getBoolean(context.getString(R.string.use_default_audio_pref), true)
            val audioDevice =
                preferences.getString(context.getString(R.string.audio_device_pref), "0")!!.toInt()
            val useDefaultAudioInput =
                preferences.getBoolean(context.getString(R.string.use_default_audio_input_pref), true)
            val audioInputDevice = if (useDefaultAudioInput) {
                getBestInputDeviceId(context)
            } else {
                preferences.getString(context.getString(R.string.audio_input_device_pref), "0")!!.toInt()
            }
            val audioDriver =
                preferences.getString(context.getString(R.string.audio_driver_pref), "AAudio")
                    .let { if (it == "android") "AAudio" else it }
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
                useDefaultAudio,
                useDefaultAudioInput,
                audioDevice,
                audioInputDevice,
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

        val useDefaultAudioPref =
            findPreference<SwitchPreferenceCompat>(getString(R.string.use_default_audio_pref))!!
        setAudioOutputSelectionEnabled(!useDefaultAudioPref.isChecked)
        useDefaultAudioPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                setAudioOutputSelectionEnabled(newValue != true)
                return@OnPreferenceChangeListener true
            }

        val useDefaultAudioInputPref =
            findPreference<SwitchPreferenceCompat>(getString(R.string.use_default_audio_input_pref))!!
        setAudioInputSelectionEnabled(!useDefaultAudioInputPref.isChecked)
        useDefaultAudioInputPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                setAudioInputSelectionEnabled(newValue != true)
                return@OnPreferenceChangeListener true
            }

        val pref =
            findPreference<SwitchPreferenceCompat>(getString(R.string.new_button_layout_pref))!!
        setOrientationLockValue(pref.isChecked)
        pref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                setOrientationLockValue(newValue == true)
                return@OnPreferenceChangeListener true
            }
    }

    private fun setAudioOutputSelectionEnabled(enabled: Boolean) {
        findPreference<ListPreference>(getString(R.string.audio_device_pref))?.isEnabled = enabled
        findPreference<ListPreference>(getString(R.string.audio_driver_pref))?.isEnabled = enabled
    }

    private fun setAudioInputSelectionEnabled(enabled: Boolean) {
        findPreference<ListPreference>(getString(R.string.audio_input_device_pref))?.isEnabled = enabled
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
        val outputDevices = allAudioOutputDevices()
        setListPreferenceData(
            findPreference(getString(R.string.audio_device_pref))!!,
            outputDevices.map { it.productName },
            outputDevices.map { it.id.toString() },
            builtInSpeaker()?.id?.toString(),
        )
        val inputDevices = allAudioInputDevices()
        setListPreferenceData(
            findPreference(getString(R.string.audio_input_device_pref))!!,
            inputDevices.map { it.productName },
            inputDevices.map { it.id.toString() },
            inputDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }?.id?.toString(),
        )
    }

    private fun allAudioInputDevices(): List<AudioDeviceInfo> {
        val audioManager = activity?.getSystemService<AudioManager>()
        return audioManager?.getDevices(AudioManager.GET_DEVICES_INPUTS)
            ?.filter { it.isSource } ?: emptyList()
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
    val useDefaultAudio: Boolean = true,
    val useDefaultAudioInput: Boolean = true,
    val audioDevice: Int = 0,
    val audioInputDevice: Int = 0,
    val audioDriver: String? = null,
    val audioBuffer: Int = 4096,
    val idleMs: Int = 0
)
