package org.muckebox.android.ui.fragment;

import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxContract.ArtistEntry;
import org.muckebox.android.db.Provider;
import org.muckebox.android.db.MuckeboxContract.AlbumEntry;
import org.muckebox.android.net.RefreshAlbumsTask;
import org.muckebox.android.net.RefreshTask;
import org.muckebox.android.ui.activity.TrackBrowseActivity;
import org.muckebox.android.ui.widgets.ImageViewRotater;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;

public class AlbumListFragment extends ListFragment
	implements OnQueryTextListener, OnCloseListener,
	LoaderManager.LoaderCallbacks<Cursor>,
	RefreshTask.Callbacks {
	private final static String LOG_TAG = "AlbumListFragment";
	
	SimpleCursorAdapter mAdapter;
	SearchView mSearchView;
	String mCurFilter;
	MenuItem mRefreshItem;
	boolean mListLoaded = false;
	
	public static AlbumListFragment newInstanceFromArtist(long artist_id) {
		AlbumListFragment f = new AlbumListFragment();
		Bundle args = new Bundle();
		
		args.putLong("artist_id", artist_id);
		f.setArguments(args);
		
		return f;
	}
	
	public long getArtistId() {
		Bundle args = getArguments();
		
		if (args == null)
			return -1;
		
		return args.getLong("artist_id", -1);
	}
	
	public boolean hasArtistId() {
		return getArtistId() != -1;
	}
	
	@Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        setEmptyText("No Albums");

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.list_row_album, null,
                new String[] { AlbumEntry.ALIAS_TITLE, ArtistEntry.ALIAS_NAME },
                new int[] { R.id.album_list_row_title, R.id.album_list_row_artist }, 0);
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    public static class MySearchView extends SearchView {
        public MySearchView(Context context) {
            super(context);
        }

        // The normal SearchView doesn't clear its search text when
        // collapsed, so we will do this for it.
        @Override
        public void onActionViewCollapsed() {
            setQuery("", false);
            super.onActionViewCollapsed();
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	if (! hasArtistId())
    	{
	        // Place an action bar item for searching.
	        MenuItem item = menu.add("Search");
	        item.setIcon(android.R.drawable.ic_menu_search);
	        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
	                | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
	        
	        mSearchView = new MySearchView(getActivity());
	        mSearchView.setOnQueryTextListener(this);
	        mSearchView.setOnCloseListener(this);
	        mSearchView.setIconifiedByDefault(true);
	        
	        //Applies white color on searchview text
	        int id = mSearchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
	        TextView textView = (TextView) mSearchView.findViewById(id);
	        textView.setTextColor(Color.WHITE);
	        
	        item.setActionView(mSearchView);
    	}
        
        mRefreshItem = menu.add("Refresh");
        mRefreshItem.setIcon(R.drawable.ic_menu_refresh);
        mRefreshItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        
        if (! mListLoaded) {
        	new RefreshAlbumsTask().setCallbacks(this).execute();
        	mListLoaded = true;
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == mRefreshItem)
    	{
    		new RefreshAlbumsTask().setCallbacks(this).execute();
    		return true;
    	}
    	
    	return false;
    }

    public boolean onQueryTextChange(String newText) {
        // Called when the action bar search text has changed.  Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        // Don't do anything if the filter hasn't actually changed.
        // Prevents restarting the loader when restoring state.
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

    @Override public boolean onQueryTextSubmit(String query) {
        // Don't care about this.
        return true;
    }

    @Override
    public boolean onClose() {
        if (!TextUtils.isEmpty(mSearchView.getQuery())) {
            mSearchView.setQuery(null, true);
        }
        return true;
    }

    @Override public void onListItemClick(ListView l, View v, int position, long id) {
    	Cursor c = (Cursor) l.getItemAtPosition(position);
    	Intent intent = new Intent(getActivity(), TrackBrowseActivity.class);
    	
    	int artist_name_index = c.getColumnIndex(ArtistEntry.ALIAS_NAME);
    	int album_title_index = c.getColumnIndex(AlbumEntry.ALIAS_TITLE);
    	
    	String artist_name = c.getString(artist_name_index);
    	String album_title = c.getString(album_title_index);
    	
    	String title = artist_name + " - " + album_title;
    	
    	Log.d(LOG_TAG, "Opening track list for album " + id + "(" + title + ")");
    	
    	intent.putExtra(TrackBrowseActivity.ALBUM_ID_ARG, id);
    	intent.putExtra(TrackBrowseActivity.TITLE_ARG, title);

    	startActivity(intent);  	
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.
        Uri baseUri;
        if (hasArtistId()) {
        	baseUri = Uri.parse(Provider.ALBUM_ARTIST_BASE + Long.toString(getArtistId()));
        } else if (mCurFilter != null) {
            baseUri = Uri.parse(Provider.ALBUM_TITLE_BASE + mCurFilter);
        } else {
            baseUri = Provider.URI_ALBUMS;
        }

        return new CursorLoader(getActivity(), baseUri,
                AlbumEntry.PROJECTION, null, null,
                AlbumEntry.SORT_ORDER);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);

        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }
    
	@Override
	public void onRefreshStarted() {
		mRefreshItem.setActionView(
				ImageViewRotater.getRotatingImageView(
						getActivity(), R.layout.action_view_refresh));
	}

	@Override
	public void onRefreshFinished(boolean success) {
		mRefreshItem.getActionView().clearAnimation();
		mRefreshItem.setActionView(null);
	}
}