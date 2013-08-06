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

package org.muckebox.android.ui.fragment;

import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxProvider;
import org.muckebox.android.db.MuckeboxContract.AlbumEntry;
import org.muckebox.android.db.MuckeboxContract.ArtistEntry;
import org.muckebox.android.net.RefreshArtistsAlbumsTask;
import org.muckebox.android.ui.widgets.SearchableListFragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ArtistListFragment extends SearchableListFragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private final static String LOG_TAG = "ArtistListFragment";
	
	SimpleCursorAdapter mAdapter;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_artist_browse, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.list_row_artist, null,
                new String[] { ArtistEntry.ALIAS_NAME, AlbumEntry.ALIAS_TITLE },
                new int[] { R.id.artist_list_row_name, R.id.artist_list_album_titles }, 0);
        setListAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, this);
        
        getActivity().setTitle(R.string.title_artists);
    }
    
    protected void onRefreshRequested() {
    	new RefreshArtistsAlbumsTask().setCallbacks(this).execute();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {  
    	Cursor c = (Cursor) l.getItemAtPosition(position);
    	int artist_name_index = c.getColumnIndex(ArtistEntry.ALIAS_NAME);
    	String artist_name = c.getString(artist_name_index);
    	
    	FragmentManager fm = getActivity().getFragmentManager();
    	FragmentTransaction t = fm.beginTransaction();
 
    	t.replace(R.id.fragment_container,
    			AlbumListFragment.newInstanceFromArtist(id, artist_name));
    	t.addToBackStack(null);
    	t.commit();
    	
    	Log.d(LOG_TAG, "Opening album list for artist " + id);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri;
        if (hasFilter()) {
        	baseUri = Uri.withAppendedPath(
        	    MuckeboxProvider.URI_ARTISTS_WITH_ALBUMS_NAME, getFilter());
        } else {
            baseUri = MuckeboxProvider.URI_ARTISTS_WITH_ALBUMS;
        }

        return new CursorLoader(getActivity(), baseUri, null, null, null, null);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        
        if (data.getCount() == 0 && ! wasRefreshedOnce() && ! hasFilter()) {
            onRefreshRequested();
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
    
    @Override
    protected void reload() {
        getLoaderManager().restartLoader(0, null, this);
    }
}