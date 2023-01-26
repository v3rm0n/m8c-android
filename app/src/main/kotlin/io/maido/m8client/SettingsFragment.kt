package io.maido.m8client

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        private const val TAG = "SettingsFragment"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val devices = allAudioOutputDevices()
        setListPreferenceData(
            findPreference("audio_device")!!,
            devices.map { it.productName },
            devices.map { it.id.toString() },
            builtInSpeaker()
        )
    }

    private fun allAudioOutputDevices(): List<AudioDeviceInfo> {
        val audioManager =
            activity?.getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices
            .filter { obj: AudioDeviceInfo -> obj.isSink }
            .filter { device: AudioDeviceInfo -> device.type != AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
    }

    private fun builtInSpeaker(): CharSequence {
        val audioManager =
            activity?.getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                Log.d(
                    TAG,
                    "Speaker device id: " + device.id + " type: " + device.type + " is sink: " + device.isSink
                )
                return device.id.toString()
            }
        }
        return "0"
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