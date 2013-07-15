package org.muckebox.android.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxContract.CacheEntry;
import org.muckebox.android.db.MuckeboxContract.PlaylistEntry;
import org.muckebox.android.db.MuckeboxContract.TrackEntry;
import org.muckebox.android.db.MuckeboxProvider;
import org.muckebox.android.db.PlaylistHelper;
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
import android.widget.Toast;

public class PlayerService extends Service
    implements MediaPlayer.OnPreparedListener, 
    MediaPlayer.OnCompletionListener, 
    MediaPlayer.OnErrorListener, 
    MediaPlayer.OnInfoListener,
    DownloadListener {
	private final static String LOG_TAG = "PlayerService";
	
	private final static int NOTIFICATION_ID = 23;
	
	private final static int PREFETCH_INTERVAL = 10;
	
	public final static String EXTRA_PLAYLIST_ITEM_ID = "playlist_item_id";
	
	public class TrackInfo {
	    public int trackId;
	    public int playlistEntryId;
	    
	    public String title;
	    public int duration;
	    
	    public int position;
	    
	    public boolean isStreaming;
	    
	    public boolean hasPrevious;
	    public boolean hasNext;
	    
	    public int nextTrackId;
	    public boolean nextTrackRequested = false;
	}
	
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
	
	private TrackInfo mTrackInfo;
	
	private Timer mTimer;
	
	private FileInputStream mCurrentFile = null;
	
	private class ElapsedTimeTask extends TimerTask {
	    private Runnable mNotifyTask = new Runnable() {
	        public void run() {
	            for (PlayerListener l: mListeners) {
	                if (l != null)
	                    l.onPlayProgress(mTrackInfo.position);
	            }
	        }
	    };
	    
        public void run() {
            if (mState != State.STOPPED)
            {
                if (mTrackInfo != null) {
                    if (mMediaPlayer.isPlaying()) {
                        mTrackInfo.position = getCurrentPlayPosition();
                        
                        if (getCurrentTimeLeft() < PREFETCH_INTERVAL &&
                            mTrackInfo.hasNext &&
                            ! mTrackInfo.nextTrackRequested)
                        {
                            Log.d(LOG_TAG, "Prefetching next track");
                            requestNextTrack(mTrackInfo.nextTrackId);
                            mTrackInfo.nextTrackRequested = true;
                        }
                    }
                }

                mMainHandler.post(mNotifyTask);
            }
        }
	}
	
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
    	    playTrack(intent.getIntExtra(EXTRA_PLAYLIST_ITEM_ID, 0));
	    }
        
		return Service.START_STICKY;
	}
	
	protected void playTrack(final int playlistEntryId) {
	    if (mState != State.STOPPED)
	        stopPlaying();
	    
        mState = State.BUFFERING;
        
        mHelperHandler.post(new Runnable() {
            public void run() {
                int trackId = PlaylistHelper.getTrackId(getApplicationContext(),
                    Uri.withAppendedPath(MuckeboxProvider.URI_PLAYLIST_ENTRY, Integer.toString(playlistEntryId)));
                boolean isStreaming = playTrackFromAnywhere(trackId);
                
                fetchTrackInfo(playlistEntryId, trackId, isStreaming);
            }
        });
	}
	
	private boolean playTrackFromAnywhere(final int trackId) {
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
                
                return false;
            } else {
                mMainHandler.post(new Runnable() {
                    public void run() {
                        playTrackFromStream(trackId);
                    }
                });
                
                return true;
            }
        } finally {
            c.close();
        }
	}
	
	private void fetchTrackInfo(int playlistEntryId, int trackId, boolean isStreaming) {
        Uri playlistEntryUri = Uri.withAppendedPath(
            MuckeboxProvider.URI_PLAYLIST_ENTRY, Integer.toString(playlistEntryId));
        
        mTrackInfo = new TrackInfo();
        
        mTrackInfo.playlistEntryId = playlistEntryId;
        mTrackInfo.trackId = trackId;
        
        mTrackInfo.isStreaming = isStreaming;
        mTrackInfo.position = 0;
        mTrackInfo.hasNext = ! PlaylistHelper.isLast(getApplicationContext(), playlistEntryUri);
        mTrackInfo.hasPrevious = ! PlaylistHelper.isFirst(getApplicationContext(), playlistEntryUri);
        
        if (mTrackInfo.hasNext)
            mTrackInfo.nextTrackId = PlaylistHelper.getNextTrackId(
                getApplicationContext(), playlistEntryUri);
  
        Cursor c = getContentResolver().query(Uri.withAppendedPath(
            MuckeboxProvider.URI_TRACKS, Integer.toString(trackId)), null, null, null, null);
        
        try {
            if (c.moveToFirst())
            {
                mTrackInfo.title =
                    c.getString(c.getColumnIndex(TrackEntry.ALIAS_DISPLAY_ARTIST)) + " - " +
                    c.getString(c.getColumnIndex(TrackEntry.ALIAS_TITLE));
                mTrackInfo.duration = c.getInt(c.getColumnIndex(TrackEntry.ALIAS_LENGTH));
                
                Log.d(LOG_TAG, "Title is " + mTrackInfo.title);
            } else {
                Log.e(LOG_TAG, "Could not fetch track info for " + trackId);
            }
            
            mMainHandler.post(new Runnable() {
                public void run() {
                    for (PlayerListener l: mListeners)
                    {
                        if (l != null)
                        {
                            l.onNewTrack(mTrackInfo);
                        }
                    }
                    
                    mNotificationBuilder.
                        setContentTitle(mTrackInfo.title).
                        setTicker(mTrackInfo.title);
                    
                    startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
                    
                    mTimer = new Timer();
                    mTimer.scheduleAtFixedRate(new ElapsedTimeTask(), 1000, 1000);
                }
            });
        } finally {
            c.close();
        }
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
	
	protected void requestNextTrack(final int trackId) {
        if (mDownloadService == null) {
            Log.d(LOG_TAG, "Download service not bound yet, waiting...");
            
            mMainHandler.postDelayed(new Runnable() {
                public void run() {
                    playTrackFromStream(trackId);
                }
            }, 100);
        } else {
            mDownloadService.startDownload(trackId, false, false);
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
	
	public void stopPlaying() {
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
        
        mTrackInfo = null;
        
        mDownloadService.removeListener(this);
        
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        
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
	    prevNext(false);
	}
	
	public void next() {
	    prevNext(true);
	}
	
	public void prevNext(final boolean isNext) {
	    if (mState == State.PLAYING || mState == State.PAUSED) {
	        mHelperHandler.post(new Runnable() {
	            public void run() {
	                Cursor c = getContentResolver().query(
	                    Uri.withAppendedPath(
	                        (isNext ? MuckeboxProvider.URI_PLAYLIST_AFTER : MuckeboxProvider.URI_PLAYLIST_BEFORE),
	                        Integer.toString(mTrackInfo.playlistEntryId)),
	                        null, null, null, null);

	                try {
	                    if (isNext ? c.moveToFirst() : c.moveToLast()) {
	                        final int nextPlaylistEntryId = c.getInt(c.getColumnIndex(PlaylistEntry.SHORT_ID));
	                        
	                        mMainHandler.post(new Runnable() {
	                            public void run() {
	                                stopPlaying();
	                                playTrack(nextPlaylistEntryId);
	                            }
	                        });
	                    }
	                } finally {
	                    c.close();
	                }

	            }
	        });
	    }

	    Log.d(LOG_TAG, "Skip to " + (isNext ? "next" : "previous") + " track");
	}
	
	public void seek(int targetSeconds) {
	    if (mState == State.PLAYING || mState == State.PAUSED) {
	        mMediaPlayer.seekTo(targetSeconds * 1000);
	    }
	}

	public boolean isPlaying() {
		return mState == State.PLAYING;
	}
	
	public boolean isStopped() {
	    return mState == State.STOPPED;
	}
	
	public Integer getCurrentTrackLength() {
	    if (mTrackInfo != null)
	        return mTrackInfo.duration;
	    
	    return null;
	}
	
	public Integer getCurrentPlayPosition() {
	    if (mState == State.PLAYING || mState == State.PAUSED)
	        return mMediaPlayer.getCurrentPosition() / 1000;
	    
	    return 0;
	}
	
	public Integer getCurrentTimeLeft() {
	    int ret = getCurrentTrackLength() - getCurrentPlayPosition();
	    
	    if (ret < 0) {
	        Log.e(LOG_TAG, "Current time remaining < 0");
	        ret = 0;
	    }
	    
	    return ret;
	}
	
	public String getCurrentTrackTitle() {
	    if (mTrackInfo != null)
	        return mTrackInfo.title;
	    
	    return null;
	}
	
	public TrackInfo getCurrentTrackInfo() {
	    return mTrackInfo;
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
        Toast.makeText(this,
            String.format((String) getText(R.string.error_playback), what, extra),
            Toast.LENGTH_SHORT).show();
        
        stopPlaying();
        
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mTrackInfo != null)
        {
            if (mTrackInfo.hasNext)
                next();
            else
                stopPlaying();
        }
    }

    @Override
    public void onDownloadStarted(long trackId, String mimeType) {
        Log.d(LOG_TAG, "Download started for " + trackId + " (" + mimeType + ")");
        
        assert(mServer == null);
        
        mServer = new DownloadServerRunnable(mimeType);
        mServerThread = new Thread(mServer);
        mServerThread.start();
        
        startStreamMediaPlayer();
    }
    
    private void startStreamMediaPlayer() {
        if (mServer == null) {
            Log.e(LOG_TAG, "HTTP server missing");
            stopPlaying();
            return;
        }
        
        if (! mServer.isReady()) {
            Log.d(LOG_TAG, "HTTP server not ready, retrying");
            
            if (! mServerThread.isAlive())
            {
                Log.e(LOG_TAG, "HTTP server died! Cannot play.");
                stopPlaying();
            } else {
                mMainHandler.postDelayed(new Runnable() {
                    public void run() {
                        startStreamMediaPlayer();
                    }
                }, 50);
            }
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
            mServer.feed(null);
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
