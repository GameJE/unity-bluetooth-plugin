package com.zentry.unity_bluetooth_plugin.utils;

import android.os.Handler;
import android.os.Looper;

public class ThreadHelper {
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    public static void runOnMainThreadDelayed(Runnable runnable, long delayMillis) {
        mainHandler.postDelayed(runnable, delayMillis);
    }

    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}
