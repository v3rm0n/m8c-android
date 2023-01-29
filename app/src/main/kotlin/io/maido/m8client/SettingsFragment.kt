package io.maido.m8client

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        addAudioDevicePreferenceValues()
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