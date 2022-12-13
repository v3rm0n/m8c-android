package io.maido.m8client;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import org.libsdl.app.SDLActivity;

import java.util.HashMap;

public class M8Activity extends SDLActivity {

    private static final String ACTION_USB_PERMISSION =
            "io.maido.m8client.USB_PERMISSION";

    private static final String TAG = M8Activity.class.getSimpleName();

    private UsbDevice m8 = null;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null && M8Util.isM8(device)) {
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
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (M8Util.isM8(device)) {
                    m8 = null;
                    View buttons = findViewById(R.id.buttons);
                    buttons.setVisibility(View.INVISIBLE);
                }
            }
        }
    };

    private void connectToM8(UsbManager usbManager, UsbDevice usbDevice) {
        UsbDeviceConnection connection = usbManager.openDevice(usbDevice);
        if (connection != null) {
            Log.d(TAG, "Setting device with id: " + usbDevice.getDeviceId() + " and file descriptor: " + connection.getFileDescriptor());
            connect(connection.getFileDescriptor());
            View buttons = findViewById(R.id.buttons);
            buttons.setVisibility(View.VISIBLE);
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

    private void searchForM8() {
        if (m8 != null) {
            Log.i(TAG, "M8 already found, skipping");
            return;
        }
        Log.i(TAG, "Searching for an M8 device");
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (UsbDevice device : deviceList.values()) {
            if (M8Util.isM8(device)) {
                m8 = device;
                connectToM8WithPermission(usbManager, device);
                break;
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(usbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        M8Util.copyConfigurationFiles(this);
        new Thread(() -> {
            while (true) {
                usbEventLoop();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(usbReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        searchForM8();
    }

    @Override
    public void setContentView(View view) {
        FrameLayout mainLayout = new FrameLayout(this);
        mainLayout.addView(view);
        getLayoutInflater().inflate(R.layout.m8layout, mainLayout, true);
        super.setContentView(mainLayout);
        addButtonListeners();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            View buttons = findViewById(R.id.buttons);
            buttons.setVisibility(View.INVISIBLE);
        }
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            View buttons = findViewById(R.id.buttons);
            buttons.setVisibility(View.VISIBLE);
        }
        super.onConfigurationChanged(newConfig);
    }

    private void addButtonListeners() {
        View up = findViewById(R.id.up);
        up.setOnTouchListener(new M8TouchListener(M8Keys.UP));
        View down = findViewById(R.id.down);
        down.setOnTouchListener(new M8TouchListener(M8Keys.DOWN));
        View left = findViewById(R.id.left);
        left.setOnTouchListener(new M8TouchListener(M8Keys.LEFT));
        View right = findViewById(R.id.right);
        right.setOnTouchListener(new M8TouchListener(M8Keys.RIGHT));

        View play = findViewById(R.id.play);
        play.setOnTouchListener(new M8TouchListener(M8Keys.PLAY));
        View shift = findViewById(R.id.shift);
        shift.setOnTouchListener(new M8TouchListener(M8Keys.SHIFT));
        View option = findViewById(R.id.option);
        option.setOnTouchListener(new M8TouchListener(M8Keys.OPTION));
        View edit = findViewById(R.id.edit);
        edit.setOnTouchListener(new M8TouchListener(M8Keys.EDIT));
    }

    public native void connect(int fileDescriptor);

    public native void usbEventLoop();

}
