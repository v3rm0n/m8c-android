package io.maido.m8client;

import android.app.Application;
import android.os.StrictMode;

public class M8Application extends Application {

    public M8Application() {
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build());
        }
    }
}
