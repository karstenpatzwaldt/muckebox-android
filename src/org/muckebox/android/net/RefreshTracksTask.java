package org.muckebox.android.net;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.muckebox.android.Muckebox;
import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxContract.TrackEntry;
import org.muckebox.android.db.MuckeboxProvider;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

public class RefreshTracksTask extends RefreshTask<Long> {
	private final static String LOG_TAG = "RefreshAlbumTracksTask";
	
	@Override
	protected Integer doInBackground(Long... album_ids)
	{
		try {
			ArrayList<ContentProviderOperation> operations =
					new ArrayList<ContentProviderOperation>(1);
			
			for (int i = 0; i < album_ids.length; ++i) {
				JSONArray json = NetHelper.callApi("tracks", null,
						new String[] { "album" },
						new String[] { album_ids[i].toString() });
				operations.ensureCapacity(operations.size() + json.length() + 1);
				
				operations.add(ContentProviderOperation.newDelete(
						Uri.withAppendedPath(MuckeboxProvider.URI_TRACKS_ALBUM, album_ids[i].toString())).build());

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
					
					operations.add(ContentProviderOperation.newInsert(MuckeboxProvider.URI_TRACKS).
							withValues(values).build());
				}

				Log.d(LOG_TAG, "Got " + json.length() + " Tracks");
			}
			
			Muckebox.getAppContext().getContentResolver().applyBatch(
					MuckeboxProvider.AUTHORITY, operations);
		} catch (IOException e) {
			Log.d(LOG_TAG, "IOException: " + e.getMessage());
			return R.string.error_reload_tracks;
		} catch (JSONException e) {
			return R.string.error_json;
		} catch (RemoteException e) {
			e.printStackTrace();
			return R.string.error_reload_tracks;
		} catch (OperationApplicationException e) {
			e.printStackTrace();
			return R.string.error_reload_tracks;
		} 
		
		return null;
	}
}
