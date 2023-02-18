package io.maido.m8client

import android.content.Context
import android.util.Log
import io.maido.m8client.M8Util.copyFile
import io.maido.m8client.M8Util.openFile
import org.ini4j.Ini
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


class M8Configuration(context: Context) {

    companion object {
        private const val TAG = "M8Configuration"
        private const val CONFIG_FILE_NAME = "config.ini"
    }

    private val configFile = Ini(openFile(context, CONFIG_FILE_NAME))

    private fun set(key: M8ConfigurationOption, value: String?) {
        if (value == null) {
            return
        }
        Log.d(TAG, "Setting ${key.section} ${key.option} to $value")
        configFile.put(key.section, key.option, value)
    }

    fun copyConfiguration(gamepadPreferences: Map<M8GamepadButton, String?>) {
        gamepadPreferences.forEach(this::set)
        ByteArrayOutputStream().use { output ->
            configFile.store(output)
            copyFile(CONFIG_FILE_NAME, ByteArrayInputStream(output.toByteArray()))
        }
    }
}

interface M8ConfigurationOption {
    val option: String
    val section: String
}

enum class M8GamepadButton : M8ConfigurationOption {
    UP,
    LEFT,
    DOWN,
    RIGHT,
    SELECT,
    START,
    OPT,
    EDIT,
    QUIT,
    RESET;

    override val option = "gamepad_${name.lowercase()}"

    override val section = "gamepad"
}