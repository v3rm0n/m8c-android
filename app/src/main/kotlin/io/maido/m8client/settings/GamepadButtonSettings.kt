package io.maido.m8client.settings

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import androidx.preference.*
import io.maido.m8client.M8GamepadButton
import io.maido.m8client.R

class GamepadButtonSettings : PreferenceFragmentCompat() {

    companion object {
        fun getGamepadPreferences(context: Context): Map<M8GamepadButton, String?> {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
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
        M8GamepadButton.values().forEach { btn ->
            val listPref = ListPreference(requireContext())
            listPref.title = btn.name
            listPref.key = btn.option
            listPref.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            val entries =
                requireContext().resources.getStringArray(R.array.sdl_gamecontroller_buttons)
            listPref.entries = entries
            listPref.entryValues = (entries.indices).map { it.toString() }.toTypedArray()
            category.addPreference(listPref)
        }
    }
}