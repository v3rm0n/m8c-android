package io.maido.m8client;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import au.id.jms.usbaudio.AudioPlayback;
import au.id.jms.usbaudio.UsbAudio;

public class M8ClientActivity extends Activity {
    private static final String ACTION_USB_PERMISSION =
            "io.maido.m8client.USB_PERMISSION";
    private static final String TAG = "M8ClientActivity";

    private UsbDevice m8 = null;

    UsbAudio mUsbAudio = null;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null && M8Device.isM8(device)) {
                            connectToM8(usbManager, device);
                        } else {
                            Log.d(TAG, "Device was not M8");
                        }
                    } else {
                        Log.d(TAG, "Permission denied for device " + device);
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.d(TAG, "Device was detached!");
                stopM8SDLActivity(intent);
            }
        }
    };

    private void stopM8SDLActivity(Intent intent) {
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        registerReceiver(usbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        M8Device.copyGameControllerDB(this);
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbDevice usbDevice = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
        // Activity was launched by attaching the USB device so permissions are implicitly granted
        if (usbDevice != null) {
            Log.i(TAG, "M8 was attached, launching application");
            connectToM8(usbManager, usbDevice);
        }
        searchForM8();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        unregisterReceiver(usbReceiver);
        if (mUsbAudio != null) {
            mUsbAudio.stop();
            mUsbAudio.close();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        searchForM8();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void searchForM8() {
        if (m8 != null) {
            Log.i(TAG, "M8 already found, skipping");
            return;
        }
        Log.i(TAG, "Searching for an M8 device");
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (UsbDevice device : deviceList.values()) {
            if (M8Device.isM8(device)) {
                connectToM8WithPermission(usbManager, device);
                break;
            }
        }
    }

    private void connectToM8WithPermission(UsbManager usbManager, UsbDevice usbDevice) {
        if (usbManager.hasPermission(usbDevice)) {
            Log.i(TAG, "Permission granted!");
            connectToM8(usbManager, usbDevice);
        } else {
            Log.i(TAG, "Requesting USB device permission");
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            registerReceiver(usbReceiver, filter);
            usbManager.requestPermission(usbDevice, permissionIntent);
        }
    }

    private void connectToM8(UsbManager usbManager, UsbDevice usbDevice) {
        UsbDeviceConnection connection = usbManager.openDevice(usbDevice);
        if (connection != null) {
            Log.d(TAG, "Setting device with id: " + usbDevice.getDeviceId() + " and file descriptor: " + connection.getFileDescriptor());
            m8 = usbDevice;
            startAudio(connection);
            startSDLActivity(connection);
        }
    }

    private void startAudio(UsbDeviceConnection connection) {
        mUsbAudio = new UsbAudio();
        AudioPlayback.setup();

        mUsbAudio.setup(connection.getFileDescriptor());

        new Thread(() -> {
            while (true) {
                mUsbAudio.loop();
            }
        }).start();
    }

    private void startSDLActivity(UsbDeviceConnection connection) {
        Intent sdlActivity = new Intent(this, M8SDLActivity.class);
        sdlActivity.putExtra(M8SDLActivity.FILE_DESCRIPTOR, connection.getFileDescriptor());
        startActivity(sdlActivity);
    }

}
