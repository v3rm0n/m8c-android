package io.maido.m8client.settings

import android.content.Context
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import io.maido.m8client.M8GamepadButton
import io.maido.m8client.R

class GamepadSettings : PreferenceFragmentCompat() {

    companion object {
        fun getGamepadPreferences(context: Context): Map<M8GamepadButton, String?> {
            val preferences = getDefaultSharedPreferences(context)
            return M8GamepadButton.values().associateWith { btn ->
                preferences.getString(btn.option, null)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        addGamepadPreferences()
    }

    private fun addGamepadPreferences() {
        val category = PreferenceCategory(requireContext())
        category.title = getString(R.string.gamepad_mappings)
        category.summary = getString(R.string.gamepad_mappings_summary)
        preferenceScreen.addPreference(category)
        M8GamepadButton.values().map { btn ->
            ListPreference(requireContext()).also { listPref ->
                listPref.title = btn.name
                listPref.key = btn.option
                listPref.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                val entries =
                    requireContext().resources.getStringArray(R.array.sdl_gamecontroller_buttons)
                listPref.entries = entries
                listPref.entryValues = (entries.indices).map { it.toString() }.toTypedArray()
            }
        }.map(category::addPreference)
    }
}