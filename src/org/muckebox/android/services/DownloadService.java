package org.muckebox.android.services;

import java.util.HashSet;
import java.util.Set;

import org.muckebox.android.Muckebox;
import org.muckebox.android.R;
import org.muckebox.android.db.DownloadEntryCursor;
import org.muckebox.android.db.MuckeboxProvider;
import org.muckebox.android.db.MuckeboxContract.CacheEntry;
import org.muckebox.android.db.MuckeboxContract.DownloadEntry;
import org.muckebox.android.ui.activity.DownloadListActivity;
import org.muckebox.android.utils.Preferences;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Binder;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

public class DownloadService
	extends Service
	implements Handler.Callback {
	public final static String EXTRA_TRACK_ID 		= "track_id";
	public final static String EXTRA_TRACK_ID_ARRAY	= "track_id_array";
	public final static String EXTRA_PIN 			= "pin";
	public final static String EXTRA_START_NOW		= "start_now";
	public final static String EXTRA_COMMAND 		= "command";
	
	public static final int COMMAND_DOWNLOAD 		= 1;
	public static final int COMMAND_CHECK_QUEUE 	= 2;
	public static final int COMMAND_CLEAR 			= 3;
	public static final int COMMAND_DISCARD 		= 4;
	
	private static final int NOTIFICATION_ID = 1;
	
	private final static String LOG_TAG = "DownloadService";
	private final IBinder mBinder = new DownloadBinder();
	
	private final Set<DownloadListener> mListeners = new HashSet<DownloadListener>();
	
	private class DownloadHandle {
		public Thread mThread = null;
		public int mTrackId;
		public Uri mUri;
		public boolean mStopping = false;
	}
	
	private DownloadHandle mCurrentDownload = null;
	
	private Handler mMyHandler = null;
	
	private HandlerThread mHelperThread = null;
	private Handler mHelperHandler = null;
	
	private NotificationManager mNotificationManager;
	private Notification.Builder mNotificationBuilder;
	
	private long mLastTotal;
	private long mLastTime;

	public void registerListener(DownloadListener listener)
	{
		mListeners.add(listener);
	}
	
	public void removeListener(DownloadListener listener)
	{
		if (mListeners.contains(listener))
			mListeners.remove(listener);
	}
	
	public class DownloadBinder extends Binder {
		public DownloadService getService() {
			return DownloadService.this;
		}
	}
	
	@Override
	public void onCreate() {
		setupNotifications();
		
		mMyHandler = new Handler(this);
		
		mHelperThread = new HandlerThread("DownloadServiceHelper");
		mHelperThread.start();
		
		mHelperHandler = new Handler(mHelperThread.getLooper());
	}
	
	@Override
	public void onDestroy() {
		mNotificationManager.cancel(NOTIFICATION_ID);
		mHelperThread.quit();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(final Intent intent, int flags, final int startId)
	{
		int command;
		
		if (intent != null)
			command = intent.getIntExtra(EXTRA_COMMAND, COMMAND_DOWNLOAD);
		else
			command = COMMAND_CHECK_QUEUE;
		
		switch (command)
		{
		case COMMAND_DISCARD:
			final int discardTrackId = intent.getIntExtra(EXTRA_TRACK_ID, -1);
			final Uri discardUri = Uri.withAppendedPath(MuckeboxProvider.URI_DOWNLOADS_TRACK, 
					Integer.toString(discardTrackId));
			
			mHelperHandler.post(new Runnable() {
				public void run() {
					removeFromCache(discardTrackId);
					
					int rowsAffected = getContentResolver().delete(discardUri, null, null);
					
					if (mCurrentDownload != null) {
						if (rowsAffected > 0)
							updateNotificationCount();
								
						if (mCurrentDownload.mTrackId == discardTrackId)
							stopCurrentDownload();
					}
				}
			});
			
			return Service.START_NOT_STICKY;
			
		case COMMAND_CLEAR:
			mHelperHandler.post(new Runnable() {
				@Override
				public void run() {
					getContentResolver().delete(
							MuckeboxProvider.URI_DOWNLOADS, null, null);
					
					stopCurrentDownload();
					stopSelf();
				}
			});
			
			return Service.START_NOT_STICKY;

		case COMMAND_DOWNLOAD:
			final int trackId = intent.getIntExtra(EXTRA_TRACK_ID, 0);
			final boolean doPin = intent.getBooleanExtra(EXTRA_PIN, false);
			final boolean startNow = intent.getBooleanExtra(EXTRA_START_NOW, false);

			mHelperHandler.post(new Runnable() {
				@Override
				public void run() {
					if (isInCache(trackId))
					{
						pinInCache(trackId);
					} else
					{
						if (startNow)
							stopCurrentDownload();
	
						addToQueue(trackId, doPin);
						downloadNextOrStop();
					}
				}
			});
			
			return Service.START_STICKY;
			
		case COMMAND_CHECK_QUEUE:
			mHelperHandler.post(new Runnable() {
				@Override public void run() {
					downloadNextOrStop();
				}
			});
			
			return Service.START_STICKY;
			
		default:
			Log.e(LOG_TAG, "ERROR: unknow command " + command);
			
			stopSelf();
			
			return Service.START_STICKY;
		}
	}
	
	private boolean isInCache(int trackId)
	{
		return getContentResolver().query(
				MuckeboxProvider.URI_CACHE_TRACK.buildUpon().appendPath(Integer.toString(trackId)).build(),
				null, null, null, null, null).getCount() > 0;
	}
	
	private void pinInCache(int trackId)
	{
		ContentValues values = new ContentValues();
		
		values.put(CacheEntry.SHORT_PINNED, 1);
		
		getContentResolver().update(MuckeboxProvider.URI_CACHE, values,
				CacheEntry.FULL_TRACK_ID + " IS ?",
				new String[] { Integer.toString(trackId) });
	}
	
	private void removeFromCache(int trackId) {
		Uri uri = MuckeboxProvider.URI_CACHE_TRACK.buildUpon().appendPath(Integer.toString(trackId)).build();
		Cursor c = getContentResolver().query(uri, null, null, null, null);

		try
		{
			while (c.moveToNext())
			{
				String fileName = c.getString(c.getColumnIndex(CacheEntry.ALIAS_FILENAME));
				
				deleteFile(fileName);
			}
			
			getContentResolver().delete(uri, null, null);
		} finally
		{
			c.close();
		}
	}

	@Override
	public boolean handleMessage(final Message msg) {
		final Uri currentUri = mCurrentDownload.mUri;
		
		switch (msg.what)
		{
		case DownloadRunnable.MESSAGE_DOWNLOAD_STARTED:
			for (DownloadListener l: mListeners)
				l.onDownloadStarted(msg.arg1, (String) msg.obj);
			
			mHelperHandler.post(new Runnable() {
				@Override
				public void run() {
					ContentValues values = new ContentValues();
					values.put(DownloadEntry.SHORT_STATUS,
							DownloadEntry.STATUS_VALUE_DOWNLOADING);
					getContentResolver().update(currentUri, values, null, null);					
				}
			});

			break;
			
		case DownloadRunnable.MESSAGE_DATA_RECEIVED:
			final DownloadRunnable.Chunk chunk = (DownloadRunnable.Chunk) msg.obj;

			for (DownloadListener l: mListeners)
				l.onDataReceived(msg.arg1, chunk.buffer);
			
			mHelperHandler.post(new Runnable() {
				public void run() {
					updateProgress(chunk.bytesTotal);
				}
			});


			break;
			
		case DownloadRunnable.MESSAGE_DOWNLOAD_FINISHED:
			for (DownloadListener l: mListeners)
				l.onDownloadFinished(msg.arg1);
			
			Toast.makeText(getApplicationContext(),
					getResources().getString(R.string.download_finished), Toast.LENGTH_SHORT).show();
			
			final DownloadRunnable.Result res = (DownloadRunnable.Result) msg.obj;
			
			Log.d(LOG_TAG, "Download finished, moving to cache");
			
			mHelperHandler.post(new Runnable() {
				public void run() {
					moveToCache(currentUri, res);
					onDownloadFinished(null);
				}
			});
			
			break;
			
		case DownloadRunnable.MESSAGE_DOWNLOAD_INTERRUPTED:
			for (DownloadListener l: mListeners)
				l.onDownloadCanceled(msg.arg1);
			
			Log.d(LOG_TAG, "Download interrupted, re-enqueueing");
			
			mHelperHandler.post(new Runnable() {
				public void run() {
					ContentValues values = new ContentValues(1);
					values.put(DownloadEntry.SHORT_STATUS, DownloadEntry.STATUS_VALUE_QUEUED);
					
					getContentResolver().update(currentUri, values, null, null);
					
					onDownloadFinished(null);	
				}
			});
			
			break;
			
		case DownloadRunnable.MESSAGE_DOWNLOAD_FAILED:
			for (DownloadListener l: mListeners)
				l.onDownloadFailed(msg.arg1);
			
			Log.d(LOG_TAG, "Download failed!");
			
			Toast.makeText(getApplicationContext(), 
					getResources().getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
			
			mHelperHandler.post(new Runnable() {
				public void run() {
					getContentResolver().delete(currentUri, null, null);
					
					onDownloadFinished(R.string.download_failed_short);	
				}
			});
			
			break;
			
		default:
			return false;
		}

		return true;
	}
	
	private void addToQueue(long trackId, boolean doPin)
	{
		boolean transcodingEnabled = Preferences.getTranscodingEnabled();
		String transcodingType = Preferences.getTranscodingType();
		String transcodingQuality = Preferences.getTranscodingQuality();
		
		Cursor result = Muckebox.getAppContext().getContentResolver().
				query(Uri.withAppendedPath(MuckeboxProvider.URI_DOWNLOADS_TRACK, Long.toString(trackId)),
						null, null, null, null);
		
		try
		{
			if (result.getCount() == 0)
			{
				ContentValues values = new ContentValues();
				
				values.put(DownloadEntry.SHORT_TRACK_ID, trackId);
				
				values.put(DownloadEntry.SHORT_TRANSCODE, transcodingEnabled ? 1 : 0);
				values.put(DownloadEntry.SHORT_TRANSCODING_TYPE, transcodingType);
				values.put(DownloadEntry.SHORT_TRANSCODING_QUALITY, transcodingQuality);
				
				values.put(DownloadEntry.SHORT_PIN_RESULT, doPin);
				
				Muckebox.getAppContext().getContentResolver().insert(
						MuckeboxProvider.URI_DOWNLOADS, values);
				
				updateNotificationCount();
			}
		} finally
		{
			result.close();
		}
	}

	@SuppressLint("DefaultLocale")
	private String getDownloadPath(DownloadEntryCursor entry)
	{
		if (Preferences.isCachingEnabled() || entry.doPin())
		{
			return String.format("track_%d_%d_%s.%s",
					entry.getTrackId(),
					entry.isTranscodingEnabled() ? 1 : 0,
					entry.getTranscodingQuality(),
					entry.getTranscodingType());
		} else
		{
			return null;
		}
	}
	
	private void moveToCache(Uri queueEntryUri, DownloadRunnable.Result res)
	{
		Log.d(LOG_TAG, "Moving " + queueEntryUri.toString() + " to cache");
		Cursor cursor = getContentResolver().query(queueEntryUri, null, null, null, null);
		
		try
		{
			if (cursor.getCount() > 0)
			{
				DownloadEntryCursor entry = new DownloadEntryCursor(cursor);
				
				ContentValues values = new ContentValues();
				
				values.put(CacheEntry.SHORT_TRACK_ID, res.trackId);
				values.put(CacheEntry.SHORT_FILENAME, res.path);
				values.put(CacheEntry.SHORT_MIME_TYPE, res.mimeType);
				values.put(CacheEntry.SHORT_SIZE, res.size);
				
				values.put(CacheEntry.SHORT_TRANSCODED, res.transcodingEnabled ? 1 : 0);
				values.put(CacheEntry.SHORT_TRANCODING_TYPE, res.transcodingType);
				values.put(CacheEntry.SHORT_TRANSCODING_QUALITY, res.transcodingQuality);
				
				values.put(CacheEntry.SHORT_PINNED, entry.doPin() ? 1 : 0);
				
				getContentResolver().insert(MuckeboxProvider.URI_CACHE, values);
				getContentResolver().delete(queueEntryUri, null, null);
			}
		} finally
		{
			cursor.close();
		}
	} 
	
	private void downloadNextOrStop()
	{
		if (mCurrentDownload == null)
		{
			Log.d(LOG_TAG, "Checking for next entry in queue");
			
			Cursor c = getContentResolver().query(MuckeboxProvider.URI_DOWNLOADS, null,
				DownloadEntry.FULL_STATUS + " IS ?",
				new String[] { Integer.toString(DownloadEntry.STATUS_VALUE_QUEUED) },
				null);
		
			try
			{
				if (c.getCount() > 0)
				{
					startNewDownload(new DownloadEntryCursor(c));
				} else
				{
					Log.d(LOG_TAG, "Nothing found, stopping");
					stopSelf();
				}
			} finally
			{
				c.close();
			}
		}
	}
	
	private void startNewDownload(DownloadEntryCursor entry) {
		mLastTotal = 0;
		mLastTime = System.nanoTime();
	
		resetNotificationProgress();
		
		mCurrentDownload = new DownloadHandle();
		
		mCurrentDownload.mThread = new Thread(new DownloadRunnable(
				entry.getTrackId(), mMyHandler, getDownloadPath(entry)));
		mCurrentDownload.mUri = entry.getUri();
		mCurrentDownload.mTrackId = entry.getTrackId();
		
		mCurrentDownload.mThread.start();
	}
	
	private void stopCurrentDownload() {
		if (mCurrentDownload != null)
		{
			try
			{
				mCurrentDownload.mStopping = true;
				mCurrentDownload.mThread.interrupt();
				mCurrentDownload.mThread.join();
			} catch (InterruptedException e)
			{
				Log.d(LOG_TAG, "SHOULD NOT HAPPEN");
			}
		}
	}
	
	private void onDownloadFinished(Integer stringId) {
		if (stringId == null)
		{
			mNotificationManager.cancel(NOTIFICATION_ID);
		} else
		{
			mNotificationBuilder
				.setProgress(0,  0, false)
				.setContentTitle(getResources().getText(stringId))
				.setContentText("")
				.setOngoing(false);
			mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
		}
		
		mCurrentDownload = null;
		
		downloadNextOrStop();
	}
	
	private void setupNotifications() {
		mNotificationManager =
			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		mNotificationBuilder =
				new Notification.Builder(this).
					setSmallIcon(android.R.drawable.stat_sys_download).
					setLargeIcon(bm).
					setContentTitle("").
					setContentText(Formatter.formatFileSize(getApplicationContext(), 0)).
					setOngoing(true);
		
		Intent notifyIntent = new Intent(this, DownloadListActivity.class);
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pendingNotifyIntent = PendingIntent.getActivity(
				        this,
				        0,
				        notifyIntent,
				        PendingIntent.FLAG_UPDATE_CURRENT);
		
		mNotificationBuilder.setContentIntent(pendingNotifyIntent);
	}
	
	private void resetNotificationProgress() {
		mNotificationBuilder
			.setContentText("0 kB")
			.setProgress(0,  0, true)
			.setOngoing(true);
		mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
		
		updateNotificationCount();
	}
	
	void updateNotificationCount()
	{
		int count = getContentResolver().query(MuckeboxProvider.URI_DOWNLOADS,
				null, null, null, null, null).getCount();
		
		mNotificationBuilder.setContentTitle(
				getResources().getQuantityString(R.plurals.downloading, count, count));
		mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
	}
	
	private void updateProgress(long bytesTotal) {
		if (! mCurrentDownload.mStopping)
		{
			String totalStr = Formatter.formatFileSize(getApplicationContext(), bytesTotal);
			long timeNow = System.nanoTime();
			
			if (timeNow - mLastTime > 1E9)
			{
				int speed = (int) (((float) bytesTotal - (float) mLastTotal) /
						((float) (timeNow - mLastTime) / 1.0E9));
				
				mLastTotal = bytesTotal;
				mLastTime = timeNow;
		
				String speedStr = Formatter.formatFileSize(getApplicationContext(), speed);
				
				mNotificationBuilder.setContentText(totalStr + " (" + speedStr + "/s)");
				mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
				
				ContentValues values = new ContentValues();
				
				values.put(DownloadEntry.SHORT_BYTES_DOWNLOADED, bytesTotal);
				
				getContentResolver().update(mCurrentDownload.mUri, values, null, null);
			}
		}
	}
}
