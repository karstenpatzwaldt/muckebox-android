package org.muckebox.android.ui.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.widget.ImageButton;

public class ImageButtonHelper {
    public static void setImageButtonEnabled(
        Context ctxt, boolean enabled, ImageButton item, int iconResId) {
        if (item != null) {
            Drawable originalIcon = ctxt.getResources().getDrawable(iconResId);
            Drawable icon = enabled ? originalIcon : convertDrawableToGrayScale(originalIcon);
            
            item.setImageDrawable(icon);
            item.setEnabled(enabled);
        }
    }
    
    public static Drawable convertDrawableToGrayScale(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        
        Drawable res = drawable.mutate();
        res.setColorFilter(Color.GRAY, Mode.SRC_IN);
        
        return res;
    }
}
