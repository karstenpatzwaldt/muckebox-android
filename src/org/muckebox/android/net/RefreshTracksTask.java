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

public class RefreshTracksTask extends AsyncTask<Integer, Void, Integer> {
	private final static String LOG_TAG = "RefreshAlbumTracksTask";
	
	@Override
	protected Integer doInBackground(Integer... album_ids)
	{
		try {
			Context c = Muckebox.getAppContext();
			
			for (int i = 0; i < album_ids.length; ++i) {
				JSONArray json = NetHelper.callApi("tracks", "",
						new String[] { "album" },
						new String[] { album_ids[i].toString() });
				SQLiteDatabase db = new MuckeboxDbHelper(c).getWritableDatabase();
				
				db.beginTransaction();
				db.delete(TrackEntry.TABLE_NAME, null, null);
				
				for (int j = 0; j < json.length(); ++j) {
					JSONObject o = json.getJSONObject(j);
					ContentValues values = new ContentValues();
					
					values.put(TrackEntry._ID, o.getInt("id"));
					values.put(TrackEntry.COLUMN_NAME_ARTIST_ID,  o.getInt("artist_id"));
					values.put(TrackEntry.COLUMN_NAME_ALBUM_ID, o.getInt("album_id"));
					
					values.put(TrackEntry.COLUMN_NAME_TITLE, o.getString("title"));
					
					values.put(TrackEntry.COLUMN_NAME_TRACKNUMBER, o.getInt("tracknumber"));
					values.put(TrackEntry.COLUMN_NAME_DISCNUMBER, o.getInt("discnumber"));
					
					values.put(TrackEntry.COLUMN_NAME_LABEL, o.getString("label"));
					values.put(TrackEntry.COLUMN_NAME_CATALOGNUMBER, o.getString("catalognumber"));
					
					values.put(TrackEntry.COLUMN_NAME_LENGTH, o.getInt("length"));
					values.put(TrackEntry.COLUMN_NAME_DISPLAY_ARTIST, o.getString("displayartist"));
					values.put(TrackEntry.COLUMN_NAME_DATE, o.getString("date"));
					
					db.insert(TrackEntry.TABLE_NAME, null, values);
				}
	
				db.setTransactionSuccessful();
				db.endTransaction();
				
				Log.d(LOG_TAG, "Got " + json.length() + " Tracks");
				
				c.getContentResolver().notifyChange(Provider.URI_TRACKS, null, false);
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
