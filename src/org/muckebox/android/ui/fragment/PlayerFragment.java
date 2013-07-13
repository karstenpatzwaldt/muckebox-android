package org.muckebox.android.ui.fragment;

import org.muckebox.android.R;
import org.muckebox.android.services.PlayerListener;
import org.muckebox.android.services.PlayerService;
import org.muckebox.android.ui.utils.HeightEvaluator;
import org.muckebox.android.ui.utils.TimeFormatter;

import android.animation.ValueAnimator;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class PlayerFragment
	extends Fragment
	implements PlayerListener
{
	private final static String LOG_TAG = "PlayerFragment";
	
	int mTotalHeight;
	int mTitleHeight;
	View mView;
	
	boolean mCollapsed = false;
	
	ImageButton mCollapseButton;
	
	ImageButton mPlayPauseButton;
	ImageButton mPreviousButton;
	ImageButton mNextButton;
	
	ImageView mPlayIndicator;
	TextView mTitleText;
	TextView mPlaytimeText;
	
	PlayerService mService = null;
	boolean mBound = false;
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_player, container, false);
        
        mTitleText = (TextView) mView.findViewById(R.id.player_title_text);
        mPlayIndicator = (ImageView) mView.findViewById(R.id.player_play_indicator);
        mPlaytimeText = (TextView) mView.findViewById(R.id.player_play_time);
        
        measureView();
        attachButtonListeners();
        
        return mView;
    }
    
    @Override
    public void onStart()
    {
    	super.onStart();
    	
    	simpleCollapse();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	Log.d(LOG_TAG, "Trying to bind to service");
    	
    	getActivity().getApplicationContext().bindService(
    			new Intent(getActivity(), PlayerService.class),
    			mConnection,
    			Context.BIND_AUTO_CREATE);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	getActivity().getApplicationContext().unbindService(mConnection);
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
    	public void onServiceConnected(ComponentName className, IBinder service)
    	{
    		PlayerService.PlayerBinder binder =
    				(PlayerService.PlayerBinder) service;
    		mService = binder.getService();
    		mBound = true;
    		
    		mService.registerListener(PlayerFragment.this);
    		
    		Log.d(LOG_TAG, "Bound to service");
    	}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mService = null;
			mBound = false;
			
			Log.d(LOG_TAG, "Unbound from service");
		}
    };
    
    private void attachButtonListeners() {
    	mCollapseButton = (ImageButton) mView.findViewById(R.id.player_collapse_button);
    	mCollapseButton.setOnClickListener(
    			new OnClickListener() {
    				public void onClick(View v) {
    					toggleCollapse();
    				}
    			});
    	
    	mPlayPauseButton = (ImageButton) mView.findViewById(R.id.player_play_pause_button);
    	mPlayPauseButton.setOnClickListener(
    			new OnClickListener() {
    				public void onClick(View v) {
    					onPlayPauseButton();
    				}
    			});
    	
    	mPreviousButton = (ImageButton) mView.findViewById(R.id.player_previous_button);
    	mPreviousButton.setOnClickListener(
    			new OnClickListener() {
    				public void onClick(View v) {
    					onPreviousButton();
    				}
    			});
    	
    	mNextButton = (ImageButton) mView.findViewById(R.id.player_next_button);
    	mNextButton.setOnClickListener(
    			new OnClickListener() {
    				public void onClick(View v) {
    					onNextButton();
    				}
    			});
    }

	private void measureView() {
		View container_view = mView.findViewById(R.id.player_container);
        View title_view = mView.findViewById(R.id.player_title_line);
        
        int spec = MeasureSpec.makeMeasureSpec(4096, MeasureSpec.AT_MOST);	
        
        container_view.measure(spec, spec);
        title_view.measure(spec, spec);
        mView.measure(spec, spec);
        
        mTotalHeight = container_view.getMeasuredHeight();
        mTitleHeight = title_view.getMeasuredHeight();
	}
	
	private void simpleCollapse() {
		ViewGroup.LayoutParams params = mView.getLayoutParams();
		
		params.height = mTitleHeight;
		mCollapsed = true;
		
		mView.setLayoutParams(params);
	}
	
	private void toggleCollapse()
	{
		if (mCollapsed)
		{
			changeHeight(mTitleHeight, mTotalHeight);
			mCollapseButton.setImageResource(R.drawable.navigation_collapse);
		} else {
			changeHeight(mTotalHeight, mTitleHeight);
			mCollapseButton.setImageResource(R.drawable.navigation_expand);
		}

		mCollapsed = ! mCollapsed;
	}
	
	private void changeHeight(int from, int to)
	{
		ValueAnimator anim = ValueAnimator.ofObject(new HeightEvaluator(mView),
				from, to);
		
		anim.setInterpolator(new AccelerateDecelerateInterpolator());
		anim.start();
	}
	
    private void onPlayPauseButton()
    {
    	if (mBound)
    	{
    		if (mService.isPlaying())
    		{
    			mService.pause();
    		} else
    		{
    			mService.resume();
    		}
    	}
    }
    
    private void onPreviousButton() {
    	if (mBound)
    	{
    		mService.previous();
    	}
    }
    
    private void onNextButton() {
    	if (mBound)
    	{
    		mService.next();
    	}
    }

	@Override
	public void onConnected() {
		onStopPlaying();
	}

	@Override
	public void onNewTrack(int id, String title, int duration) {
		mTitleText.setText(title);
		mPlaytimeText.setText(TimeFormatter.formatDuration(duration));
	}

	@Override
	public void onStartBuffering() {

	}

	@Override
	public void onStartPlaying() {
		onPlayResumed();
	}

	@Override
	public void onStopPlaying() {
		mTitleText.setText(R.string.no_track);
		mPlayPauseButton.setImageResource(R.drawable.av_play);
		mPlaytimeText.setText(R.string.null_null);
		mPlayIndicator.setImageResource(R.drawable.av_stop);
	}

	@Override
	public void onPlayPaused() {
		mPlayPauseButton.setImageResource(R.drawable.av_play);
		mPlayIndicator.setImageResource(R.drawable.av_pause);
	}

	@Override
	public void onPlayResumed() {
		mPlayPauseButton.setImageResource(R.drawable.av_pause);
		mPlayIndicator.setImageResource(R.drawable.av_play);
	}
	
}
