package io.maido.m8client.settings

import android.content.Context
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import io.maido.m8client.R

class KeyboardSettings : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {
            title = getString(R.string.keyboard_mappings)
        }
        for (binding in KeyboardBinding.entries) {
            preferenceScreen.addPreference(Preference(requireContext()).apply {
                key = binding.preferenceKey
                setTitle(binding.title)
                isPersistent = false
                setOnPreferenceClickListener {
                    showKeyPrompt(binding)
                    true
                }
            })
        }
        refreshSummaries()
    }

    private fun refreshSummaries() {
        val preferences = getDefaultSharedPreferences(requireContext())
        for (binding in KeyboardBinding.entries) {
            val code = preferences.getInt(binding.preferenceKey, KeyEvent.KEYCODE_UNKNOWN)
            findPreference<Preference>(binding.preferenceKey)?.summary =
                if (code == KeyEvent.KEYCODE_UNKNOWN) getString(R.string.keyboard_default_binding)
                else KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_").replace('_', ' ')
        }
    }

    private fun saveBinding(binding: KeyboardBinding, code: Int) {
        val preferences = getDefaultSharedPreferences(requireContext())
        preferences.edit {
            for (other in KeyboardBinding.entries) {
                if (preferences.getInt(other.preferenceKey, KeyEvent.KEYCODE_UNKNOWN) == code) {
                    remove(other.preferenceKey)
                }
            }
            putInt(binding.preferenceKey, code)
        }
        refreshSummaries()
    }

    private fun showKeyPrompt(binding: KeyboardBinding) {
        var pressedKey = KeyEvent.KEYCODE_UNKNOWN
        AlertDialog.Builder(requireContext())
            .setTitle(binding.title)
            .setMessage(R.string.keyboard_press_key)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.keyboard_default_binding) { _, _ ->
                getDefaultSharedPreferences(requireContext()).edit { remove(binding.preferenceKey) }
                refreshSummaries()
            }
            .setOnKeyListener { dialog, code, event ->
                if (!KeyboardBinding.isKeyboardEvent(event) || code in reservedKeys) {
                    false
                } else {
                    if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                        pressedKey = code
                    } else if (event.action == KeyEvent.ACTION_UP && code == pressedKey && !event.isCanceled) {
                        saveBinding(binding, code)
                        dialog.dismiss()
                    }
                    true
                }
            }
            .show()
    }

    private companion object {
        val reservedKeys = setOf(
            KeyEvent.KEYCODE_UNKNOWN, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
        )
    }
}

internal enum class KeyboardBinding(val title: Int, val sdlKeyCode: Int) {
    UP(R.string.up, KeyEvent.KEYCODE_DPAD_UP),
    DOWN(R.string.down, KeyEvent.KEYCODE_DPAD_DOWN),
    LEFT(R.string.left, KeyEvent.KEYCODE_DPAD_LEFT),
    RIGHT(R.string.right, KeyEvent.KEYCODE_DPAD_RIGHT),
    SHIFT(R.string.shift, KeyEvent.KEYCODE_SHIFT_LEFT),
    PLAY(R.string.play, KeyEvent.KEYCODE_SPACE),
    OPTION(R.string.option, KeyEvent.KEYCODE_Z),
    EDIT(R.string.edit, KeyEvent.KEYCODE_X);

    val preferenceKey = "physical_key_${name.lowercase()}"

    companion object {
        fun isKeyboardEvent(event: KeyEvent): Boolean {
            val device = event.device ?: return false
            val keyboardSource = event.isFromSource(InputDevice.SOURCE_KEYBOARD) ||
                (event.isFromSource(InputDevice.SOURCE_DPAD) &&
                    device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC)
            return !device.isVirtual && keyboardSource &&
                !device.supportsSource(InputDevice.SOURCE_GAMEPAD) &&
                !device.supportsSource(InputDevice.SOURCE_JOYSTICK)
        }

        fun getMappings(context: Context): Map<Int, Int> {
            val preferences = getDefaultSharedPreferences(context)
            return buildMap {
                for (binding in KeyboardBinding.entries) {
                    val code = preferences.getInt(binding.preferenceKey, KeyEvent.KEYCODE_UNKNOWN)
                    if (code != KeyEvent.KEYCODE_UNKNOWN) put(code, binding.sdlKeyCode)
                }
            }
        }
    }
}
