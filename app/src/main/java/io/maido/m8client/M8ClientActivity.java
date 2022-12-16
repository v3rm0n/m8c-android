package io.maido.m8client;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.Button;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.maido.m8client.log.CollectLogsTask;
import io.maido.m8client.log.LogcatHelper;

public class M8ClientActivity extends Activity implements CollectLogsTask.OnSendLogsDialogListener {
    private static final String ACTION_USB_PERMISSION =
            "io.maido.m8client.USB_PERMISSION";
    private static final String TAG = "M8ClientActivity";

    private LogcatHelper logcatHelper;
    private boolean isLogging = false;

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
                m8 = null;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        registerReceiver(usbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        M8Util.copyGameControllerDB(this);
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbDevice usbDevice = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
        // Activity was launched by attaching the USB device so permissions are implicitly granted
        if (usbDevice != null) {
            Log.i(TAG, "M8 was attached, launching application");
            connectToM8(usbManager, usbDevice);
        }
        searchForM8();
        setContentView(R.layout.nodevice);
        logcatHelper = new LogcatHelper();
        Button startLogging = findViewById(R.id.logStart);
        startLogging.setOnClickListener(view -> {
            if (isLogging) {
                isLogging = false;
                startLogging.setText(R.string.start_logging);
                new CollectLogsTask(this, this).execute();
                logcatHelper.stop();
            } else {
                isLogging = true;
                logcatHelper.prepareNewLogFile();
                logcatHelper.start(null);
                startLogging.setText(R.string.stop_logging);
            }
        });
    }

    @Override
    public void onShowSendLogsDialog(Pair<String[], String> stringPair) {
        String emailTo = "logs@maido.io";
        String emailSubj = "M8C logs";
        String chooserTitle = "Title";
        List<String> fileNames = new ArrayList<>(Arrays.asList(stringPair.first));
        sendEmail(fileNames, emailTo, emailSubj, chooserTitle, stringPair.second);
    }

    private void sendEmail(List<String> fileNames,
                           String emailTo, String emailSubj, String chooserTitle,
                           String msg) {
        ArrayList<Uri> attachments = new ArrayList<>();
        if (fileNames != null) {
            for (String fileName : fileNames) {
                Uri uri = Uri.parse(this.getString(R.string.uri_content_cache, fileName));
                attachments.add(uri);
            }
        }
        try {
            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setType("plain/text");
            intent.putExtra(Intent.EXTRA_EMAIL,
                    new String[]{emailTo});
            intent.putExtra(Intent.EXTRA_SUBJECT, emailSubj);
            intent.putExtra(Intent.EXTRA_TEXT, msg);
            if (attachments.size() != 0) {
                Log.i(TAG, "add attachment $attachments");
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments);
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooserIntent;
            if (chooserTitle == null) {
                chooserIntent = intent;
            } else {
                chooserIntent = Intent.createChooser(intent, chooserTitle);
                chooserIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            startActivity(chooserIntent);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        unregisterReceiver(usbReceiver);
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
            if (M8Util.isM8(device)) {
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
            startSDLActivity(connection);
        }
    }


    private void startSDLActivity(UsbDeviceConnection connection) {
        Intent sdlActivity = new Intent(this, M8SDLActivity.class);
        sdlActivity.putExtra(M8SDLActivity.FILE_DESCRIPTOR, connection.getFileDescriptor());
        startActivity(sdlActivity);
    }

}
