package org.muckebox.android.services;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import org.muckebox.android.Muckebox;
import org.muckebox.android.db.DownloadEntryCursor;
import org.muckebox.android.db.MuckeboxProvider;
import org.muckebox.android.db.MuckeboxContract.DownloadEntry;
import org.muckebox.android.utils.Preferences;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Binder;
import android.os.Message;
import android.util.Log;

public class DownloadService
	extends Service
	implements Handler.Callback {
	public final static String EXTRA_TRACK_ID = "track_id";
	public final static String EXTRA_PIN = "pin";
	public final static String EXTRA_START_NOW = "start_now";
	
	private final static String LOG_TAG = "DownloadService";
	private final IBinder mBinder = new DownloadBinder();
	
	private final Set<DownloadListener> mListeners = new HashSet<DownloadListener>();
	
	private Thread mCurrentThread = null;
	private Uri mCurrentUri = null;

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
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(final Intent intent, int flags, final int startId)
	{
		final long trackId = intent.getLongExtra(EXTRA_TRACK_ID, 0);
		final boolean doPin = intent.getBooleanExtra(EXTRA_PIN, false);
		final boolean startNow = intent.getBooleanExtra(EXTRA_START_NOW, false);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (startNow)
				{
					if (mCurrentThread != null)
					{
						try
						{
							mCurrentThread.interrupt();
							mCurrentThread.join();
							mCurrentThread = null;
						} catch (InterruptedException e)
						{
							Log.d(LOG_TAG, "SHOULD NOT HAPPEN");
						}
					}
				}
				
				if (mCurrentThread == null)
				{
					mCurrentUri = getQueueEntryUri(trackId, doPin);
					mCurrentThread = new Thread(
						new DownloadRunnable(trackId,
								new Handler(DownloadService.this),
								getDownloadPath(mCurrentUri)));
					mCurrentThread.start();
				}

			}
		}).start();
		
		return Service.START_STICKY;
	}

	@Override
	public boolean handleMessage(Message msg) {
		for (DownloadListener l: mListeners)
		{
			switch (msg.what)
			{
			case DownloadRunnable.MESSAGE_DOWNLOAD_STARTED:
				l.onDownloadStarted(msg.arg1, (String) msg.obj);
				
				new Thread(new Runnable() {
					@Override
					public void run() {
						ContentValues values = new ContentValues();
						values.put(DownloadEntry.SHORT_STATUS, DownloadEntry.STATUS_VALUE_DOWNLOADING);
						getContentResolver().update(mCurrentUri, values, null, null);					
					}
				}).start();

				break;
			case DownloadRunnable.MESSAGE_DATA_RECEIVED:
				l.onDataReceived(msg.arg1, (ByteBuffer) msg.obj);
				break;
			case DownloadRunnable.MESSAGE_DOWNLOAD_FINISHED:
				l.onDownloadStopped(msg.arg1);
				
				new Thread(new Runnable() {
					@Override
					public void run() {
						getContentResolver().delete(mCurrentUri, null, null);		
					}
				}).start();

				// mark as downloaded
				
				break;
			case DownloadRunnable.MESSAGE_DOWNLOAD_CANCELED:
				l.onDownloadCanceled(msg.arg1);
				
				new Thread(new Runnable() {
					@Override
					public void run() {
						getContentResolver().delete(mCurrentUri, null, null);
					}
				}).start();
				
				break;
			default:
				return false;
			}
		}

		return true;
	}	
	
	Uri getQueueEntryUri(long trackId, boolean doPin) {
		boolean transcodingEnabled = Preferences.getTranscodingEnabled();
		String transcodingType = Preferences.getTranscodingType();
		String transcodingQuality = Preferences.getTranscodingQuality();
		
		String whereString = DownloadEntry.FULL_TRACK_ID + " IS ?";
		String[] whereArgs;
		
		if (! transcodingEnabled)
		{
			whereString += " AND " + DownloadEntry.FULL_TRANSCODE + " IS 0";
			whereArgs = new String[] { Long.toString(trackId) };
		} else
		{
			whereString += " AND " +
				DownloadEntry.FULL_TRANSCODE + " IS 1 AND " +
				DownloadEntry.FULL_TRANSCODING_TYPE + " IS ? AND " +
				DownloadEntry.FULL_TRANSCODING_QUALITY + " IS ?";
			whereArgs = new String[] { 
				Long.toString(trackId), transcodingType, transcodingQuality
			};
		};
		
		Cursor result = Muckebox.getAppContext().getContentResolver().
				query(MuckeboxProvider.URI_DOWNLOADS,
				null, whereString, whereArgs, null);
		
		if (result.getCount() == 0)
		{
			ContentValues values = new ContentValues();
			
			values.put(DownloadEntry.SHORT_TRACK_ID, trackId);
			
			values.put(DownloadEntry.SHORT_TRANSCODE, transcodingEnabled ? 1 : 0);
			values.put(DownloadEntry.SHORT_TRANSCODING_TYPE, transcodingType);
			values.put(DownloadEntry.SHORT_TRANSCODING_QUALITY, transcodingQuality);
			
			values.put(DownloadEntry.SHORT_PIN_RESULT, doPin);
			
			return getContentResolver().insert(MuckeboxProvider.URI_DOWNLOADS, values);
		} else
		{
			int idIndex = result.getColumnIndex(DownloadEntry.SHORT_ID);
			
			return Uri.parse(MuckeboxProvider.DOWNLOAD_ID_BASE +
					Integer.toString(result.getInt(idIndex)));
		}
	}
	
	@SuppressLint("DefaultLocale")
	private String getDownloadPath(Uri queueEntryUri)
	{
		DownloadEntryCursor entry = new DownloadEntryCursor(
				getContentResolver().query(queueEntryUri, null, null, null, null));
		
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
}
