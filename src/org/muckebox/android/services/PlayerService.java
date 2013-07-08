package org.muckebox.android.services;

import java.util.HashSet;
import java.util.Set;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class PlayerService extends Service {
	private final static String LOG_TAG = "PlayerService";
	
	public final static String EXTRA_TRACK_ID = "track_id";
	
	private enum State {
		STOPPED, 
		PAUSED,
		BUFFERING,
		PLAYING
	};
	
	private final IBinder mBinder = new PlayerBinder();
	
	private State mState = State.STOPPED;
	private Set<PlayerListener> mListeners = new HashSet<PlayerListener>();
	
	@Override
	public void onCreate() {
		Log.d(LOG_TAG, "Service created.");
	}
	
	@Override
	public void onDestroy() {
		Log.d(LOG_TAG, "Service destroyed.");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int trackId = intent.getIntExtra(EXTRA_TRACK_ID, -1);
		mState = State.PLAYING;

		Log.d(LOG_TAG, "Start playing");
		
		for (PlayerListener l: mListeners)
		{
			if (l != null)
			{
				l.onNewTrack(trackId, "Mordsmaessiger Track", 423);
				l.onStartPlaying();
			}
		}
		
		stopSelf(startId);
		
		return Service.START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	public class PlayerBinder extends Binder {
		public PlayerService getService() {
			return PlayerService.this;
		}
	}
	
	// public interface
	
	public void registerListener(PlayerListener listener)
	{
		mListeners.add(listener);
		
		listener.onConnected();
	}

	public void resume() {
		if (mState == State.PAUSED)
		{
			mState = State.PLAYING;
			
			for (PlayerListener l: mListeners)
				if (l != null) l.onPlayResumed();
			
			Log.d(LOG_TAG, "Resuming");
		}
	}
	
	public void pause() {
		if (mState == State.PLAYING)
		{
			mState = State.PAUSED;
			
			for (PlayerListener l: mListeners)
				if (l != null) l.onPlayPaused();
			
			Log.d(LOG_TAG, "Paused");
		}
	}
	
	public void previous() {
		if (mState == State.PAUSED || mState == State.PLAYING)
		{
			for (PlayerListener l: mListeners)
				if (l != null) l.onNewTrack(23,  "Cooler Track", 523);
			
			Log.d(LOG_TAG, "Previous track");
		}
	}
	
	public void next() {
		if (mState == State.PAUSED || mState == State.PLAYING)
		{
			for (PlayerListener l: mListeners)
				if (l != null) l.onNewTrack(42, "Naechster Track", 325);
			
			Log.d(LOG_TAG, "Next track");
		}
	}

	public boolean isPlaying() {
		return mState == State.PLAYING;
	}
	
	public Integer getCurrentTrackLength() {
		return null;
	}
	
	public Integer getCurrentPlayPosition() {
		return null;
	}
	
	public String getCurrentTrackTitle() {
		return null;
	}
}
