package io.maido.m8client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

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
                disconnect();
                finish();
            }
        }
    };

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        registerReceiver(finishReceiver, new IntentFilter(FINISH));
        int fileDescriptor = getIntent().getIntExtra(FILE_DESCRIPTOR, -1);
        Log.d(TAG, "Setting file descriptor to " + fileDescriptor);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        int audioDeviceId = 0;
        for (AudioDeviceInfo device : devices) {
            if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                Log.d(TAG, "Speaker device id: " + device.getId());
                audioDeviceId = device.getId();
            }
        }
        connect(fileDescriptor, audioDeviceId);
        new Thread(() -> {
            while (true) {
                loop();
            }
        }).start();
        super.onStart();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(finishReceiver);
        super.onStop();
    }

    @Override
    public void setContentView(View view) {
        FrameLayout mainLayout = new FrameLayout(this);
        mainLayout.addView(view);
        getLayoutInflater().inflate(R.layout.m8layout, mainLayout, true);
        super.setContentView(mainLayout);
        addListeners();
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

    private void addListeners() {
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

    @Override
    protected String getMainFunction() {
        return "android_main";
    }

    public native void connect(int fileDescriptor, int audioDeviceId);

    public native void disconnect();

    public native void loop();
}
