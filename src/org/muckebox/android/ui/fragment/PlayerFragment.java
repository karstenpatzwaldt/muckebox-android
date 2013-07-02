package org.muckebox.android.ui.fragment;

import org.muckebox.android.R;
import org.muckebox.android.ui.utils.HeightEvaluator;

import android.animation.ValueAnimator;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;

public class PlayerFragment extends Fragment {
	int mTotalHeight;
	int mTitleHeight;
	View mView;
	boolean mCollapsed = false;
	
	ImageButton mCollapseButton;
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_player, container, false);

        measureView();
        attachButtonListeners();
//        simpleCollapse();
        
        return mView;
    }
    
    @Override
    public void onStart()
    {
    	super.onStart();
    	
    	simpleCollapse();
    }
    
    private void attachButtonListeners() {
    	mCollapseButton = (ImageButton) mView.findViewById(R.id.player_collapse_button);
    	
    	mCollapseButton.setOnClickListener(
    			new OnClickListener() {
    				public void onClick(View v) {
    					toggleCollapse();
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
			mCollapseButton.setImageResource(R.drawable.navigation_expand);
		} else {
			changeHeight(mTotalHeight, mTitleHeight);
			mCollapseButton.setImageResource(R.drawable.navigation_collapse);
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
}
