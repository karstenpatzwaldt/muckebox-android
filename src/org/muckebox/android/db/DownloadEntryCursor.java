/*   
 * Copyright 2013 Karsten Patzwaldt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.muckebox.android.db;

import org.muckebox.android.db.MuckeboxContract.DownloadEntry;

import android.database.Cursor;
import android.net.Uri;

public class DownloadEntryCursor {
	Cursor mCursor;
	
	public DownloadEntryCursor(Cursor cursor)
	{
		assert(cursor.getCount() > 0);
		
		mCursor = cursor;
		mCursor.moveToFirst();
	}
	
	public int getId()
	{
		return mCursor.getInt(mCursor.getColumnIndex(DownloadEntry.SHORT_ID));
	}
	
	public int getTrackId()
	{
		return mCursor.getInt(mCursor.getColumnIndex(DownloadEntry.ALIAS_TRACK_ID));
	}
	
	public boolean isTranscodingEnabled()
	{
		return (mCursor.getInt(mCursor.getColumnIndex(DownloadEntry.ALIAS_TRANSCODE)) == 0) ?
				false : true;
	}
	
	public String getTranscodingType()
	{
		return mCursor.getString(mCursor.getColumnIndex(DownloadEntry.ALIAS_TRANSCODING_TYPE));
	}
	
	public String getTranscodingQuality()
	{
		return mCursor.getString(mCursor.getColumnIndex(DownloadEntry.ALIAS_TRANSCODING_QUALITY));
	}
	
	public boolean doPin()
	{
		return (mCursor.getInt(mCursor.getColumnIndex(DownloadEntry.ALIAS_PIN_RESULT)) == 0) ?
				false : true;
	}
	
	public Uri getUri() {
		return MuckeboxProvider.URI_DOWNLOADS.buildUpon().appendPath(Integer.toString(getId())).build();
	}
}
