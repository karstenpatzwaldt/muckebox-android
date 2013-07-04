package org.muckebox.android.ui.widgets;

import org.muckebox.android.R;
import org.muckebox.android.net.RefreshTask;

import android.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

abstract public class RefreshableListFragment
	extends ListFragment
	implements RefreshTask.Callbacks{
	
	private MenuItem mRefreshItem = null;
	private boolean mIsRefreshing = false;

	// Clients just implement this to handle the action menu click
	protected abstract void onRefreshRequested();

	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	stopRefreshAnimation();
    	
    	super.onCreateOptionsMenu(menu, inflater);
    	
      	inflater.inflate(R.menu.refreshable_list, menu);
      	
      	mRefreshItem = menu.findItem(R.id.action_refresh);
    }
    
    @Override
    public void onDestroyOptionsMenu()
    {
    	stopRefreshAnimation();
    	
    	super.onDestroyOptionsMenu();
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == mRefreshItem)
    	{
    		onRefreshRequested();
    		return true;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause()
    {
    	stopRefreshAnimation();
    	
    	super.onPause();
    }

    protected void startRefreshAnimation()
    {
		if (mRefreshItem != null && mRefreshItem.getActionView() == null)
			mRefreshItem.setActionView(
					ImageViewRotater.getRotatingImageView(
							getActivity(), R.layout.action_view_refresh));
    }
    
    protected void stopRefreshAnimation()
    {
    	if (mRefreshItem != null)
    	{
			if (mRefreshItem.getActionView() != null)
				mRefreshItem.getActionView().clearAnimation();
			
			mRefreshItem.setActionView(null);
    	}
    }
    
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		if (mIsRefreshing)
		{
			startRefreshAnimation();
		} else
		{
			stopRefreshAnimation();
		}
	}
	
	protected void setRefreshing(boolean refreshing) {
		mIsRefreshing = refreshing;
		
		if (refreshing)
		{
			startRefreshAnimation();
		} else
		{
			stopRefreshAnimation();
		}
	}

	@Override
	public void onRefreshStarted() {
		setRefreshing(true);
	}
	
	@Override
	public void onRefreshFinished(boolean success) {
		setRefreshing(false);
	}
}
