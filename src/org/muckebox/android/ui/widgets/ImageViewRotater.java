package org.muckebox.android.ui.widgets;

import org.muckebox.android.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class ImageViewRotater  {
	static public ImageView getRotatingImageView(Context context, int resource_id) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    
		ImageView iv = (ImageView) inflater.inflate(resource_id, null);

		Animation rotation = AnimationUtils.loadAnimation(context, R.anim.clockwise_refresh);
	    rotation.setRepeatCount(Animation.INFINITE);
	    iv.startAnimation(rotation);
	    
	    return iv;
	}
}
