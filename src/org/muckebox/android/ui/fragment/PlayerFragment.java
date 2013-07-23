/*   
 * Copyright 2013 Karsten Patzwaldt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.muckebox.android.ui.fragment;

import org.muckebox.android.R;
import org.muckebox.android.services.PlayerListener;
import org.muckebox.android.services.PlayerService;
import org.muckebox.android.ui.utils.HeightEvaluator;
import org.muckebox.android.ui.utils.ImageButtonHelper;
import org.muckebox.android.ui.utils.TimeFormatter;

import android.animation.ValueAnimator;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
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
import android.widget.SeekBar;
import android.widget.TextView;

public class PlayerFragment
	extends Fragment
	implements PlayerListener, SeekBar.OnSeekBarChangeListener
{
	private final static String LOG_TAG = "PlayerFragment";
	
	private final static String STATE_COLLAPSED = "collapsed";

	int mTotalHeight;
	int mTitleHeight;
	View mView;
	
	boolean mCollapsed = true;
	
	ImageButton mCollapseButton;
	
	ImageButton mPlayPauseButton;
	ImageButton mPreviousButton;
	ImageButton mNextButton;
	ImageButton mStopButton;
	
	SeekBar mSeekBar;
	
	ImageView mPlayIndicator;
	TextView mTitleText;
	TextView mPlaytimeText;
	
	PlayerService mService = null;
	boolean mBound = false;
	
	private String mCurrentTitle = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_player, container, false);
        
        mTitleText = (TextView) mView.findViewById(R.id.player_title_text);
        mPlayIndicator = (ImageView) mView.findViewById(R.id.player_play_indicator);
        
        mPlaytimeText = (TextView) mView.findViewById(R.id.player_play_time);
        mSeekBar = (SeekBar) mView.findViewById(R.id.player_seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);
        
        measureView();
        attachButtonListeners();

        return  mView;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	Log.d(LOG_TAG, "Trying to bind to service");
    	
    	getActivity().getApplicationContext().bindService(
    			new Intent(getActivity(), PlayerService.class),
    			mConnection,
    			Context.BIND_AUTO_CREATE);
    	
    	getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	
    	if (savedInstanceState != null)
    	    mCollapsed = savedInstanceState.getBoolean(STATE_COLLAPSED, true);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        attachButtonListeners();
        
        if (mCollapsed)
            simpleCollapse();
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        
        savedInstanceState.putBoolean(STATE_COLLAPSED, mCollapsed);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	mService.removeListener(this);
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
    		
    		PlayerService.TrackInfo trackInfo = mService.getCurrentTrackInfo();
    		
    		if (trackInfo != null)
    		    onNewTrack(trackInfo);
    		
    		if (mService.isPlaying())
    		    onPlayResumed();
    		
    		if (mService.isPaused())
    		    onPlayPaused();
    		
    		if (mService.isStopped())
    		    onStopPlaying();
    		
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
    	
    	mStopButton = (ImageButton) mView.findViewById(R.id.player_stop_button);
    	mStopButton.setOnClickListener(
    	    new OnClickListener() {
    	        public void onClick(View v) {
    	            onStopButton();
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
		
		mView.setLayoutParams(params);
		mCollapseButton.setImageResource(R.drawable.navigation_expand);
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
    
    private void onStopButton() {
        if (mBound)
        {
            mService.stop();
        }
    }

	@Override
	public void onConnected() {
		onStopPlaying();
	}

	@Override
	public void onNewTrack(PlayerService.TrackInfo trackInfo) {
	    mCurrentTitle = trackInfo.title;
	    
	    if (! mService.isBuffering()) {
    	    mTitleText.setText(mCurrentTitle); 
            ImageButtonHelper.setImageViewDisabled(getActivity(),
                mPlayIndicator, R.drawable.av_pause);
	    }

	    ImageButtonHelper.setImageButtonEnabled(
	        getActivity(), trackInfo.hasNext, mNextButton, R.drawable.av_next);
	    ImageButtonHelper.setImageButtonEnabled(
	        getActivity(), trackInfo.hasPrevious, mPreviousButton, R.drawable.av_previous);
	    ImageButtonHelper.setImageButtonEnabled(
	        getActivity(), true, mStopButton, R.drawable.av_stop);

	    mSeekBar.setEnabled(! trackInfo.isStreaming);
	    mSeekBar.setMax(trackInfo.duration);

	    onPlayProgress(0);
	}

	@Override
	public void onStartBuffering() {
	    ImageButtonHelper.setImageViewDisabled(getActivity(),
	        mPlayIndicator, R.drawable.av_pause);
	    mTitleText.setText(R.string.status_buffering);
	}

	@Override
	public void onStartPlaying() {
		onPlayResumed();
	}

	@Override
	public void onStopPlaying() {
		mTitleText.setText(R.string.no_track);
		
        ImageButtonHelper.setImageButtonEnabled(
            getActivity(), false, mPlayPauseButton, R.drawable.av_play);
        ImageButtonHelper.setImageButtonEnabled(
            getActivity(), false, mStopButton, R.drawable.av_stop);
        ImageButtonHelper.setImageButtonEnabled(
            getActivity(), false, mNextButton, R.drawable.av_next);
        ImageButtonHelper.setImageButtonEnabled(
            getActivity(), false, mPreviousButton, R.drawable.av_previous);		
		
		mPlaytimeText.setText(R.string.null_null);
		ImageButtonHelper.setImageViewDisabled(getActivity(),
		    mPlayIndicator, R.drawable.av_stop);
		
		mSeekBar.setProgress(0);
		mSeekBar.setEnabled(false);
	}

	@Override
	public void onPlayPaused() {
       ImageButtonHelper.setImageButtonEnabled(
            getActivity(), true, mPlayPauseButton, R.drawable.av_play);
	       
		ImageButtonHelper.setImageViewDisabled(getActivity(),
		    mPlayIndicator, R.drawable.av_pause);
	}

	@Override
	public void onPlayResumed() {
       ImageButtonHelper.setImageButtonEnabled(
            getActivity(), true, mPlayPauseButton, R.drawable.av_pause);
		ImageButtonHelper.setImageViewDisabled(getActivity(),
		    mPlayIndicator, R.drawable.av_play);
		mTitleText.setText(mCurrentTitle);
	}
	
	@Override
	public void onPlayProgress(int secondsElapsed) {
        int timeRemaining = mService.getCurrentTimeLeft();
        
        mPlaytimeText.setText(TimeFormatter.formatDuration(timeRemaining));
        mSeekBar.setProgress(secondsElapsed);
	}

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mService != null && fromUser) {
            mService.seek(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // nothing
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // nothing
    }
}
