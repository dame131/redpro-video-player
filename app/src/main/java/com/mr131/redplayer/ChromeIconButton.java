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

/** A borderless, freestanding icon that preserves each asset's built-in 3D depth. */
public final class ChromeIconButton extends AppCompatImageButton {
    private float pulse;
    private boolean active;
    private ValueAnimator animator;
    private ThemePalette colors;

    public ChromeIconButton(Context c) { super(c); prepare(); }
    public ChromeIconButton(Context c, @Nullable AttributeSet a) { super(c,a); prepare(); }
    public ChromeIconButton(Context c, @Nullable AttributeSet a, int s) { super(c,a,s); prepare(); }

    private void prepare() {
        colors = ThemePalette.from(getContext());
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
        super.onDraw(canvas);
        Drawable icon=getDrawable();float colorPulse=Math.max(pulse,active?.18f:0f);
        if(icon==null||colorPulse<=0f)return;
        Drawable.Callback callback=icon.getCallback();icon.setCallback(null);
        icon.setColorFilter(colors.withAlpha(colors.orange,(int)(90*colorPulse)),PorterDuff.Mode.SRC_ATOP);
        icon.draw(canvas);icon.setColorFilter(null);icon.setCallback(callback);
    }

    @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
}
