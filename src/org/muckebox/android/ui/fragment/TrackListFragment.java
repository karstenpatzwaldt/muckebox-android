package org.muckebox.android.ui.fragment;

import java.util.concurrent.TimeUnit;

import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxContract.TrackEntry;
import org.muckebox.android.db.Provider;
import org.muckebox.android.net.RefreshTracksTask;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SearchView.OnCloseListener;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class TrackListFragment extends ListFragment
	implements OnCloseListener,
	LoaderManager.LoaderCallbacks<Cursor> {
	
	private static final String ALBUM_ID_ARG = "album_id";
	
	SimpleCursorAdapter mAdapter;
	MenuItem mRefreshItem;
	boolean mListLoaded = false;
	
	private class DurationViewBinder implements ViewBinder {
        @SuppressLint("DefaultLocale")
		public boolean setViewValue(View view, Cursor cursor, int columnIndex)
        {
			if (columnIndex == cursor.getColumnIndex(TrackEntry.ALIAS_LENGTH))
			{
				TextView textview = (TextView) view;
				String text;

				long duration = cursor.getInt(columnIndex);
				long hours = TimeUnit.SECONDS.toHours(duration);
				long minutes = TimeUnit.SECONDS.toMinutes(duration - hours * 60);
				long seconds = duration - ((hours * 60) + minutes) * 60;
				
				if (hours > 0)
					text = String.format("%d:%02d:%02d", hours, minutes, seconds);
				else
					text = String.format("%d:%02d", minutes, seconds);
				
				textview.setText(text);
				
				return true;
    		}
			
			return false;
    	}
	}
	
	public static TrackListFragment newInstanceFromAlbum(long artist_id) {
		TrackListFragment f = new TrackListFragment();
		Bundle args = new Bundle();
		
		args.putLong(ALBUM_ID_ARG, artist_id);
		f.setArguments(args);
		
		return f;
	}
	
	public long getAlbumId() {
		Bundle args = getArguments();
		
		if (args == null)
			return -1;
		
		return args.getLong(ALBUM_ID_ARG, -1);
	}
	
	public boolean hasAlbumId() {
		return getAlbumId() != -1;
	}
	
	@Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        setEmptyText("No Tracks");

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.list_row_track, null,
                new String[] {
        			TrackEntry.ALIAS_TITLE,
        			TrackEntry.ALIAS_DISPLAY_ARTIST,
        			TrackEntry.ALIAS_LENGTH
        			},
                new int[] {
        			R.id.track_list_title,
        			R.id.track_list_artist,
        			R.id.track_list_duration
        			}, 0);
        
        mAdapter.setViewBinder(new DurationViewBinder());
 
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
        
        if (! mListLoaded) {
        	new RefreshTracksTask().execute(getAlbumId());
        	mListLoaded = true;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mRefreshItem = menu.add("Refresh");
        mRefreshItem.setIcon(R.drawable.ic_menu_refresh);
        mRefreshItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == mRefreshItem)
    	{
    		new RefreshTracksTask().execute(getAlbumId());
    		return true;
    	}
    	
    	return false;
    }

    @Override
    public boolean onClose() {
        return true;
    }

    @Override public void onListItemClick(ListView l, View v, int position, long id) {
        // Insert desired behavior here.
        Log.i("FragmentComplexList", "Item clicked: " + id);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.
        Uri baseUri;
        if (hasAlbumId()) {
        	baseUri = Uri.parse(Provider.TRACK_ALBUM_BASE + Long.toString(getAlbumId()));
        } else {
            baseUri = Provider.URI_TRACKS;
        }

        return new CursorLoader(getActivity(), baseUri,
                TrackEntry.PROJECTION, null, null,
                TrackEntry.SORT_ORDER);
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