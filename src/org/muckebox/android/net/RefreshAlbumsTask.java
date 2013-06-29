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
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class RefreshAlbumsTask extends AsyncTask<Void, Void, Integer> {
	private final static String LOG_TAG = "RefreshAlbumsTask";
	
	@Override
	protected Integer doInBackground(Void... nothing)
	{
		try {
			Context c = Muckebox.getAppContext();
			JSONArray json = NetHelper.callApi("albums");
			SQLiteDatabase db = new MuckeboxDbHelper(c).getWritableDatabase();
			
			db.beginTransaction();
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
			db.endTransaction();
			
			Log.d(LOG_TAG, "Got " + json.length() + " albums");
			
			c.getContentResolver().notifyChange(Provider.URI_ALBUMS, null, false);
		} catch (IOException e) {
			Log.d(LOG_TAG, e.getMessage());
			return R.string.error_reload_albums;
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
