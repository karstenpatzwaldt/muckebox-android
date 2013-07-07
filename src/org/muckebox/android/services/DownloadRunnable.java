package org.muckebox.android.services;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

import org.muckebox.android.Muckebox;
import org.muckebox.android.net.NetHelper;
import org.muckebox.android.utils.Preferences;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class DownloadRunnable implements Runnable
{
	private static final String LOG_TAG = "DownloadRunnable";
	
	private final static int BUFFER_SIZE = 32 * 1024;

	private boolean mTranscodingEnabled;
	private String mTranscodingType;
	private String mTranscodingQuality;
	
	private long mTrackId;
	
	private Handler mHandler;
	private String mOutputPath;
	
	private FileOutputStream mOutputStream = null;
	
	public static final int MESSAGE_DOWNLOAD_STARTED = 1;
	public static final int MESSAGE_DATA_RECEIVED = 2;
	public static final int MESSAGE_DOWNLOAD_FINISHED = 3;
	public static final int MESSAGE_DOWNLOAD_CANCELED = 4;
	
	DownloadRunnable(long trackId, Handler resultHandler)
	{
		init(trackId, resultHandler, null);
	}
	
	DownloadRunnable(long trackId, Handler resultHandler, String outputPath)
	{
		init(trackId, resultHandler, outputPath);
	}
	
	private void init(long trackId, Handler resultHandler, String outputPath)
	{
		mTranscodingEnabled = Preferences.getTranscodingEnabled();
		mTranscodingType = Preferences.getTranscodingType();
		mTranscodingQuality = Preferences.getTranscodingQuality();
		
		mTrackId = trackId;
		mHandler = resultHandler;
		
		mOutputPath = outputPath;
	}

	private String getDownloadUrl() throws IOException
	{
		String url;
		String idString = Long.toString(mTrackId);
		
		if (mTranscodingEnabled)
		{
			url = NetHelper.getApiUrl(
					"stream",
					idString,
					new String[] { "format", "quality" },
					new String[] { mTranscodingType, mTranscodingQuality });
		} else
		{
			url = NetHelper.getApiUrl("stream", idString, null, null);
		}
		
		return url;
	}
	
	private void handleReceivedData(ByteBuffer data) throws IOException
	{
		if (! isEmpty(data))
		{
			mHandler.sendMessage(mHandler.obtainMessage(
					MESSAGE_DATA_RECEIVED, (int) mTrackId, 0, data));
			
			if (mOutputStream != null)
			{
				mOutputStream.write(data.array(), 0, data.position());
			}
		}
	}
	
	private int bytesRemaining(ByteBuffer buf)
	{
		return buf.capacity() - buf.position();
	}
	
	private boolean isFull(ByteBuffer buf)
	{
		return (buf.position() + 1) >= buf.capacity();
	}
	
	private boolean isEmpty(ByteBuffer buf)
	{
		return buf.position() == 0;
	}
	
	private void increasePosition(ByteBuffer buf, int delta)
	{
		buf.position(buf.position() + delta);
	}
	
	private boolean readIntoBuffer(InputStream is, ByteBuffer buf) throws IOException
	{
		while (! isFull(buf))
		{
			int bytes_read = is.read(buf.array(),
					buf.position(), bytesRemaining(buf));
			
			if (bytes_read == -1)
				return true;
			
			increasePosition(buf, bytes_read);
		}
		
		return false;
	}
	
	public void closeOutputStream()
	{
		if (mOutputStream != null)
		{
			try {
				mOutputStream.close();
			} catch (IOException eInner)
			{
				Log.e(LOG_TAG, "Yo dawg, i heard ya like exceptions!");
				eInner.printStackTrace();
			}
		}
	}
	
	public void run()
	{
		HttpURLConnection conn = null;
			
		try
		{
			String downloadUrl = getDownloadUrl();
			conn = NetHelper.getDefaultConnection(new URL(downloadUrl));
			InputStream is = conn.getInputStream();
			
			Log.d(LOG_TAG, "Downloading from " + downloadUrl);
			
			if (mOutputPath != null)
			{
				Muckebox.getAppContext().openFileOutput(mOutputPath, Context.MODE_PRIVATE);
				
				Log.d(LOG_TAG, "Saving to " + mOutputPath);
			}

			mHandler.sendMessage(mHandler.obtainMessage(
					MESSAGE_DOWNLOAD_STARTED, (int) mTrackId, 0,
					conn.getContentType()));

			while (true)
			{
				ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
				boolean eosReached = readIntoBuffer(is, buf);
				
				handleReceivedData(buf);
				
				if (eosReached)
				{
					Log.v(LOG_TAG, "Download finished!");
					
					mHandler.sendMessage(mHandler.obtainMessage(
						MESSAGE_DOWNLOAD_FINISHED, mTrackId));
					return;
				}
				
				if (Thread.interrupted())
				{
					throw new IOException("interrupted");
				}
			}
		} catch (IOException e)
		{
			Log.d(LOG_TAG, "Error downloading");
			
			closeOutputStream();
			Muckebox.getAppContext().deleteFile(mOutputPath);

			mHandler.sendMessage(mHandler.obtainMessage(
					MESSAGE_DOWNLOAD_CANCELED, mTrackId));
		} finally
		{
			if (conn != null)
				conn.disconnect();
			
			closeOutputStream();
		}
	}
}