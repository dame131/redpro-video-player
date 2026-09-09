package com.mr131.redplayer;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;

/** Creates a real thick chrome emblem for the top of secondary screens. */
public final class LuxuryIconView {
    private LuxuryIconView() {}
    public static ImageView create(Context context,int drawable,String description){
        ImageView icon=new ImageView(context);icon.setImageResource(drawable);icon.setContentDescription(description);
        icon.setBackgroundColor(Color.TRANSPARENT);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int size=Math.round(78*context.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(size,size);params.gravity=Gravity.CENTER_HORIZONTAL;
        icon.setLayoutParams(params);return icon;
    }
}
