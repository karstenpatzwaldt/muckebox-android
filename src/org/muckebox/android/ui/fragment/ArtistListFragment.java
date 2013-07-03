package org.muckebox.android.ui.fragment;

import org.muckebox.android.R;
import org.muckebox.android.db.Provider;
import org.muckebox.android.db.MuckeboxContract.AlbumEntry;
import org.muckebox.android.db.MuckeboxContract.ArtistEntry;
import org.muckebox.android.net.RefreshTask;
import org.muckebox.android.net.RefreshArtistsTask;
import org.muckebox.android.ui.widgets.ImageViewRotater;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
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

public class ArtistListFragment extends ListFragment
	implements OnQueryTextListener, OnCloseListener,
	LoaderManager.LoaderCallbacks<Cursor>,
	RefreshTask.Callbacks {
	private final static String LOG_TAG = "ArtistListFragment";
	
	SimpleCursorAdapter mAdapter;
	SearchView mSearchView;
	String mCurFilter;
	MenuItem mRefreshItem;
	static boolean mListLoaded = false;
	
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

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	super.onCreateOptionsMenu(menu, inflater);
    	
        MenuItem item = menu.add("Search");
        item.setIcon(R.drawable.action_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
                | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        
        mSearchView = new MySearchView(getActivity());
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);
        mSearchView.setIconifiedByDefault(true);
        
        item.setActionView(mSearchView);
        
        mRefreshItem = menu.add("Refresh");
        mRefreshItem.setIcon(R.drawable.navigation_refresh);
        mRefreshItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        
        if (! mListLoaded) {
        	new RefreshArtistsTask().setCallbacks(this).execute();
        	mListLoaded = true;
        }
    }
    
    @Override
    public void onDestroyOptionsMenu()
    {
    	super.onDestroyOptionsMenu();
    	
		onRefreshFinished(false);
		
    	mRefreshItem = null;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == mRefreshItem)
    	{
    		Log.d(LOG_TAG, "Refreshing");
    		
    		new RefreshArtistsTask().setCallbacks(this).execute();
    		return true;
    	}
    	
    	return false;
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

    @Override public boolean onQueryTextSubmit(String query) {
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
            baseUri = Uri.parse(Provider.ARTIST_NAME_BASE + mCurFilter);
        } else {
            baseUri = Provider.URI_ARTISTS;
        }

        return new CursorLoader(getActivity(), baseUri,
                ArtistEntry.PROJECTION, null, null,
                ArtistEntry.SORT_ORDER);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
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
		if (mRefreshItem != null && mRefreshItem.getActionView() != null)
		{
			mRefreshItem.getActionView().clearAnimation();
			mRefreshItem.setActionView(null);
		}
	}
}