package io.maido.m8client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import org.libsdl.app.SDLActivity;

import java.util.HashSet;
import java.util.Set;

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
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    public native void loop();

    @Override
    protected void onStart() {
        registerReceiver(finishReceiver, new IntentFilter(FINISH));
        int fileDescriptor = getIntent().getIntExtra(FILE_DESCRIPTOR, -1);
        Log.d(TAG, "Setting file descriptor to " + fileDescriptor);
        setFileDescriptor(fileDescriptor);
        new Thread(() -> {
            while (true) {
                loop();
            }
        }).start();
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    native public void sendClickEvent(char event);

    @Override
    protected void onStop() {
        unregisterReceiver(finishReceiver);
        super.onStop();
    }

    @Override
    protected String[] getLibraries() {
        return new String[]{
                "main"
        };
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
        up.setOnTouchListener(new ButtonTouchListener(M8Keys.UP));
        View down = findViewById(R.id.down);
        down.setOnTouchListener(new ButtonTouchListener(M8Keys.DOWN));
        View left = findViewById(R.id.left);
        left.setOnTouchListener(new ButtonTouchListener(M8Keys.LEFT));
        View right = findViewById(R.id.right);
        right.setOnTouchListener(new ButtonTouchListener(M8Keys.RIGHT));

        View play = findViewById(R.id.play);
        play.setOnTouchListener(new ButtonTouchListener(M8Keys.PLAY));
        View shift = findViewById(R.id.shift);
        shift.setOnTouchListener(new ButtonTouchListener(M8Keys.SHIFT));
        View option = findViewById(R.id.option);
        option.setOnTouchListener(new ButtonTouchListener(M8Keys.OPTION));
        View edit = findViewById(R.id.edit);
        edit.setOnTouchListener(new ButtonTouchListener(M8Keys.EDIT));
    }

    private static final Set<M8Keys> modifiers = new HashSet<>();

    class ButtonTouchListener implements View.OnTouchListener {

        private final M8Keys key;

        ButtonTouchListener(M8Keys key) {
            this.key = key;
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            int action = motionEvent.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    modifiers.add(key);
                    char code = key.getCode(modifiers);
                    Log.d(TAG, "Sending " + key + " as " + code);
                    sendClickEvent(code);
                    break;
                case MotionEvent.ACTION_UP:
                    modifiers.remove(key);
                    Log.d(TAG, "Key up " + key);
                    sendClickEvent((char) 0);
                    view.performClick();
                    break;
            }
            return true;
        }
    }

    public native void setFileDescriptor(int fileDescriptor);
}
