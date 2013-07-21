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

package org.muckebox.android.net;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.muckebox.android.Muckebox;
import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxContract.ArtistEntry;
import org.muckebox.android.db.MuckeboxProvider;

import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.util.Log;

public class RefreshArtistsTask extends RefreshTask<Void> {
	private final static String LOG_TAG = "RefreshArtistsTask";
	
	@Override
	protected Integer doInBackground(Void... nothing)
	{
		try {
			JSONArray json = NetHelper.callApi("artists");
			ArrayList<ContentProviderOperation> operations =
					new ArrayList<ContentProviderOperation>(json.length() + 1);
			
			operations.add(ContentProviderOperation.newDelete(MuckeboxProvider.URI_ARTISTS).build());
			
			for (int i = 0; i < json.length(); ++i) {
				JSONObject o = json.getJSONObject(i);
				operations.add(ContentProviderOperation.newInsert(MuckeboxProvider.URI_ARTISTS).
						withValue(ArtistEntry.SHORT_ID, o.getInt("id")).
						withValue(ArtistEntry.SHORT_NAME, o.getString("name")).build());
			}
			
			Muckebox.getAppContext().getContentResolver().applyBatch(
					MuckeboxProvider.AUTHORITY, operations);
		} catch (IOException e) {
			Log.d(LOG_TAG, "IOException: " + e.getMessage());
			return R.string.error_reload_artists;
		} catch (JSONException e) {
			return R.string.error_json;
		} catch (RemoteException e) {
			e.printStackTrace();
			return R.string.error_reload_artists;
		} catch (OperationApplicationException e) {
			e.printStackTrace();
			return R.string.error_reload_artists;
		}
		
		return null;
	}
}
