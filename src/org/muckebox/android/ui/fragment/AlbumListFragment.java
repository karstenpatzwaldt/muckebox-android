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
import org.muckebox.android.db.MuckeboxContract.ArtistEntry;
import org.muckebox.android.db.MuckeboxProvider;
import org.muckebox.android.db.MuckeboxContract.AlbumEntry;
import org.muckebox.android.net.RefreshArtistsAlbumsTask;
import org.muckebox.android.ui.activity.BrowseActivity;
import org.muckebox.android.ui.widgets.RefreshableListFragment;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;

public class AlbumListFragment extends RefreshableListFragment
	implements OnQueryTextListener, OnCloseListener,
	LoaderManager.LoaderCallbacks<Cursor> {
	SimpleCursorAdapter mAdapter;
	SearchView mSearchView;
	String mCurFilter;
	
	private long mArtistId = -1;
	private String mTitle;
	
	private static final String STATE_ARTIST_ID = "artist_id";
	private static final String STATE_TITLE = "title";
	
	public interface OnAlbumSelectedListener {
	    public void onAlbumSelected(long id, String title);
	}

	public static AlbumListFragment newInstanceFromArtist(long artist_id, String title) {
		AlbumListFragment f = new AlbumListFragment();
		
		f.mArtistId = artist_id;
		f.mTitle = title;
		
		return f;
	}

	public boolean hasArtistId() {
		return mArtistId != -1;
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
	    super.onCreateView(inflater, container, savedInstanceState);
	    
        if (savedInstanceState != null) {
            mArtistId = savedInstanceState.getLong(STATE_ARTIST_ID);
            mTitle = savedInstanceState.getString(STATE_TITLE);
        }
        
        return inflater.inflate(R.layout.fragment_album_browse, container, false);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);

	    outState.putLong(STATE_ARTIST_ID, mArtistId);
	    outState.putString(STATE_TITLE, mTitle);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.list_row_album, null,
                new String[] { AlbumEntry.ALIAS_TITLE, ArtistEntry.ALIAS_NAME },
                new int[] { R.id.album_list_row_title, R.id.album_list_row_artist }, 0);
        setListAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, this);
        
        TextView titleStrip = (TextView) getActivity().findViewById(R.id.album_list_title_strip);
        
        if (hasArtistId())
            titleStrip.setText(mTitle);
        else
            titleStrip.setText(R.string.title_albums);
    }

    public static class MySearchView extends SearchView {
        public MySearchView(Context context) {
            super(context);
        }

        @Override
        public void onActionViewCollapsed() {
            setQuery("", false);
            super.onActionViewCollapsed();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	MenuItem searchItem = menu.findItem(R.id.album_list_action_search);
        
    	if (searchItem == null) {
    	    inflater.inflate(R.menu.album_list, menu);
    	    searchItem = menu.findItem(R.id.album_list_action_search);
    	}
        	
    	if (hasArtistId())
    	{
    		searchItem.setVisible(false);
    	} else
    	{
	        mSearchView = new MySearchView(getActivity());
	        mSearchView.setOnQueryTextListener(this);
	        mSearchView.setOnCloseListener(this);
	        mSearchView.setIconifiedByDefault(true);
	        
	        searchItem.setActionView(mSearchView);
    	}
    	
        super.onCreateOptionsMenu(menu, inflater);
    }
   
    @Override
    public void onDestroyOptionsMenu()
    {
    	if (mSearchView != null)
    		mSearchView.setQuery(null, true);

    	super.onDestroyOptionsMenu();
    }

    @Override
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
    public void onListItemClick(ListView l, View v, int position, long id) {
    	Cursor c = (Cursor) l.getItemAtPosition(position);
    	
    	int artist_name_index = c.getColumnIndex(ArtistEntry.ALIAS_NAME);
    	int album_title_index = c.getColumnIndex(AlbumEntry.ALIAS_TITLE);
    	
    	String artist_name = c.getString(artist_name_index);
    	String album_title = c.getString(album_title_index);
    	
    	String title = artist_name + " - " + album_title;
    	
    	BrowseActivity parent = (BrowseActivity) getActivity();
    	
    	parent.onAlbumSelected(id,  title);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri;
        
        if (hasArtistId()) {
        	baseUri = Uri.withAppendedPath(
        	    MuckeboxProvider.URI_ALBUMS_WITH_ARTIST_ARTIST,
        	    Long.toString(mArtistId));
        } else if (mCurFilter != null) {
            baseUri = Uri.withAppendedPath(
                MuckeboxProvider.URI_ALBUMS_WITH_ARTIST_TITLE, mCurFilter);
        } else {
            baseUri = MuckeboxProvider.URI_ALBUMS_WITH_ARTIST;
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

	@Override
	public boolean onClose() {
        if (!TextUtils.isEmpty(mSearchView.getQuery())) {
            mSearchView.setQuery(null, true);
        }

        return true;
	}
}