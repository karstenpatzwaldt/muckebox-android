package org.muckebox.android;

import android.app.Application;
import android.content.Context;

public class Muckebox extends Application {
    private static Context context;

    public void onCreate(){
        super.onCreate();
        Muckebox.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return Muckebox.context;
    }
}
