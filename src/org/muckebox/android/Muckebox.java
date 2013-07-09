package org.muckebox.android;

import org.muckebox.android.services.DownloadService;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

public class Muckebox extends Application {
    private static Context context;

    public void onCreate(){
        super.onCreate();
        Muckebox.context = getApplicationContext();
        
		Intent intent = new Intent(this, DownloadService.class);
		
		intent.putExtra(DownloadService.EXTRA_COMMAND,
				DownloadService.COMMAND_CHECK_QUEUE);
		
		startService(intent);
    }

    public static Context getAppContext() {
        return Muckebox.context;
    }
}
