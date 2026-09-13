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

/** A borderless, freestanding icon with an extra-thick warm clay extrusion. */
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
        Drawable icon=getDrawable();
        if(icon==null){super.onDraw(canvas);return;}
        if("raw_asset".equals(String.valueOf(getTag()))) {
            Drawable.Callback callback=icon.getCallback();icon.setCallback(null);
            int side=colors.claySide(colors.accentFor(getContentDescription()));
            for(int layer=9;layer>=1;layer--){
                icon.setColorFilter(side,PorterDuff.Mode.SRC_IN);
                canvas.save();canvas.translate(dp(layer*.52f),dp(layer*.52f));icon.draw(canvas);canvas.restore();
            }
            icon.setColorFilter(null);icon.setCallback(callback);
            super.onDraw(canvas);
            float colorPulse=Math.max(pulse,active?.24f:0f);
            if(colorPulse>0f){
                callback=icon.getCallback();icon.setCallback(null);
                icon.setColorFilter(colors.withAlpha(colors.orange,(int)(145*colorPulse)),PorterDuff.Mode.SRC_ATOP);
                icon.draw(canvas);icon.setColorFilter(null);icon.setCallback(callback);
            }
            return;
        }
        Drawable.Callback callback=icon.getCallback();icon.setCallback(null);
        int face=colors.accentFor(getContentDescription());
        int side=colors.claySide(face);
        for(int layer=10;layer>=1;layer--){icon.setColorFilter(side,PorterDuff.Mode.SRC_IN);canvas.save();canvas.translate(dp(layer*.5f),dp(layer*.5f));icon.draw(canvas);canvas.restore();}
        icon.setColorFilter(colors.shadow,PorterDuff.Mode.SRC_IN);
        canvas.save();canvas.translate(-dp(.75f),-dp(.45f));icon.draw(canvas);canvas.restore();
        int liveFace=androidx.core.graphics.ColorUtils.blendARGB(face,colors.orange,Math.max(pulse,active?.18f:0f));
        icon.setColorFilter(liveFace,PorterDuff.Mode.SRC_IN);icon.draw(canvas);
        icon.setColorFilter(colors.withAlpha(colors.highlight,145),PorterDuff.Mode.SRC_IN);canvas.save();canvas.translate(-dp(.38f),-dp(.38f));icon.draw(canvas);canvas.restore();
        icon.setColorFilter(null);icon.setCallback(callback);
    }

    private float dp(float value){return value*getResources().getDisplayMetrics().density;}

    @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
}
