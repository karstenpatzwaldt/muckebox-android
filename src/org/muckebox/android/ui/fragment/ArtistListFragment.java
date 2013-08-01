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
import org.muckebox.android.ui.widgets.LiveSearchView;
import org.muckebox.android.ui.widgets.RefreshableListFragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;

public class ArtistListFragment extends RefreshableListFragment
	implements OnQueryTextListener, OnCloseListener,
	LoaderManager.LoaderCallbacks<Cursor>
{
	private final static String LOG_TAG = "ArtistListFragment";
	
	SimpleCursorAdapter mAdapter;
	SearchView mSearchView;
	String mCurFilter;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_artist_browse, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.list_row_artist, null,
                new String[] { ArtistEntry.ALIAS_NAME, AlbumEntry.ALIAS_TITLE },
                new int[] { R.id.artist_list_row_name, R.id.artist_list_album_titles }, 0);
        setListAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, this);
        
        getActivity().setTitle(R.string.title_artists);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	MenuItem searchItem = menu.findItem(R.id.artist_list_action_search);
    	
    	if (searchItem == null) {
    	    inflater.inflate(R.menu.artist_list, menu);
    	    searchItem = menu.findItem(R.id.artist_list_action_search);
    	}
    	
    	LiveSearchView searchAction =
    			(LiveSearchView) searchItem.getActionView();
    	
        searchAction.setOnQueryTextListener(this);
        searchAction.setOnCloseListener(this);
        
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    protected void onRefreshRequested() {
    	new RefreshArtistsAlbumsTask().setCallbacks(this).execute();
    }
    
    public boolean onQueryTextChange(String newText) {
        String newFilter = !TextUtils.isEmpty(newText) ? newText : null;

        if (mCurFilter == null && newFilter == null) {
            return true;
        }
        if (mCurFilter != null && mCurFilter.equals(newFilter)) {
            return true;
        }
        mCurFilter = newFilter;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onClose() {
        if (!TextUtils.isEmpty(mSearchView.getQuery())) {
            mSearchView.setQuery(null, true);
        }
        
        Log.d(LOG_TAG, "Closing");
        
        return true;
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
        if (mCurFilter != null) {
        	baseUri = MuckeboxProvider.URI_ARTISTS_WITH_ALBUMS_NAME.buildUpon().appendPath(mCurFilter).build();
        } else {
            baseUri = MuckeboxProvider.URI_ARTISTS_WITH_ALBUMS;
        }

        return new CursorLoader(getActivity(), baseUri, null, null, null, null);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        
        if (data.getCount() == 0 && ! wasRefreshedOnce() && mCurFilter == null) {
            onRefreshRequested();
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}