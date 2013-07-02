package org.muckebox.android.net;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.muckebox.android.Muckebox;
import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxContract.ArtistEntry;
import org.muckebox.android.db.MuckeboxDbHelper;
import org.muckebox.android.db.Provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class RefreshArtistsTask extends RefreshTask<Void> {
	private final static String LOG_TAG = "RefreshArtistsTask";
	
	@Override
	protected Integer doInBackground(Void... nothing)
	{

		try {
			Context c = Muckebox.getAppContext();
			SQLiteDatabase db = new MuckeboxDbHelper(c).getWritableDatabase();
			JSONArray json = NetHelper.callApi("artists");
			
			db.beginTransaction();
			
			try {
				db.delete(ArtistEntry.TABLE_NAME, null, null);
				
				for (int i = 0; i < json.length(); ++i) {
					JSONObject o = json.getJSONObject(i);
					ContentValues values = new ContentValues();
					
					values.put(ArtistEntry.SHORT_ID, o.getInt("id"));
					values.put(ArtistEntry.SHORT_NAME, o.getString("name"));
					
					db.insert(ArtistEntry.TABLE_NAME, null, values);
				}
	
				db.setTransactionSuccessful();
				
				Log.d(LOG_TAG, "Got " + json.length() + " artists");
				
				c.getContentResolver().notifyChange(Provider.URI_ARTISTS, null, false);
			}  finally
			{
				db.endTransaction();
			}
		} catch (IOException e) {
			Log.d(LOG_TAG, e.getMessage());
			return R.string.error_reload_artists;
		} catch (JSONException e) {
			return R.string.error_json;
		}
		
		return null;
	}
}
