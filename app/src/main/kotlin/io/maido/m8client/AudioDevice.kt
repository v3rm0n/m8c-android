package io.maido.m8client

import android.media.AudioDeviceInfo
import java.util.*

data class AudioDevice(
    private val name: String,
    private val type: Int,
    val deviceId: Int,
) {
    constructor(info: AudioDeviceInfo) : this(info.productName.toString(), info.type, info.id)

    override fun toString() = "$name $type"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as AudioDevice
        return type == that.type && deviceId == that.deviceId && name == that.name
    }

    override fun hashCode() = Objects.hash(name, type, deviceId)
}