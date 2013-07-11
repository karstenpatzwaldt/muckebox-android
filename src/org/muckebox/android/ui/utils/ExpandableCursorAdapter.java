package org.muckebox.android.ui.utils;

import org.muckebox.android.R;

import android.animation.ValueAnimator;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ExpandableCursorAdapter extends SimpleCursorAdapter {
	private static final int SPEC = MeasureSpec.makeMeasureSpec(4096, MeasureSpec.AT_MOST);
	private static final int TAG = 892766765;

	private int mIndexExtended = -1;
	
	private OnClickListener mTriggerListener;
	
	private class ListItemState {
		public int index = 0;
		public int collapsedHeight = 0;
		public int totalHeight = 0;
		public ListView list = null;
	}
	
	public ExpandableCursorAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
		
		mTriggerListener = new View.OnClickListener() {
			public void onClick(View v) {
				toggleExpanded(v);
			}
		};
	}
	
	public void setIndexExtended(int index) {
		mIndexExtended = index;
	}
	
	public int getIndexExtended() {
		return mIndexExtended;
	}
	
    private void hideItem(View item)
    {
    	ListItemState state = (ListItemState) item.getTag(TAG);
    	
		ValueAnimator anim = ValueAnimator.ofObject(new HeightEvaluator(item),
				state.totalHeight, state.collapsedHeight);

		anim.setInterpolator(new AccelerateDecelerateInterpolator());
		anim.start();

		item.setTag(TAG, state);
    }
    
    private void showItem(View item)
    {
    	ListItemState state = (ListItemState) item.getTag(TAG);

    	ValueAnimator anim = ValueAnimator.ofObject(new HeightEvaluator(item),
				state.collapsedHeight, state.totalHeight);

		anim.setInterpolator(new AccelerateDecelerateInterpolator());
		anim.start();
		
		item.setTag(TAG, state);
    }
    
    private View getListItem(View view) {
    	View parent = view;
    	
    	do {
        	parent = (View) parent.getParent();
    	} while (parent.getId() != R.id.expandable_full);
    	
    	return parent;
    }
    
    private ListItemState getItemState(View view) {
    	return (ListItemState) getListItem(view).getTag(TAG);
    }
    
    protected int getItemIndex(View view) {
    	return getItemState(view).index;
    }
    
    public void toggleExpanded(View item)
    {
    	View parent = getListItem(item);
    	ListItemState state = (ListItemState) parent.getTag(TAG);
    	int indexExtended = getIndexExtended();
    	
    	if (state.index == indexExtended)
    	{
    		setIndexExtended(-1);
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

    		setIndexExtended(state.index);
    		showItem(parent);
    	}
    }

	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		View ret = super.getView(position, convertView, parent);
		ListItemState state = (ListItemState) ret.getTag(TAG);
		ViewGroup.LayoutParams params = ret.getLayoutParams();
				
		if (state == null)
		{
			View triggerView = ret.findViewById(R.id.expandable_short);

			ret.measure(SPEC, SPEC);
			triggerView.measure(SPEC, SPEC);

			state = new ListItemState();
			
			state.collapsedHeight = triggerView.getMeasuredHeight();
			state.totalHeight = ret.getMeasuredHeight();
			state.list = (ListView) parent;

			triggerView.setOnClickListener(mTriggerListener);
		}
		
		state.index = position;
		
		params.height = (position == mIndexExtended) ?
				state.totalHeight : state.collapsedHeight;
		
		ret.setLayoutParams(params);
		ret.setTag(TAG, state);
		
		return ret;
	}
}