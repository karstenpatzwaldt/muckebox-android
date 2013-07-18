package org.muckebox.android.utils;

import org.muckebox.android.services.PlayerService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;

public class RemoteControlReceiver extends BroadcastReceiver {
    private final static String LOG_TAG = "RemoteControlReceiver";
    
    public RemoteControlReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        IBinder binder = peekService(context, new Intent(context, PlayerService.class));
        
        if (binder != null) {
            PlayerService.PlayerBinder playerBinder =
                (PlayerService.PlayerBinder) binder;
            PlayerService service = playerBinder.getService();
            
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                KeyEvent keyEvent = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                
                if (keyEvent == null)
                {
                    Log.e(LOG_TAG, "Key event missing!");
                    return;
                }
                
                int keyCode = keyEvent.getKeyCode();

                if (keyEvent.getAction() != KeyEvent.ACTION_UP)
                    return;
                
                switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    if (service.isPlaying()) {
                        service.pause();
                    } else if (service.isPaused()) {
                        service.resume();
                    } else {
                        Log.e(LOG_TAG, "Invalid state, ignoring key event");
                    }
                    
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    service.stop();
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    service.next();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    service.previous();
                    break;
                }
            } else {
                Log.e(LOG_TAG, "Unknown intent: " + intent.getAction());
            }
        } else {
            Log.e(LOG_TAG, "Got media button click, but no player running, ignoring");
        }
    }

}