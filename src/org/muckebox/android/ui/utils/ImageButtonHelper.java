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

package org.muckebox.android.ui.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.widget.ImageButton;
import android.widget.ImageView;

public class ImageButtonHelper {
    private static SparseArray<Drawable> mGrayscaleCache = new SparseArray<Drawable>();
    
    public static void setImageButtonEnabled(
        Context ctx, boolean enabled, ImageButton item, int resId) {
        setImageViewEnabled(ctx, item, resId, enabled);
        item.setEnabled(enabled);
    }
    
    public static void setImageViewEnabled(
        Context ctx, ImageView view, int resId, boolean enabled) {
        view.setImageDrawable(getDrawable(ctx, resId, enabled));
    }
    
    public static void setImageViewEnabled(Context ctx, ImageView view, int resId) {
        setImageViewEnabled(ctx, view, resId, true);
    }
    
    public static void setImageViewDisabled(Context ctx, ImageView view, int resId) {
        setImageViewEnabled(ctx, view, resId, false);
    }
    
    public static Drawable getDrawable(Context ctx, int resId, boolean enabled) {
        if (enabled)
            return ctx.getResources().getDrawable(resId);
                
        if (mGrayscaleCache.indexOfKey(resId) > 0) {
            return mGrayscaleCache.get(resId);
        } else {
            Drawable drawable = convertDrawableToGrayScale(ctx, resId);
        
            mGrayscaleCache.put(resId, drawable);
            
            return drawable;
        }
    }
    
    public static Drawable convertDrawableToGrayScale(Context ctx, int resId) {
        Drawable res = ctx.getResources().getDrawable(resId).mutate();
        
        res.setColorFilter(Color.GRAY, Mode.SRC_IN);
        
        return res;
    }
}
