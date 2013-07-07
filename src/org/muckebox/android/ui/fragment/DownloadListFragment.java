package org.muckebox.android.ui.fragment;

import java.util.Random;

import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxProvider;
import org.muckebox.android.db.MuckeboxContract.DownloadEntry;
import org.muckebox.android.services.DownloadService;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

public class DownloadListFragment extends ListFragment
	implements LoaderManager.LoaderCallbacks<Cursor> {

	SimpleCursorAdapter mAdapter;
	
	MenuItem mAddButton;
	MenuItem mClearButton;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_downloads, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.list_row_download, null,
                new String[] { DownloadEntry.ALIAS_TRACK_ID, DownloadEntry.ALIAS_STATUS },
                new int[] { R.id.download_list_row_title, R.id.download_list_row_status }, 0);
        setListAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, this);
	}
	
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	super.onCreateOptionsMenu(menu, inflater);
    	
    	inflater.inflate(R.menu.download_list, menu);
    	
    	mAddButton = menu.findItem(R.id.download_list_action_add);
    	mClearButton = menu.findItem(R.id.download_list_action_clear);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == mAddButton)
    	{
    		Intent intent = new Intent(getActivity(), DownloadService.class);
    		
    		intent.putExtra(DownloadService.EXTRA_TRACK_ID, new Random().nextInt(1000));
    		intent.putExtra(DownloadService.EXTRA_START_NOW, true);
    		
    		getActivity().startService(intent);
    		
    		return true;
    	} else if (item == mClearButton)
    	{
    		Intent intent = new Intent(getActivity(), DownloadService.class);
    		intent.putExtra(DownloadService.EXTRA_COMMAND, DownloadService.COMMAND_CLEAR);
    		getActivity().startService(intent);
    		
    		return true;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
	
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
        		MuckeboxProvider.URI_DOWNLOADS,
                DownloadEntry.PROJECTION, null, null,
                DownloadEntry.SORT_ORDER);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
