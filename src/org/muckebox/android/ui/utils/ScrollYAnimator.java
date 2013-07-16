package org.muckebox.android.ui.utils;

import android.animation.IntEvaluator;
import android.widget.ListView;

public class ScrollYAnimator extends IntEvaluator {
    private ListView mView;
    private int mIndex;
    
    public ScrollYAnimator(ListView view, int index) {
        mView = view;
        mIndex = index;
    }
    
    @Override
    public Integer evaluate(float fraction, Integer startValue, Integer endValue)
    {
        final int num = super.evaluate(fraction, startValue, endValue);
        
        mView.setSelectionFromTop(mIndex, num);
        
        return num;
    }
}
