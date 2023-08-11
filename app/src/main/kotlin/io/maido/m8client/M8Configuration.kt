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

    fun copyConfiguration(configuration: Map<out M8ConfigurationOption, String?>) {
        configuration.forEach(this::set)
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

enum class M8GraphicsOption : M8ConfigurationOption {
    FULLSCREEN,
    USE_GPU,
    IDLE_MS,
    WAIT_FOR_DEVICE,
    WAIT_PACKETS;

    override val option = name.lowercase()

    override val section = "graphics"
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

enum class M8AudioOption : M8ConfigurationOption {
    ENABLED,
    BUFFER_SIZE,
    DEVICE_NAME;

    override val option = "audio_${name.lowercase()}"

    override val section = "audio"
}