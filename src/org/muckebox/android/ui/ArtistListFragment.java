package org.muckebox.android.ui;

import org.muckebox.android.Muckebox;
import org.muckebox.android.R;
import org.muckebox.android.db.Provider;
import org.muckebox.android.db.MuckeboxContract.ArtistEntry;
import org.muckebox.android.net.RefreshArtistsTask;
import org.muckebox.android.ui.ArtistAlbumBrowseActivity;

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

public class ArtistListFragment extends ListFragment
	implements OnQueryTextListener, OnCloseListener,
	LoaderManager.LoaderCallbacks<Cursor> {
	private final static String LOG_TAG = "ArtistListFragment";
	
	SimpleCursorAdapter mAdapter;
	SearchView mSearchView;
	String mCurFilter;
	MenuItem mRefreshItem;
	static boolean mListLoaded = false;
	
	@Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        setEmptyText("No artists");

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_1, null,
                new String[] { ArtistEntry.COLUMN_NAME_NAME },
                new int[] { android.R.id.text1 }, 0);
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
        
        if (! mListLoaded) {
        	new RefreshArtistsTask().execute();
        	mListLoaded = true;
        }
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
        
        mRefreshItem = menu.add("Refresh");
        mRefreshItem.setIcon(R.drawable.ic_menu_refresh);
        mRefreshItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == mRefreshItem)
    	{
    		new RefreshArtistsTask().execute();
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
    	Cursor c = Muckebox.getAppContext().getContentResolver().query(
    			Uri.parse(Provider.ARTIST_ID_BASE + id),
    			ArtistEntry.PROJECTION,
    			ArtistEntry._ID + "IS ?",
    			new String[] { String.valueOf(id) },
    			null);

    	int name_index = c.getColumnIndex(ArtistEntry.COLUMN_NAME_NAME);
    	
    	while (c.moveToNext()) {
        	Intent intent = new Intent(getActivity(), ArtistAlbumBrowseActivity.class);
        	
        	String name = c.getString(name_index);
        	
        	Log.d(LOG_TAG, "Opening album list for artist " + id + "(" + name + ")");
        	
        	intent.putExtra("artist_id", id);
        	intent.putExtra("artist_name", name);

        	startActivity(intent);  		
    	}
    	
    	c.close();
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.
        Uri baseUri;
        if (mCurFilter != null) {
            baseUri = Uri.parse(Provider.ARTIST_NAME_BASE + mCurFilter);
        } else {
            baseUri = Provider.URI_ARTISTS;
        }

        return new CursorLoader(getActivity(), baseUri,
                ArtistEntry.PROJECTION, null, null,
                ArtistEntry.SORT_ORDER);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);

        // The list should now be shown.
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
}