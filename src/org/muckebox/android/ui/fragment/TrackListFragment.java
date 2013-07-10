package org.muckebox.android.ui.fragment;

import java.util.concurrent.TimeUnit;

import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxContract.CacheEntry;
import org.muckebox.android.db.MuckeboxContract.DownloadEntry;
import org.muckebox.android.db.MuckeboxContract.TrackDownloadCacheJoin;
import org.muckebox.android.db.MuckeboxContract.TrackEntry;
import org.muckebox.android.db.MuckeboxProvider;
import org.muckebox.android.net.RefreshTracksTask;
import org.muckebox.android.services.DownloadService;
import org.muckebox.android.services.PlayerService;
import org.muckebox.android.ui.utils.HeightEvaluator;
import org.muckebox.android.ui.widgets.RefreshableListFragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SearchView.OnCloseListener;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class TrackListFragment extends RefreshableListFragment
	implements OnCloseListener,
	LoaderManager.LoaderCallbacks<Cursor> {
	private static final String LOG_TAG = "TrackListFragment";
	
	private static final String ALBUM_ID_ARG = "album_id";
	private static final String ALBUM_TITLE_ARG = "album_title";
	
	private ContextCursorAdapter mAdapter;
	private boolean mListLoaded = false;
	
	private class ListItemState {
		public int index = 0;
		public int textsHeight = 0;
		public int totalHeight = 0;
		public ListView list = null;
	}
	
	MenuItem mPinAllItem;
	MenuItem mRemoveAllItem;
	
	private class ContextCursorAdapter extends SimpleCursorAdapter {
		int mIndexExtended = -1;
		final int mSpec = MeasureSpec.makeMeasureSpec(4096, MeasureSpec.AT_MOST);			
		
		public ContextCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
		}
		
		public void setIndexExtended(int index) {
			mIndexExtended = index;
		}
		
		public int getIndexExtended() {
			return mIndexExtended;
		}
		
		public int getTrackId(int position) {
			Cursor c = getCursor();
			
			c.moveToPosition(position);
			
			return c.getInt(c.getColumnIndex(TrackEntry.SHORT_ID));
		}
		
	    private void hideItem(View item)
	    {
	    	ListItemState state = (ListItemState) item.getTag();
	    	
			ValueAnimator anim = ValueAnimator.ofObject(new HeightEvaluator(item),
					state.totalHeight, state.textsHeight);
			
			Log.d(LOG_TAG, "animate " + state.totalHeight + " -> " + state.textsHeight);
			
			anim.setInterpolator(new AccelerateDecelerateInterpolator());
			anim.start();

			item.setTag(state);
	    }
	    
	    private void showItem(View item)
	    {
	    	ListItemState state = (ListItemState) item.getTag();

	    	ValueAnimator anim = ValueAnimator.ofObject(new HeightEvaluator(item),
					state.textsHeight, state.totalHeight);

			anim.setInterpolator(new AccelerateDecelerateInterpolator());
			anim.start();
			
			item.setTag(state);
	    }
	    
	    private View getListItem(View view) {
	    	View parent = view;
	    	
	    	do {
	        	parent = (View) parent.getParent();
	    	} while (parent.getId() != R.id.track_list_top);
	    	
	    	return parent;
	    }
	    
	    private ListItemState getItemState(View view) {
	    	return (ListItemState) getListItem(view).getTag();
	    }
	    
	    public void toggleButtons(View item)
	    {
	    	View parent = getListItem(item);
	    	ListItemState state = (ListItemState) parent.getTag();
	    	int indexExtended = mAdapter.getIndexExtended();
	    	
	    	if (state.index == indexExtended)
	    	{
	    		mAdapter.setIndexExtended(-1);
	    		hideItem(parent);
	    	} else
	    	{
	    		int first = state.list.getFirstVisiblePosition();
	    		int end = first + state.list.getChildCount();
	    		
	    		if (indexExtended != -1 && indexExtended >= first && indexExtended < end)
	    		{
	    			View extendedItem = state.list.getChildAt(indexExtended - first);
	    			
	    			if (extendedItem != null)
	    				hideItem(extendedItem);
	    		}

	    		mAdapter.setIndexExtended(state.index);
	    		showItem(parent);
	    	}
	    }

		@Override
		public View getView(final int position, View convertView, ViewGroup parent)
		{
			View ret = super.getView(position, convertView, parent);
			ListItemState state = (ListItemState) ret.getTag();
			ViewGroup.LayoutParams params = ret.getLayoutParams();
					
			if (state == null)
			{
				LinearLayout texts =
						(LinearLayout) ret.findViewById(R.id.track_list_texts);

				ret.measure(mSpec, mSpec);
				texts.measure(mSpec, mSpec);
	
				state = new ListItemState();
				
				state.textsHeight = texts.getMeasuredHeight();
				state.totalHeight = ret.getMeasuredHeight();
				state.list = (ListView) parent;
	
				texts.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						toggleButtons(v);
					}
				});
				
				ImageView playButton = (ImageButton) ret.findViewById(R.id.track_list_play);
				playButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(), PlayerService.class);
						
						intent.putExtra(PlayerService.EXTRA_TRACK_ID,
								mAdapter.getTrackId(getItemState(v).index));
						
						getActivity().startService(intent);
						toggleButtons(v);
					}
				});
				
				ImageView downloadButton =
						(ImageButton) ret.findViewById(R.id.track_list_download);
				downloadButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(), DownloadService.class);
						
						intent.putExtra(DownloadService.EXTRA_TRACK_ID,
								mAdapter.getTrackId(getItemState(v).index));
						
						getActivity().startService(intent);
						toggleButtons(v);
					}
				});
				
				ImageButton pinButton =
						(ImageButton) ret.findViewById(R.id.track_list_pin);
				pinButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						pinTrack(mAdapter.getTrackId(getItemState(v).index));
						toggleButtons(v);
					}
				});
				
				ImageButton discardButton =
						(ImageButton) ret.findViewById(R.id.track_list_discard);
				discardButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						discardTrack(mAdapter.getTrackId(getItemState(v).index));
						toggleButtons(v);
					}
				});
			}
			
			state.index = position;
			
			params.height = (position == mIndexExtended) ?
					state.totalHeight : state.textsHeight;
			
			ret.setLayoutParams(params);
			ret.setTag(state);
			
			return ret;
		}
	}
	
	private void pinTrack(int trackId) {
		Intent intent = new Intent(getActivity(), DownloadService.class);
		
		intent.putExtra(DownloadService.EXTRA_TRACK_ID, trackId);
		intent.putExtra(DownloadService.EXTRA_PIN, true);
		
		getActivity().startService(intent);
	}
	
	private void discardTrack(int trackId) {
		Intent intent = new Intent(getActivity(), DownloadService.class);
		
		intent.putExtra(DownloadService.EXTRA_COMMAND,
				DownloadService.COMMAND_DISCARD);
		intent.putExtra(DownloadService.EXTRA_TRACK_ID, trackId);
		
		getActivity().startService(intent);
	}
	
	private class TracklistViewBinder implements ViewBinder {
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
    		} else if (columnIndex == cursor.getColumnIndex(DownloadEntry.ALIAS_STATUS))
			{
    			ImageView icon = (ImageView) view;
    			
    			if (cursor.isNull(columnIndex))
    			{
    				icon.setVisibility(View.GONE);
    			} else
    			{
    				icon.setVisibility(View.VISIBLE);
    				
	    			switch (cursor.getInt(columnIndex))
	    			{
	    			case DownloadEntry.STATUS_VALUE_QUEUED:
	    				icon.setImageResource(R.drawable.device_access_time);
	    				break;
	    			case DownloadEntry.STATUS_VALUE_DOWNLOADING:
	    				// XXX add download animation
	    				icon.setImageResource(R.drawable.av_download);
	    				break;
	    			}
    			}
	    			
    			return true;
			} else if (columnIndex == cursor.getColumnIndex(CacheEntry.ALIAS_PINNED))
			{
				ImageView icon = (ImageView) view;
				
				if (cursor.isNull(columnIndex))
				{
					icon.setVisibility(View.GONE);
				} else
				{
					icon.setVisibility(View.VISIBLE);
					
					switch (cursor.getInt(columnIndex))
					{
					case 0:
						icon.setImageResource(R.drawable.navigation_accept);
						break;
					case 1:
						icon.setImageResource(R.drawable.av_make_available_offline);
						break;
					}
				}
				
				return true;
			} else if (columnIndex ==
					cursor.getColumnIndex(TrackDownloadCacheJoin.ALIAS_CANCELABLE))
			{
				boolean downloading;
				int pinStatus;
				
				int downloadStatusIndex = cursor.getColumnIndex(DownloadEntry.ALIAS_STATUS);
				int pinStatusIndex = cursor.getColumnIndex(CacheEntry.ALIAS_PINNED);

				downloading = ! cursor.isNull(downloadStatusIndex);
				pinStatus = cursor.isNull(pinStatusIndex) ? -1 : cursor.getInt(pinStatusIndex);
				
				ImageButton downloadButton =
						(ImageButton) view.findViewById(R.id.track_list_download);
				ImageButton pinButton =
						(ImageButton) view.findViewById(R.id.track_list_pin);
				ImageButton discardButton =
						(ImageButton) view.findViewById(R.id.track_list_discard);
				
				if (! downloading && pinStatus == -1)
				{
					downloadButton.setVisibility(View.VISIBLE);
					pinButton.setVisibility(View.VISIBLE);
					discardButton.setVisibility(View.GONE);
				} else if (downloading)
				{
					downloadButton.setVisibility(View.GONE);
					pinButton.setVisibility(View.GONE);
					discardButton.setVisibility(View.VISIBLE);
					
					discardButton.setImageResource(R.drawable.navigation_cancel);
				} else
				{
					downloadButton.setVisibility(View.GONE);
					pinButton.setVisibility(pinStatus == 0 ? View.VISIBLE : View.GONE);
					discardButton.setVisibility(View.VISIBLE);
					
					discardButton.setImageResource(R.drawable.content_discard);
				}
				
				return true;
			}
			
			return false;
    	}
	}
	
	public static TrackListFragment newInstanceFromAlbum(long album_id, String title) {
		TrackListFragment f = new TrackListFragment();
		Bundle args = new Bundle();
		
		args.putLong(ALBUM_ID_ARG, album_id);
		args.putString(ALBUM_TITLE_ARG, title);
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
	
	public String getAlbumTitle() {
		Bundle args = getArguments();
		
		return args.getString(ALBUM_TITLE_ARG, "");
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_track_browse, container, false);
	}

	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        mAdapter = new ContextCursorAdapter(getActivity(),
                R.layout.list_row_track, null,
                new String[] {
        			TrackEntry.ALIAS_TITLE,
        			TrackEntry.ALIAS_DISPLAY_ARTIST,
        			TrackEntry.ALIAS_LENGTH,
        			DownloadEntry.ALIAS_STATUS,
        			CacheEntry.ALIAS_PINNED,
        			TrackDownloadCacheJoin.ALIAS_CANCELABLE
        			},
                new int[] {
        			R.id.track_list_title,
        			R.id.track_list_artist,
        			R.id.track_list_duration,
        			R.id.track_list_download_status,
        			R.id.track_list_cache_status,
        			R.id.track_list_buttons,
        			}, 0);
        
        mAdapter.setViewBinder(new TracklistViewBinder());
        
        TextView header = (TextView) getActivity().findViewById(R.id.track_list_title_strip);
        
        header.setText(getAlbumTitle());
 
        setListAdapter(mAdapter);
        getLoaderManager().initLoader(0, null, this);
        
        if (! mListLoaded)
        	onRefreshRequested();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	super.onCreateOptionsMenu(menu, inflater);
    	
    	inflater.inflate(R.menu.track_list, menu);
    	
    	mPinAllItem = menu.findItem(R.id.track_list_action_pin);
    	mRemoveAllItem = menu.findItem(R.id.track_list_action_remove);
    	
    	mRemoveAllItem.setVisible(false);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == mPinAllItem)
    	{
    		Cursor c = mAdapter.getCursor();
    		int trackIdIndex = c.getColumnIndex(TrackEntry.SHORT_ID);
    		
    		c.moveToPosition(-1);
    		
    		while (c.moveToNext())
    			pinTrack(c.getInt(trackIdIndex));
    				
    		return true;
    	} else if (item == mRemoveAllItem)
    	{
    		Cursor c = mAdapter.getCursor();
    		int trackIdIndex = c.getColumnIndex(TrackEntry.SHORT_ID);
    		
    		c.moveToPosition(-1);
    		
    		while (c.moveToNext())
    			discardTrack(c.getInt(trackIdIndex));
    				
    		return true;
    	}
    	
    	return false;
    }

    protected void onRefreshRequested() {
		new RefreshTracksTask().setCallbacks(this).execute(getAlbumId());
		mListLoaded = true;
    }

    @Override
    public boolean onClose() {
        return true;
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri;
        if (hasAlbumId()) {
        	baseUri = MuckeboxProvider.URI_TRACKS_WITH_DETAILS_ALBUM.buildUpon().appendPath(
        			Long.toString(getAlbumId())).build();
        } else {
            baseUri = MuckeboxProvider.URI_TRACKS_WITH_DETAILS;
        }

        return new CursorLoader(getActivity(), baseUri, null, null, null, null);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        
        data.moveToPosition(-1);
        
        boolean oneCached = false;
        boolean oneNotPinned = false;
        
        int cachedIndex = data.getColumnIndex(CacheEntry.ALIAS_PINNED);
        int downloadingIndex = data.getColumnIndex(DownloadEntry.ALIAS_STATUS);
        
        while (data.moveToNext())
        {
        	if (data.isNull(cachedIndex) && data.isNull(downloadingIndex))
        		oneNotPinned = true;
        	else
        		oneCached = true;
        }
        
        mPinAllItem.setVisible(oneNotPinned);
        mRemoveAllItem.setVisible(oneCached);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}