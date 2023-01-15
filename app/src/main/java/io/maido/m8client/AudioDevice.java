package io.maido.m8client;

import android.media.AudioDeviceInfo;

import androidx.annotation.NonNull;

import java.util.Objects;

public class AudioDevice {

    private final String name;
    private final int type;
    private final int deviceId;

    public AudioDevice(AudioDeviceInfo info) {
        this.name = info.getProductName().toString();
        this.type = info.getType();
        this.deviceId = info.getId();
    }

    @NonNull
    @Override
    public String toString() {
        return name + " " + type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioDevice that = (AudioDevice) o;
        return type == that.type && deviceId == that.deviceId && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, deviceId);
    }

    public int getDeviceId() {
        return deviceId;
    }
}
