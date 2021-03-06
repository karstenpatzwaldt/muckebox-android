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
import org.muckebox.android.ui.utils.NavigationListener;
import org.muckebox.android.ui.widgets.SearchableListFragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class AlbumListFragment extends SearchableListFragment
	implements LoaderManager.LoaderCallbacks<Cursor> {
	SimpleCursorAdapter mAdapter;
	
	private long mArtistId = -1;
	private String mTitle;
	private boolean mIsLatest = false;
	
	private static final String STATE_ARTIST_ID = "artist_id";
	private static final String STATE_TITLE = "title";
	private static final String STATE_ISLATEST = "islatest";

	public static AlbumListFragment newInstanceFromArtist(long artist_id, String title) {
		AlbumListFragment f = new AlbumListFragment();
		
		f.mArtistId = artist_id;
		f.mTitle = title;
		
		return f;
	}
	
	public static AlbumListFragment newInstanceLatest() {
	    AlbumListFragment f = new AlbumListFragment();
	    
	    f.mIsLatest = true;
	    
	    return f;
	}

	public boolean hasArtistId() {
		return mArtistId != -1;
	}
	
	public boolean isLatest() {
	    return mIsLatest;
	}
	
	@Override
	protected boolean isSearchable() {
	    return ! hasArtistId();
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
	    super.onCreateView(inflater, container, savedInstanceState);
	    
        if (savedInstanceState != null) {
            mArtistId = savedInstanceState.getLong(STATE_ARTIST_ID);
            mTitle = savedInstanceState.getString(STATE_TITLE);
            mIsLatest = savedInstanceState.getBoolean(STATE_ISLATEST);
        }
        
        return inflater.inflate(R.layout.fragment_album_browse, container, false);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);

	    outState.putLong(STATE_ARTIST_ID, mArtistId);
	    outState.putString(STATE_TITLE, mTitle);
	    outState.putBoolean(STATE_ISLATEST, mIsLatest);
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

        if (hasArtistId())
            getActivity().setTitle(mTitle);
        else if (isLatest())
            getActivity().setTitle(R.string.title_latest);
        else
            getActivity().setTitle(R.string.title_albums);
    }

    @Override
    protected void onRefreshRequested() {
  		new RefreshArtistsAlbumsTask().setCallbacks(this).execute();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
    	Cursor c = (Cursor) l.getItemAtPosition(position);
    	
    	int artist_name_index = c.getColumnIndex(ArtistEntry.ALIAS_NAME);
    	int album_title_index = c.getColumnIndex(AlbumEntry.ALIAS_TITLE);
    	
    	String artist_name = c.getString(artist_name_index);
    	String album_title = c.getString(album_title_index);
    	
    	String title = artist_name + " - " + album_title;
    	
    	NavigationListener parent = (NavigationListener) getActivity();
    	
    	parent.onAlbumSelected(id,  title);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri;
        String sortOrder = null;
        
        if (hasArtistId()) {
        	baseUri = Uri.withAppendedPath(
        	    MuckeboxProvider.URI_ALBUMS_WITH_ARTIST_ARTIST,
        	    Long.toString(mArtistId));
        } else if (hasFilter()) {
            baseUri = Uri.withAppendedPath(
                MuckeboxProvider.URI_ALBUMS_WITH_ARTIST_TITLE, getFilter());
        } else {
            baseUri = MuckeboxProvider.URI_ALBUMS_WITH_ARTIST;
        }
        
        if (isLatest()) {
            sortOrder = AlbumEntry.SORT_ORDER_CREATED;
        }

        return new CursorLoader(getActivity(), baseUri, null, null, null, sortOrder);
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