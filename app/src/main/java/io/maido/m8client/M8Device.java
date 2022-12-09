package io.maido.m8client;

import android.hardware.usb.UsbDevice;

public class M8Device {

    public static boolean isM8(UsbDevice usbDevice) {
        return usbDevice.getProductName().equals("M8");
    }
}
