package io.maido.m8client;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashSet;
import java.util.Set;


class M8TouchListener implements View.OnTouchListener {

    private static final String TAG = M8TouchListener.class.getSimpleName();

    private static final Set<M8Key> modifiers = new HashSet<>();

    private final M8Key key;

    public M8TouchListener(M8Key key) {
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

    native public void sendClickEvent(char event);
}