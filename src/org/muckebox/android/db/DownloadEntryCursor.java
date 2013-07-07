package org.muckebox.android.db;

import org.muckebox.android.db.MuckeboxContract.DownloadEntry;

import android.database.Cursor;

public class DownloadEntryCursor {
	Cursor mCursor;
	
	public DownloadEntryCursor(Cursor cursor)
	{
		mCursor = cursor;
	}
	
	public int getTrackId()
	{
		return mCursor.getInt(mCursor.getColumnIndex(DownloadEntry.SHORT_TRACK_ID));
	}
	
	public boolean isTranscodingEnabled()
	{
		return (mCursor.getInt(mCursor.getColumnIndex(DownloadEntry.SHORT_TRANSCODE)) == 0) ?
				false : true;
	}
	
	public String getTranscodingType()
	{
		return mCursor.getString(mCursor.getColumnIndex(DownloadEntry.SHORT_TRANSCODING_TYPE));
	}
	
	public String getTranscodingQuality()
	{
		return mCursor.getString(mCursor.getColumnIndex(DownloadEntry.SHORT_TRANSCODING_QUALITY));
	}
	
	public boolean doPin()
	{
		return (mCursor.getInt(mCursor.getColumnIndex(DownloadEntry.SHORT_PIN_RESULT)) == 0) ?
				false : true;
	}
}
