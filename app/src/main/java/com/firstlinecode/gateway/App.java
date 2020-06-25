package com.firstlinecode.gateway;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

public class App extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();
    }

    public static Context getAppContext() {
        return context;
    }
}
