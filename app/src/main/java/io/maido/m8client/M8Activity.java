package io.maido.m8client;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class M8Activity extends SDLActivity {
    private static final String ACTION_USB_PERMISSION =
            "io.maido.m8client.USB_PERMISSION";
    private static final String TAG = "M8Activity";

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                            connectToM8(device, usbManager);
                        }
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    @Override
    protected void onStart() {
        copyFile(this, "gamecontrollerdb.txt", "/data/data/io.maido.m8client/files/gamecontrollerdb.txt");
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        UsbDevice usbDevice = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
        // Activity was launched by attaching the USB device so permissions are implicitly granted
        if (usbDevice != null) {
            Log.i(TAG, "M8 was attached, launching application");
            connectToM8(usbDevice, usbManager);
        } else {
            Log.i(TAG, "Searching for an M8 device");
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            for (UsbDevice device : deviceList.values()) {
                if (Objects.equals(device.getProductName(), "M8")) {
                    requestPermissionIfNeeded(usbManager, device);
                    connectToM8(device, usbManager);
                    break;
                }
            }
        }
        super.onStart();
    }

    private boolean copyFile(Context context, String sourceFileName, String destFileName) {
        AssetManager assetManager = context.getAssets();

        File destFile = new File(destFileName);

        File destParentDir = destFile.getParentFile();
        destParentDir.mkdir();

        InputStream in;
        OutputStream out;
        try {
            in = assetManager.open(sourceFileName);
            out = new FileOutputStream(destFile);


            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void requestPermissionIfNeeded(UsbManager usbManager, UsbDevice usbDevice) {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);
        usbManager.requestPermission(usbDevice, permissionIntent);
    }

    private void connectToM8(UsbDevice device, UsbManager usbManager) {
        UsbDeviceConnection connection = usbManager.openDevice(device);
        // if we make this, kernel driver will be disconnected
        connection.claimInterface(device.getInterface(0), true);
        Log.d(TAG, "Setting device with id: " + device.getDeviceId() + " and file descriptor: " + connection.getFileDescriptor());
        setFileDescriptor(connection.getFileDescriptor());
    }

    public native void setFileDescriptor(int fileDescriptor);

    @Override
    protected String[] getLibraries() {
        return new String[]{
                "SDL2",
                "usb-1.0",
                "m8c",
                "main"
        };
    }
}
