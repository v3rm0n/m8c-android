package io.maido.m8client;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.usb.UsbDeviceConnection;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import org.libsdl.app.SDLActivity;

public class M8SDLActivity extends SDLActivity {
    private static final String FILE_DESCRIPTOR = M8SDLActivity.class.getSimpleName() + ".FILE_DESCRIPTOR";

    private static final String TAG = "M8SDLActivity";

    public static void startM8SDLActivity(Context context, UsbDeviceConnection connection) {
        Intent sdlActivity = new Intent(context, M8SDLActivity.class);
        sdlActivity.putExtra(M8SDLActivity.FILE_DESCRIPTOR, connection.getFileDescriptor());
        context.startActivity(sdlActivity);
    }

    @Override
    protected void onStart() {
        hideButtonsOnPortrait(getResources().getConfiguration().orientation);
        int fileDescriptor = getIntent().getIntExtra(FILE_DESCRIPTOR, -1);
        int audioDeviceId = getBuiltInSpeakerId();
        Log.d(TAG, "Setting file descriptor to " + fileDescriptor + " and audio device to " + audioDeviceId);
        connect(fileDescriptor, audioDeviceId);
        new Thread(() -> {
            while (true) {
                loop();
            }
        }).start();
        super.onStart();
    }

    private int getBuiltInSpeakerId() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        int audioDeviceId = 0;
        for (AudioDeviceInfo device : devices) {
            if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                Log.d(TAG, "Speaker device id: " + device.getId() + " type: " + device.getType() + " is sink: " + device.isSink());
                audioDeviceId = device.getId();
            }
        }
        return audioDeviceId;
    }

    @Override
    public void setContentView(View view) {
        FrameLayout mainLayout = new FrameLayout(this);
        mainLayout.addView(view);
        getLayoutInflater().inflate(R.layout.m8layout, mainLayout, true);
        super.setContentView(mainLayout);
        setButtonListeners();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        hideButtonsOnPortrait(newConfig.orientation);
    }

    private void hideButtonsOnPortrait(int currentOrientation) {
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            View buttons = findViewById(R.id.buttons);
            buttons.setVisibility(View.INVISIBLE);
        }
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            View buttons = findViewById(R.id.buttons);
            buttons.setVisibility(View.VISIBLE);
        }
    }

    private void setButtonListeners() {
        View up = findViewById(R.id.up);
        up.setOnTouchListener(new M8TouchListener(M8Key.UP));
        View down = findViewById(R.id.down);
        down.setOnTouchListener(new M8TouchListener(M8Key.DOWN));
        View left = findViewById(R.id.left);
        left.setOnTouchListener(new M8TouchListener(M8Key.LEFT));
        View right = findViewById(R.id.right);
        right.setOnTouchListener(new M8TouchListener(M8Key.RIGHT));

        View play = findViewById(R.id.play);
        play.setOnTouchListener(new M8TouchListener(M8Key.PLAY));
        View shift = findViewById(R.id.shift);
        shift.setOnTouchListener(new M8TouchListener(M8Key.SHIFT));
        View option = findViewById(R.id.option);
        option.setOnTouchListener(new M8TouchListener(M8Key.OPTION));
        View edit = findViewById(R.id.edit);
        edit.setOnTouchListener(new M8TouchListener(M8Key.EDIT));
    }

    @Override
    protected String getMainFunction() {
        return "android_main";
    }

    public native void connect(int fileDescriptor, int audioDeviceId);

    public native void loop();
}
