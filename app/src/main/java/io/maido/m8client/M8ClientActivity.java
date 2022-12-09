package io.maido.m8client;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class M8ClientActivity extends Activity {
    private static final String ACTION_USB_PERMISSION =
            "io.maido.m8client.USB_PERMISSION";
    private static final String TAG = "M8ClientActivity";

    private UsbDevice m8 = null;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null && M8Device.isM8(device)) {
                            connectToM8(device, usbManager);
                        } else {
                            Log.d(TAG, "Device was not M8");
                        }
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.d(TAG, "Device was detached!");
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && M8Device.isM8(device)) {
                    Log.i(TAG, "Device disconnected");
                    Intent finishActivity = new Intent(M8SDLActivity.FINISH);
                    sendBroadcast(finishActivity);
                    m8 = null;
                } else {
                    Log.d(TAG, "Device was not M8");
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        registerReceiver(usbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        copyFile(this, "gamecontrollerdb.txt", "/data/data/io.maido.m8client/files/gamecontrollerdb.txt");
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbDevice usbDevice = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
        // Activity was launched by attaching the USB device so permissions are implicitly granted
        if (usbDevice != null) {
            Log.i(TAG, "M8 was attached, launching application");
            connectToM8(usbDevice, usbManager);
        }
        if (m8 == null) {
            searchForM8();
        }
        super.onCreate(savedInstanceState);
    }


    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        if (m8 == null) {
            searchForM8();
        }
        super.onResume();
    }

    private void searchForM8() {
        Log.i(TAG, "Searching for an M8 device");
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (UsbDevice device : deviceList.values()) {
            if (M8Device.isM8(device)) {
                requestPermissionIfNeeded(usbManager, device);
                break;
            }
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
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
        if (usbManager.hasPermission(usbDevice)) {
            Log.i(TAG, "Permission granted!");
            connectToM8(usbDevice, usbManager);
        } else {
            Log.i(TAG, "Requesting USB device permission");
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            registerReceiver(usbReceiver, filter);
            usbManager.requestPermission(usbDevice, permissionIntent);
        }
    }

    private void connectToM8(UsbDevice device, UsbManager usbManager) {
        UsbDeviceConnection connection = usbManager.openDevice(device);
        if (connection != null) {
            // if we make this, kernel driver will be disconnected
            connection.claimInterface(device.getInterface(0), true);
            Log.d(TAG, "Setting device with id: " + device.getDeviceId() + " and file descriptor: " + connection.getFileDescriptor());
            m8 = device;
            Intent sdlActivity = new Intent(this, M8SDLActivity.class);
            sdlActivity.putExtra(M8SDLActivity.FILE_DESCRIPTOR, connection.getFileDescriptor());
            startActivity(sdlActivity);
        }
    }

}
