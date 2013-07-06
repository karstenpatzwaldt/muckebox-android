package org.muckebox.android.ui.fragment;

import java.util.concurrent.TimeUnit;

import org.muckebox.android.R;
import org.muckebox.android.db.MuckeboxContract.TrackEntry;
import org.muckebox.android.db.MuckeboxProvider;
import org.muckebox.android.net.RefreshTracksTask;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
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
	
	private SimpleCursorAdapter mAdapter;
	private boolean mListLoaded = false;
	
	private class ListItemState {
		public int index = 0;
		public boolean extended = false;
		public int texts_height = 0;
		public int total_height = 0;
		public ListView list = null;
		
		public ListItemState(int newIndex)
		{
			index = newIndex;
		}
	}
	
	private class ContextCursorAdapter extends SimpleCursorAdapter {
		public ContextCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View ret = super.getView(position, convertView, parent);
			
			if (ret.getTag() == null)
			{
				ListItemState state = new ListItemState(position);
				int spec = MeasureSpec.makeMeasureSpec(4096, MeasureSpec.AT_MOST);			
				LinearLayout texts =
						(LinearLayout) ret.findViewById(R.id.track_list_texts);
				ViewGroup.LayoutParams params = ret.getLayoutParams();
				
				ret.measure(spec, spec);
				texts.measure(spec, spec);
	
				state.texts_height = texts.getMeasuredHeight();
				state.total_height = ret.getMeasuredHeight();
				state.list = (ListView) parent;
	
				params.height = state.texts_height;
				
				ret.setLayoutParams(params);
				ret.setTag(state);
				
				texts.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						toggleButtons(v);
					}
				});
				
				ImageButton playButton =
						(ImageButton) ret.findViewById(R.id.track_list_play);
				playButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(), PlayerService.class);
						getActivity().startService(intent);
						
						toggleButtons(v);
					}
				});
			}
			
			return ret;
		}
	}
	
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
        			TrackEntry.ALIAS_LENGTH
        			},
                new int[] {
        			R.id.track_list_title,
        			R.id.track_list_artist,
        			R.id.track_list_duration
        			}, 0);
        
        mAdapter.setViewBinder(new DurationViewBinder());
        
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
    }

    protected void onRefreshRequested() {
		new RefreshTracksTask().setCallbacks(this).execute(getAlbumId());
		mListLoaded = true;
    }

    @Override
    public boolean onClose() {
        return true;
    }
    
    private void hideItem(View item)
    {
    	ListItemState state = (ListItemState) item.getTag();
    	
    	if (state.extended)
    	{
			ValueAnimator anim = ValueAnimator.ofObject(new HeightEvaluator(item),
					state.total_height, state.texts_height);
			
			Log.d(LOG_TAG, "animate " + state.total_height + " -> " + state.texts_height);
			
			anim.setInterpolator(new AccelerateDecelerateInterpolator());
			anim.start();
			
			state.extended = false;
			item.setTag(state);   	
    	}
    }
    
    private void showItem(View item)
    {
    	ListItemState state = (ListItemState) item.getTag();

    	for (int i = 0; i < state.list.getCount(); ++i)
    	{
    		View child = state.list.getChildAt(i);
    		
    		if (child != null)
    		{
	    		if (i != state.index)
	    		{
	    			hideItem(child);
	    		} else
	    		{

	    	    	if (! state.extended)
	    	    	{
		    	    	ValueAnimator anim = ValueAnimator.ofObject(new HeightEvaluator(item),
		        				state.texts_height, state.total_height);
		
		        		Log.d(LOG_TAG, "animate " + state.texts_height + " -> " + state.total_height);
		
		        		anim.setInterpolator(new AccelerateDecelerateInterpolator());
		        		anim.start();
		        		
		        		state.extended = true;
		        		
		        		item.setTag(state);  	
	    	    	}
	    		}
    		}
    	}
    }
    
    public void toggleButtons(View item)
    {
    	View parent = item;
    	
    	do {
        	parent = (View) parent.getParent();
    	} while (parent.getId() != R.id.track_list_top);

    	ListItemState state = (ListItemState) parent.getTag();
    	
    	if (state.extended)
    	{
    		hideItem(parent);
    	} else
    	{
    		showItem(parent);
    	}
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri;
        if (hasAlbumId()) {
        	baseUri = Uri.parse(MuckeboxProvider.TRACK_ALBUM_BASE + Long.toString(getAlbumId()));
        } else {
            baseUri = MuckeboxProvider.URI_TRACKS;
        }

        return new CursorLoader(getActivity(), baseUri,
                TrackEntry.PROJECTION, null, null,
                TrackEntry.SORT_ORDER);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}