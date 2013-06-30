package org.muckebox.android.net;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.muckebox.android.Muckebox;
import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxContract.TrackEntry;
import org.muckebox.android.db.MuckeboxDbHelper;
import org.muckebox.android.db.Provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class RefreshTracksTask extends AsyncTask<Long, Void, Integer> {
	private final static String LOG_TAG = "RefreshAlbumTracksTask";
	
	@Override
	protected Integer doInBackground(Long... album_ids)
	{
		try {
			Context c = Muckebox.getAppContext();
			SQLiteDatabase db = new MuckeboxDbHelper(c).getWritableDatabase();

			for (int i = 0; i < album_ids.length; ++i) {
				JSONArray json = NetHelper.callApi("tracks", "",
						new String[] { "album" },
						new String[] { album_ids[i].toString() });

				db.beginTransaction();
				
				try {
					db.delete(TrackEntry.TABLE_NAME, TrackEntry.FULL_ALBUM_ID + " IS ?",
							new String[] { album_ids[i].toString() });
					
					for (int j = 0; j < json.length(); ++j) {
						JSONObject o = json.getJSONObject(j);
						ContentValues values = new ContentValues();
						
						values.put(TrackEntry.SHORT_ID, o.getInt("id"));
						values.put(TrackEntry.SHORT_ARTIST_ID,  o.getInt("artist_id"));
						values.put(TrackEntry.SHORT_ALBUM_ID, o.getInt("album_id"));
						
						values.put(TrackEntry.SHORT_TITLE, o.getString("title"));
						
						if (! o.isNull("tracknumber"))
							values.put(TrackEntry.SHORT_TRACKNUMBER, o.getInt("tracknumber"));
						
						if (! o.isNull("discnumber"))
							values.put(TrackEntry.SHORT_DISCNUMBER, o.getInt("discnumber"));
						
						values.put(TrackEntry.SHORT_LABEL, o.getString("label"));
						values.put(TrackEntry.SHORT_CATALOGNUMBER, o.getString("catalognumber"));
						
						values.put(TrackEntry.SHORT_LENGTH, o.getInt("length"));
						values.put(TrackEntry.SHORT_DISPLAY_ARTIST, o.getString("displayartist"));
						values.put(TrackEntry.SHORT_DATE, o.getString("date"));
						
						db.insert(TrackEntry.TABLE_NAME, null, values);
					}
		
	
					Log.d(LOG_TAG, "Got " + json.length() + " Tracks");
					db.setTransactionSuccessful();
					
					c.getContentResolver().notifyChange(Provider.URI_TRACKS, null, false);
				} finally {
					db.endTransaction();
				}
			}
		} catch (IOException e) {
			Log.d(LOG_TAG, e.getMessage());
			return R.string.error_reload_tracks;
		} catch (JSONException e) {
			return R.string.error_json;
		} 
		
		return null;
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		if (result != null)
			Toast.makeText(Muckebox.getAppContext(),
					result,
					Toast.LENGTH_SHORT).show();
	}
}
