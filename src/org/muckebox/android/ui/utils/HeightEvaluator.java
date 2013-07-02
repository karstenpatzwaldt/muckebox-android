package org.muckebox.android.ui.utils;

import android.animation.IntEvaluator;
import android.view.View;
import android.view.ViewGroup;

public class HeightEvaluator extends IntEvaluator {
	private View mView;
	
	public HeightEvaluator(View view) {
		mView = view;
	}
	
	@Override
	public Integer evaluate(float fraction, Integer startValue, Integer endValue)
	{
		int num = super.evaluate(fraction, startValue, endValue);
		
		ViewGroup.LayoutParams p = mView.getLayoutParams();
		p.height = num;
		mView.setLayoutParams(p);
		
		return num;
	}
}
