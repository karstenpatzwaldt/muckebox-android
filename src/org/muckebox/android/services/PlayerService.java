package org.muckebox.android.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxContract.CacheEntry;
import org.muckebox.android.db.MuckeboxContract.TrackEntry;
import org.muckebox.android.db.MuckeboxProvider;
import org.muckebox.android.net.DownloadServerRunnable;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class PlayerService extends Service
    implements MediaPlayer.OnPreparedListener, 
    MediaPlayer.OnCompletionListener, 
    MediaPlayer.OnErrorListener, 
    MediaPlayer.OnInfoListener,
    DownloadListener {
	private final static String LOG_TAG = "PlayerService";
	
	private final static int NOTIFICATION_ID = 23;
	
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
	
	private MediaPlayer mMediaPlayer;
	
	private DownloadService mDownloadService;
	
	private DownloadServerRunnable mServer;
	private Thread mServerThread;

	private Notification.Builder mNotificationBuilder;
	
	private Handler mMainHandler;
	
	private HandlerThread mHelperThread;
	private Handler mHelperHandler;
	
	private String mCurrentTitle = "";
	private int mCurrentDuration = 0;
	
	private FileInputStream mCurrentFile = null;
	
	@Override
	public void onCreate() {
		Log.d(LOG_TAG, "Service created.");
		
		bindService(new Intent(getBaseContext(), DownloadService.class),
            mConnection, Context.BIND_AUTO_CREATE);
		
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        mNotificationBuilder =
                  new Notification.Builder(this).
                      setSmallIcon(R.drawable.av_play_dark).
                      setLargeIcon(bm).
                      setContentTitle("Playing...").
                      setOngoing(true);
          
        mMediaPlayer = new MediaPlayer();
        
        mMediaPlayer.setWakeMode(getApplicationContext(),
            PowerManager.PARTIAL_WAKE_LOCK);
        
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnInfoListener(this);
        mMediaPlayer.setOnErrorListener(this);
        
        mMainHandler = new Handler(getMainLooper());
        
        mHelperThread = new HandlerThread("PlayerServiceHelper");
        mHelperThread.start();
        
        mHelperHandler = new Handler(mHelperThread.getLooper());
	}
	
	@Override
	public void onDestroy() {
	    if (isPlaying()) {
	        Log.e(LOG_TAG, "Player service still playing when destroyed");
	        stopPlaying();
	    }
	    
	    mHelperThread.quit();
	    
	    if (mMediaPlayer != null)
	        mMediaPlayer.release();
	    
	    getBaseContext().unbindService(mConnection);

		Log.d(LOG_TAG, "Service destroyed.");
	}
	
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            DownloadService.DownloadBinder binder =
                    (DownloadService.DownloadBinder) service;
            mDownloadService = binder.getService();

            Log.d(LOG_TAG, "Bound to download service");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mDownloadService = null;
            
            Log.d(LOG_TAG, "Unbound from download service");
        }
    };

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    if (intent != null)
	    {
    	    playTrack(intent.getIntExtra(EXTRA_TRACK_ID, -1));
    		
            startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
	    }
        
		return Service.START_STICKY;
	}
	
	protected void playTrack(final int trackId) {
	    if (mState != State.STOPPED)
	        stopPlaying();
	    
        mState = State.BUFFERING;
        
        mHelperHandler.post(new Runnable() {
            public void run() {
                Cursor c = getContentResolver().query(Uri.withAppendedPath(
                    MuckeboxProvider.URI_TRACKS, Integer.toString(trackId)), null, null, null, null);
                
                try {
                    if (c.moveToFirst())
                    {
                        mCurrentTitle =
                            c.getString(c.getColumnIndex(TrackEntry.ALIAS_DISPLAY_ARTIST)) + " - " +
                            c.getString(c.getColumnIndex(TrackEntry.ALIAS_TITLE));
                        mCurrentDuration = c.getInt(c.getColumnIndex(TrackEntry.ALIAS_LENGTH));
                        
                        Log.d(LOG_TAG, "Title is " + mCurrentTitle);
                    }
                    
                    mMainHandler.post(new Runnable() {
                        public void run() {
                            for (PlayerListener l: mListeners)
                            {
                                if (l != null)
                                {
                                    l.onNewTrack(trackId, mCurrentTitle, mCurrentDuration);
                                }
                            }
                        }
                    });
                } finally {
                    c.close();
                }
            }
        });
        
        mHelperHandler.post(new Runnable() {
            public void run() {
                Cursor c = getContentResolver().query(
                    Uri.withAppendedPath(MuckeboxProvider.URI_CACHE_TRACK, Integer.toString(trackId)),
                        null, null, null, null);
                
                try {
                    if (c.moveToFirst()) {
                        final String filename = c.getString(c.getColumnIndex(CacheEntry.ALIAS_FILENAME));
                        
                        mMainHandler.post(new Runnable() {
                            public void run() {
                                playTrackFromFile(filename);
                            }
                        });
                    } else {
                        mMainHandler.post(new Runnable() {
                            public void run() {
                                playTrackFromStream(trackId);
                            }
                        });
                    }
                } finally {
                    c.close();
                }
            }
        });
	}
	
	protected void playTrackFromFile(final String filename) {
	    Log.d(LOG_TAG, "Playing from local file " + filename);
	    
	    try {
            mCurrentFile = openFileInput(filename);
            
            mMediaPlayer.reset();
            
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(mCurrentFile.getFD());
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Could not open file");
            
            e.printStackTrace();
            
            // XXX handle error
            stopPlaying();
        }
	}
	
	protected void playTrackFromStream(final int trackId) {
        if (mDownloadService == null) {
            Log.d(LOG_TAG, "Download service not bound yet, waiting...");
            
            mMainHandler.postDelayed(new Runnable() {
                public void run() {
                    playTrackFromStream(trackId);
                }
            }, 100);
        } else {
            Log.d(LOG_TAG, "Start playing streamed track " + trackId);
            
            mDownloadService.registerListener(this, trackId);
            mDownloadService.startDownload(trackId, false, true);
        }
	}
	
	protected void stopPlaying() {
	    mState = State.STOPPED;
	    
	    mMediaPlayer.reset();
	    
        for (PlayerListener l: mListeners)
            if (l != null)
                l.onStopPlaying();
        
        if (mCurrentFile != null) {
            try {
                mCurrentFile.close();
            } catch (IOException e) {
                // WTF?
                e.printStackTrace();
            }
        }
        
        mCurrentFile = null;
        
        if (mServerThread != null)
            mServerThread.interrupt();
        
        mServer = null;
        mServerThread = null;
        
        mCurrentTitle = "";
        mCurrentDuration = 0;
        
        Log.d(LOG_TAG, "Stopped playing");
        
        stopForeground(true);
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
			
			mMediaPlayer.start();
			
			for (PlayerListener l: mListeners)
				if (l != null) l.onPlayResumed();
			
			Log.d(LOG_TAG, "Resuming");
		}
	}
	
	public void pause() {
		if (mState == State.PLAYING)
		{
			mState = State.PAUSED;
			
			mMediaPlayer.pause();
			
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
	    return mCurrentDuration;
	}
	
	public Integer getCurrentPlayPosition() {
	    return mMediaPlayer.getCurrentPosition() / 1000;
	}
	
	public String getCurrentTrackTitle() {
		return mCurrentTitle;
	}

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        
        for (PlayerListener l: mListeners)
            l.onStartPlaying();
        
        mState = State.PLAYING;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        // TODO Add notifications for buffering etc.
        return false;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        stopPlaying();
        
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopPlaying();
    }

    @Override
    public void onDownloadStarted(long trackId, String mimeType) {
        mServer = new DownloadServerRunnable(mimeType);
        mServerThread = new Thread(mServer);
        mServerThread.start();
        
        startStreamMediaPlayer();
    }
    
    private void startStreamMediaPlayer() {
        if (! mServer.isReady()) {
            Log.d(LOG_TAG, "HTTP server not ready, retrying");
            
            mMainHandler.postDelayed(new Runnable() {
                public void run() {
                    startStreamMediaPlayer();
                }
            }, 50);
        } else {
            try
            {
                Log.d(LOG_TAG, "Start playing!");
                
                mMediaPlayer.reset();
                
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(DownloadServerRunnable.getUrl());
                mMediaPlayer.prepareAsync();
            } catch (IOException e) {
                stopPlaying();
            }   
        }
    }

    @Override
    public void onDataReceived(long trackId, ByteBuffer buffer) {
        if (mServer != null)
            mServer.feed(buffer);
    }

    @Override
    public void onDownloadFinished(long trackId) {
        if (mServer != null)
            mServer.done();
    }

    @Override
    public void onDownloadCanceled(long trackId) {
        stopPlaying();
    }

    @Override
    public void onDownloadFailed(long trackId) {
        stopPlaying(); 
    }
}
