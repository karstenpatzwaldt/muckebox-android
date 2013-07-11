package org.muckebox.android.ui.fragment;

import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxProvider;
import org.muckebox.android.db.MuckeboxContract.ArtistEntry;
import org.muckebox.android.db.MuckeboxContract.DownloadEntry;
import org.muckebox.android.db.MuckeboxContract.TrackEntry;
import org.muckebox.android.services.DownloadService;
import org.muckebox.android.ui.utils.ExpandableCursorAdapter;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class DownloadListFragment extends ListFragment
	implements LoaderManager.LoaderCallbacks<Cursor> {

	SimpleCursorAdapter mAdapter;

	MenuItem mClearButton;
	
	private class DownloadViewBinder implements ViewBinder
	{
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (columnIndex == cursor.getColumnIndex(DownloadEntry.ALIAS_STATUS))
			{
				ImageView icon = (ImageView) view;
				
				switch (cursor.getInt(columnIndex))
				{
				case DownloadEntry.STATUS_VALUE_DOWNLOADING:
					icon.setImageResource(R.drawable.av_download);
					break;
				case DownloadEntry.STATUS_VALUE_QUEUED:
					icon.setImageResource(R.drawable.device_access_time);
					break;
				}
				
				return true;
			} else if (columnIndex == cursor.getColumnIndex(DownloadEntry.ALIAS_BYTES_DOWNLOADED)) {
				TextView text = (TextView) view;
				int size = cursor.getInt(columnIndex);
				
				if (size == 0)
					text.setText("");
				else
					text.setText(Formatter.formatFileSize(getActivity(), size));
				
				return true;
			}
				
			return false;
		}
	}
	
	private class DownloadCursorAdapter extends ExpandableCursorAdapter {
		private OnClickListener mDiscardListener;
		
		public DownloadCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
			
			mDiscardListener = new OnClickListener() {
				public void onClick(View v) {
					Intent intent = new Intent(getActivity(), DownloadService.class);
					
					intent.putExtra(DownloadService.EXTRA_COMMAND,
							DownloadService.COMMAND_DISCARD);
					intent.putExtra(DownloadService.EXTRA_TRACK_ID,
							getTrackId(getItemIndex(v)));
					
					getActivity().startService(intent);
					
					toggleExpanded(v);
				}
			};
		}
		
		private int getTrackId(int position) {
			Cursor c = getCursor();
			
			c.moveToPosition(position);
			
			return c.getInt(c.getColumnIndex(TrackEntry.ALIAS_ID));
		}
		
		@Override
		public View getView(final int position, View convertView, ViewGroup parent)
		{
			View ret = super.getView(position, convertView, parent);
			
			if (ret.getTag() == null)
			{
				ImageButton discardButton =
						(ImageButton) ret.findViewById(R.id.download_list_row_discard);
				
				discardButton.setOnClickListener(mDiscardListener);
				
				ret.setTag(true);
			}
			
			return ret;
		}
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_downloads, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        mAdapter = new DownloadCursorAdapter(getActivity(),
                R.layout.list_row_download, null,
                new String[] {
        			TrackEntry.ALIAS_TITLE,
        			ArtistEntry.ALIAS_NAME,
        			DownloadEntry.ALIAS_STATUS,
        			DownloadEntry.ALIAS_BYTES_DOWNLOADED
        		},
                new int[] {
        			R.id.download_list_row_title,
        			R.id.download_list_row_artist,
        			R.id.download_list_row_status,
        			R.id.download_list_row_bytes_downloaded
        		}, 0);
        mAdapter.setViewBinder(new DownloadViewBinder());
        
        setListAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, this);
	}
	
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	super.onCreateOptionsMenu(menu, inflater);
    	
    	inflater.inflate(R.menu.download_list, menu);
    	
    	mClearButton = menu.findItem(R.id.download_list_action_clear);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == mClearButton)
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
        		MuckeboxProvider.URI_DOWNLOADS_WITHDETAILS, null, null, null, null);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
