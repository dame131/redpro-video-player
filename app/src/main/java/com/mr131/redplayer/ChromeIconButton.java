package com.mr131.redplayer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

/** A borderless icon with thick crimson sides and a bright chrome face. */
public final class ChromeIconButton extends AppCompatImageButton {
    private float pulse;
    private boolean active;
    private ValueAnimator animator;

    public ChromeIconButton(Context c) { super(c); prepare(); }
    public ChromeIconButton(Context c, @Nullable AttributeSet a) { super(c,a); prepare(); }
    public ChromeIconButton(Context c, @Nullable AttributeSet a, int s) { super(c,a,s); prepare(); }

    private void prepare() {
        setBackgroundColor(Color.TRANSPARENT);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setChromeActive(boolean value) { active=value; invalidate(); }

    public void pulse() {
        if(!ValueAnimator.areAnimatorsEnabled()||isEmulator()){pulse=.35f;invalidate();return;}
        if(animator!=null)animator.cancel();
        animator=ValueAnimator.ofFloat(0f,1f,0f);
        animator.setDuration(620);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a->{pulse=(float)a.getAnimatedValue();invalidate();});
        animator.start();
    }

    private boolean isEmulator(){String model=android.os.Build.MODEL.toLowerCase();String hardware=android.os.Build.HARDWARE.toLowerCase();String product=android.os.Build.PRODUCT.toLowerCase();return model.contains("sdk")||model.contains("emulator")||hardware.contains("ranchu")||hardware.contains("goldfish")||product.contains("sdk")||product.contains("emulator");}

    @Override protected void onDraw(Canvas canvas) {
        Drawable icon=getDrawable();
        if(icon==null){super.onDraw(canvas);return;}
        int save=canvas.save();
        canvas.translate(getScrollX(),getScrollY());
        for(int layer=7;layer>=1;layer--){
            icon.setColorFilter(Color.rgb(58+layer*7,3,2), PorterDuff.Mode.SRC_IN);
            canvas.save();canvas.translate(layer*.82f,layer*.82f);icon.draw(canvas);canvas.restore();
        }
        icon.setColorFilter(Color.rgb(32,33,36),PorterDuff.Mode.SRC_IN);
        canvas.save();canvas.translate(-1.8f,-1.8f);icon.draw(canvas);canvas.restore();
        int red=(int)(205*pulse)+(active?35:0);
        int green=(int)(222*(1f-pulse));
        int blue=(int)(230*(1f-pulse));
        icon.setColorFilter(Color.rgb(Math.min(255,215+red/7),green,blue),PorterDuff.Mode.SRC_IN);
        icon.draw(canvas);
        icon.setColorFilter(null);
        canvas.restoreToCount(save);
    }

    @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
}
