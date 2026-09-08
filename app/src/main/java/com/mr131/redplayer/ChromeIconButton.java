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
        if(!ValueAnimator.areAnimatorsEnabled()||isEmulator()){pulse=0f;invalidate();return;}
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
        if("raw_asset".equals(String.valueOf(getTag()))) {
            super.onDraw(canvas);
            float colorPulse=Math.max(pulse,active?.22f:0f);
            if(colorPulse>0f){
                Drawable.Callback callback=icon.getCallback();icon.setCallback(null);
                icon.setColorFilter(Color.argb((int)(150*colorPulse),214,10,0),PorterDuff.Mode.SRC_ATOP);
                icon.draw(canvas);icon.setColorFilter(null);icon.setCallback(callback);
            }
            return;
        }
        Drawable.Callback callback=icon.getCallback();icon.setCallback(null);
        for(int layer=8;layer>=1;layer--){icon.setColorFilter(Color.rgb(55+layer*8,2,2),PorterDuff.Mode.SRC_IN);canvas.save();canvas.translate(layer*.76f,layer*.76f);icon.draw(canvas);canvas.restore();}
        icon.setColorFilter(Color.rgb(9,10,12),PorterDuff.Mode.SRC_IN);
        canvas.save();canvas.translate(-2.2f,-1.2f);icon.draw(canvas);canvas.restore();
        int silver=(int)(230*(1f-pulse));int faceRed=(int)(214*pulse);
        icon.setColorFilter(Color.rgb(Math.max(faceRed,silver),Math.max(10,(int)(235*(1f-pulse))),Math.max(8,(int)(242*(1f-pulse)))),PorterDuff.Mode.SRC_IN);icon.draw(canvas);
        icon.setColorFilter(Color.argb(165,255,255,255),PorterDuff.Mode.SRC_IN);canvas.save();canvas.translate(-1.1f,-1.1f);icon.draw(canvas);canvas.restore();
        icon.setColorFilter(null);icon.setCallback(callback);
    }

    @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
}
