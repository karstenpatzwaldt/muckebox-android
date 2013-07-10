package org.muckebox.android.net;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.muckebox.android.Muckebox;
import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxContract.AlbumEntry;
import org.muckebox.android.db.MuckeboxProvider;

import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.util.Log;

public class RefreshAlbumsTask extends RefreshTask<Void> {
	private final static String LOG_TAG = "RefreshAlbumsTask";
	
	@Override
	protected Integer doInBackground(Void... nothing)
	{	
		try {
			JSONArray json = NetHelper.callApi("albums");
			ArrayList<ContentProviderOperation> operations = 
					new ArrayList<ContentProviderOperation>(json.length() + 1);
			
			operations.add(
					ContentProviderOperation.newDelete(MuckeboxProvider.URI_ALBUMS).build());
			
			for (int i = 0; i < json.length(); ++i) {
				JSONObject o = json.getJSONObject(i);

				operations.add(
						ContentProviderOperation.newInsert(MuckeboxProvider.URI_ALBUMS).
							withValue(AlbumEntry.SHORT_ID, o.getInt("id")).
							withValue(AlbumEntry.SHORT_TITLE, o.getString("title")).
							withValue(AlbumEntry.SHORT_ARTIST_ID, o.getInt("artist_id")).build());
			}
			
			Muckebox.getAppContext().getContentResolver().applyBatch(
					MuckeboxProvider.AUTHORITY, operations);
	
			Log.d(LOG_TAG, "Got " + json.length() + " albums");
		} catch (IOException e) {
			Log.d(LOG_TAG, "IOException: " + e.getMessage());
			return R.string.error_reload_albums;
		} catch (JSONException e) {
			return R.string.error_json;
		} catch (RemoteException e) {
			e.printStackTrace();
			return R.string.error_reload_albums;
		} catch (OperationApplicationException e) {
			e.printStackTrace();
			return R.string.error_reload_albums;
		}
		
		return null;
	}
}
