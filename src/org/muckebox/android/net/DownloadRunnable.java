package org.muckebox.android.net;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;

import org.muckebox.android.Muckebox;
import org.muckebox.android.utils.BufferUtils;
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
	
	private int mBytesTotal = 0;
	
	private FileOutputStream mOutputStream = null;
	
	public static final int MESSAGE_DOWNLOAD_STARTED = 1;
	public static final int MESSAGE_DATA_RECEIVED = 2;
	public static final int MESSAGE_DOWNLOAD_FINISHED = 3;
	public static final int MESSAGE_DOWNLOAD_FAILED = 4;
	public static final int MESSAGE_DOWNLOAD_INTERRUPTED = 5;
	
	public static class FileInfo {
	    public String mimeType;
	    public String path;
	}

	public static class Result {
		public long trackId;
		
		public String path;
		public String mimeType;
		public int size;
		
		public boolean transcodingEnabled;
		public String transcodingType;
		public String transcodingQuality;
	}
	
	public static class Chunk {
		public long bytesTotal;
		public ByteBuffer buffer;
	}
	
	DownloadRunnable(long trackId, Handler resultHandler)
	{
		init(trackId, resultHandler, null);
	}
	
	public DownloadRunnable(long trackId, Handler resultHandler, String outputPath)
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
	
	private int handleReceivedData(ByteBuffer data) throws IOException
	{
		if (! BufferUtils.isEmpty(data))
		{
			Log.d(LOG_TAG, "Sending message for " + data.position() + " bytes");
			
			Chunk chunk = new Chunk();
			
			chunk.bytesTotal = mBytesTotal;
			chunk.buffer = data;
			
			if (mHandler != null)
				mHandler.sendMessage(mHandler.obtainMessage(
						MESSAGE_DATA_RECEIVED, (int) mTrackId, 0, chunk));
			
			if (mOutputStream != null)
			{
				mOutputStream.write(data.array(), 0, data.position());
			}
			
			return data.position();
		}
		
		return 0;
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
	
	private Result makeResult(String mimeType, int bytesTotal) {
		Result res = new Result();
		
		res.trackId = mTrackId;
		
		res.path = mOutputPath;
		res.mimeType = mimeType;
		res.size = bytesTotal;
		
		res.transcodingEnabled = mTranscodingEnabled;
		res.transcodingType = mTranscodingType;
		res.transcodingQuality = mTranscodingQuality;
		
		return res;
	}
	
	private FileInfo makeFileInfo(String mimeType, String path) {
	    FileInfo ret = new FileInfo();
	    
	    ret.mimeType = mimeType;
	    ret.path = path;
	    
	    return ret;
	}
	
	public void run()
	{
		HttpURLConnection conn = null;
			
		try
		{
			String downloadUrl = getDownloadUrl();
			conn = NetHelper.getDefaultConnection(new URL(downloadUrl));
			String mimeType = conn.getContentType();
			InputStream is = conn.getInputStream();
			
			Log.d(LOG_TAG, "Downloading from " + downloadUrl);
			
			if (mOutputPath != null)
			{
				Muckebox.getAppContext().openFileOutput(mOutputPath, Context.MODE_PRIVATE);
				
				Log.d(LOG_TAG, "Saving to " + mOutputPath);
			}
			
			FileInfo fileInfo = makeFileInfo(mimeType, mOutputPath);

			if (mHandler != null)
				mHandler.sendMessage(mHandler.obtainMessage(
						MESSAGE_DOWNLOAD_STARTED, (int) mTrackId, 0, fileInfo));

			while (true)
			{
				ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
				boolean eosReached = BufferUtils.readIntoBuffer(is, buf);
				
				mBytesTotal += handleReceivedData(buf);
				
				if (eosReached)
				{
					Log.v(LOG_TAG, "Download finished!");
					
					Result res = makeResult(mimeType, mBytesTotal);
					
					if (mHandler != null)
						mHandler.sendMessage(mHandler.obtainMessage(
							MESSAGE_DOWNLOAD_FINISHED, (int) mTrackId, 0, res));
					
					return;
				}
				
				if (Thread.interrupted())
				{
					throw new ClosedByInterruptException();
				}
			}
		} catch (ClosedByInterruptException e)
		{
			handleFailure(MESSAGE_DOWNLOAD_INTERRUPTED);
		} catch (IOException e)
		{
			Log.d(LOG_TAG, "Error downloading");
			e.printStackTrace();

			handleFailure(MESSAGE_DOWNLOAD_FAILED);
		} finally
		{
			if (conn != null)
				conn.disconnect();
			
			closeOutputStream();
		}
	}
	
	void handleFailure(int messageType) {
		closeOutputStream();
		Muckebox.getAppContext().deleteFile(mOutputPath);

		if (mHandler != null)
			mHandler.sendMessage(mHandler.obtainMessage(
					messageType, mTrackId));
	}
}
