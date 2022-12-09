package io.maido.m8client;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;

import org.libsdl.app.SDLActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class M8Activity extends SDLActivity {
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private static final String TAG = "M8Activity";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                        }
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    private List<String> mainArguments = new ArrayList<>();
    private HashMap<Integer, Integer> connectedDevices = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Log.i(TAG, deviceList.keySet().toString());
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            if (Objects.equals(device.getProductName(), "M8")) {
                mainArguments.add(device.getDeviceName());
                Log.i(TAG, device.toString());
                usbManager.requestPermission(device, permissionIntent);

                UsbDeviceConnection connection = usbManager.openDevice(device);
                // if we make this, kernel driver will be disconnected
                connection.claimInterface(device.getInterface(0), true);
                connection.claimInterface(device.getInterface(1), true);
                Log.d(TAG, "inserting device with id: " + device.getDeviceId() + " and file descriptor: " + connection.getFileDescriptor());
                connectedDevices.put(device.getDeviceId(), connection.getFileDescriptor());
                mainArguments.add(String.valueOf(connection.getFileDescriptor()));
                break;
            }
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String[] getArguments() {
        return mainArguments.toArray(new String[]{});
    }
}
