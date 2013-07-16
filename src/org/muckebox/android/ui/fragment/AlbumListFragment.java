package org.muckebox.android.ui.fragment;

import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxContract.ArtistEntry;
import org.muckebox.android.db.MuckeboxProvider;
import org.muckebox.android.db.MuckeboxContract.AlbumEntry;
import org.muckebox.android.net.RefreshAlbumsTask;
import org.muckebox.android.ui.widgets.RefreshableListFragment;

import android.content.Context;
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
import android.widget.TextView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;

public class AlbumListFragment extends RefreshableListFragment
	implements OnQueryTextListener, OnCloseListener,
	LoaderManager.LoaderCallbacks<Cursor> {
	private final static String LOG_TAG = "AlbumListFragment";
	
	private final static String ARTIST_ID_ARG = "artist_id";
	private final static String TITLE_ARG = "title";
	
	SimpleCursorAdapter mAdapter;
	SearchView mSearchView;
	String mCurFilter;

	public static AlbumListFragment newInstanceFromArtist(long artist_id, String title) {
		AlbumListFragment f = new AlbumListFragment();
		Bundle args = new Bundle();
		
		args.putLong(ARTIST_ID_ARG, artist_id);
		args.putString(TITLE_ARG, title);
		f.setArguments(args);
		
		return f;
	}
	
	public long getArtistId() {
		Bundle args = getArguments();
		
		if (args == null)
			return -1;
		
		return args.getLong(ARTIST_ID_ARG, -1);
	}
	
	public boolean hasArtistId() {
		return getArtistId() != -1;
	}
	
	public String getTitle() {
		Bundle args = getArguments();
		
		if (args == null)
			return null;
		
		return args.getString(TITLE_ARG, null);
	}
	
	public boolean hasTitle() {
		return getTitle() != null;
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_album_browse, container, false);
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
        
        if (titleStrip != null)
        {
	        if (hasTitle())
	        	titleStrip.setText(getTitle());
	        else
	        	titleStrip.setVisibility(View.GONE);
        }
        
        if (! RefreshAlbumsTask.wasRunning())
        	onRefreshRequested();
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
    	super.onCreateOptionsMenu(menu, inflater);
    	
    	inflater.inflate(R.menu.album_list, menu);
    	
    	MenuItem searchItem = menu.findItem(R.id.album_list_action_search);
    	
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
  		new RefreshAlbumsTask().setCallbacks(this).execute();
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
    	
    	Log.d(LOG_TAG, "Opening track list for album " + id + "(" + title + ")");
    	
    	TrackListFragment tracklist = TrackListFragment.newInstanceFromAlbum(id, title);
    	FragmentManager fm = getActivity().getFragmentManager();
    	FragmentTransaction t = fm.beginTransaction();
 
    	t.replace(R.id.fragment_container, tracklist);
    	t.addToBackStack(null);
    	t.commit();
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri;
        
        if (hasArtistId()) {
        	baseUri = MuckeboxProvider.URI_ALBUMS_WITH_ARTIST_ARTIST.buildUpon().appendPath(Long.toString(getArtistId())).build();
        } else if (mCurFilter != null) {
            baseUri = MuckeboxProvider.URI_ALBUMS_WITH_ARTIST_TITLE.buildUpon().appendPath(mCurFilter).build();
        } else {
            baseUri = MuckeboxProvider.URI_ALBUMS_WITH_ARTIST;
        }

        return new CursorLoader(getActivity(), baseUri, null, null, null, null);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
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