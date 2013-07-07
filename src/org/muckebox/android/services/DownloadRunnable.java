package org.muckebox.android.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

import org.muckebox.android.net.NetHelper;
import org.muckebox.android.utils.Preferences;

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
	
	public static final int MESSAGE_DOWNLOAD_STARTED = 1;
	public static final int MESSAGE_DATA_RECEIVED = 2;
	public static final int MESSAGE_DOWNLOAD_FINISHED = 3;
	public static final int MESSAGE_DOWNLOAD_CANCELED = 4;
	
	DownloadRunnable(long trackId, Handler resultHandler)
	{
		mTranscodingEnabled = Preferences.getTranscodingEnabled();
		mTranscodingType = Preferences.getTranscodingType();
		mTranscodingQuality = Preferences.getTranscodingQuality();
		
		mTrackId = trackId;
		mHandler = resultHandler;
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
	
	private void handleReceivedData(ByteBuffer data)
	{
		if (! isEmpty(data))
		{
			mHandler.sendMessage(mHandler.obtainMessage(
					MESSAGE_DATA_RECEIVED, (int) mTrackId, 0, data));
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
	
	public void run()
	{
		HttpURLConnection conn = null;

		mHandler.sendMessage(mHandler.obtainMessage(
				MESSAGE_DOWNLOAD_STARTED, mTrackId));
						
		try
		{
			conn = NetHelper.getDefaultConnection(new URL(getDownloadUrl()));
			InputStream is = conn.getInputStream();

			while (true)
			{
				ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
				boolean eosReached = readIntoBuffer(is, buf);
				
				handleReceivedData(buf);
				
				if (eosReached)
				{
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

			mHandler.sendMessage(mHandler.obtainMessage(
					MESSAGE_DOWNLOAD_CANCELED, mTrackId));
		} finally
		{
			if (conn != null)
				conn.disconnect();
		}
	}
}
