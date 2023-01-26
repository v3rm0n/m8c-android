package io.maido.m8client

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy

class M8Application : Application() {

    init {
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
    }

}