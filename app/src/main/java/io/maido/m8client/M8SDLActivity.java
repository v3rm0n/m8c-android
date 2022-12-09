package io.maido.m8client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.libsdl.app.SDLActivity;

public class M8SDLActivity extends SDLActivity {
    public static String FINISH = M8SDLActivity.class.getSimpleName() + ".FINISH";
    public static String FILE_DESCRIPTOR = M8SDLActivity.class.getSimpleName() + ".FILE_DESCRIPTOR";

    private static final String TAG = "M8SDLActivity";

    BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(FINISH)) {
                Log.i(TAG, "Finishing SDL activity");
                setFileDescriptor(-1);
                finish();
            }
        }
    };

    @Override
    protected void onStart() {
        registerReceiver(finishReceiver, new IntentFilter(FINISH));
        int fileDescriptor = getIntent().getIntExtra(FILE_DESCRIPTOR, -1);
        Log.d(TAG, "Setting file descriptor to " + fileDescriptor);
        setFileDescriptor(fileDescriptor);
        super.onStart();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(finishReceiver);
        super.onStop();
    }

    @Override
    protected String[] getLibraries() {
        return new String[]{
                "SDL2",
                "usb-1.0",
                "m8c",
                "main"
        };
    }

    public native void setFileDescriptor(int fileDescriptor);
}
