package org.muckebox.android.net;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.muckebox.android.Muckebox;
import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxContract.AlbumEntry;
import org.muckebox.android.db.MuckeboxDbHelper;
import org.muckebox.android.db.Provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class RefreshAlbumsTask extends RefreshTask<Void> {
	private final static String LOG_TAG = "RefreshAlbumsTask";
	
	@Override
	protected Integer doInBackground(Void... nothing)
	{
		Context c = Muckebox.getAppContext();
		SQLiteDatabase db = new MuckeboxDbHelper(c).getWritableDatabase();
				
		try {
			JSONArray json = NetHelper.callApi("albums");
			
			db.beginTransaction();
			
			try {
				db.delete(AlbumEntry.TABLE_NAME, null, null);
				
				for (int i = 0; i < json.length(); ++i) {
					JSONObject o = json.getJSONObject(i);
					ContentValues values = new ContentValues();
					
					values.put(AlbumEntry.SHORT_ID, o.getInt("id"));
					values.put(AlbumEntry.SHORT_TITLE, o.getString("title"));
					values.put(AlbumEntry.SHORT_ARTIST_ID, o.getInt("artist_id"));
					
					db.insert(AlbumEntry.TABLE_NAME, null, values);
				}
	
				db.setTransactionSuccessful();
				
				Log.d(LOG_TAG, "Got " + json.length() + " albums");
				
				c.getContentResolver().notifyChange(Provider.URI_ALBUMS, null, false);
			} finally {
				db.endTransaction();				
			}
		} catch (IOException e) {
			Log.d(LOG_TAG, "IOException: " + e.getMessage());
			return R.string.error_reload_albums;
		} catch (JSONException e) {
			return R.string.error_json;
		}
		
		return null;
	}
}
