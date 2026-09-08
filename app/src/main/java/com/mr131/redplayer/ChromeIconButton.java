package com.mr131.redplayer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
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
    private final Paint metalPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint depthPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Matrix metalMotion=new Matrix();
    private Bitmap iconMask;
    private LinearGradient metal;

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
        ensureMask(icon);
        if(iconMask==null)return;

        depthPaint.setShadowLayer(5f,2f,4f,Color.BLACK);
        for(int layer=8;layer>=1;layer--){
            depthPaint.setColor(Color.rgb(58+layer*8,2,2));
            canvas.drawBitmap(iconMask,layer*.78f,layer*.78f,depthPaint);
        }
        depthPaint.clearShadowLayer();
        depthPaint.setColor(Color.rgb(8,8,10));
        canvas.drawBitmap(iconMask,-2.1f,0,depthPaint);canvas.drawBitmap(iconMask,2.1f,0,depthPaint);
        canvas.drawBitmap(iconMask,0,-2.1f,depthPaint);canvas.drawBitmap(iconMask,0,2.1f,depthPaint);

        float travel=(pulse+(active?.28f:0f))*getWidth()*.55f;
        metalMotion.setTranslate(travel,0f);metal.setLocalMatrix(metalMotion);
        metalPaint.setShader(metal);
        canvas.drawBitmap(iconMask,0,0,metalPaint);
    }

    private void ensureMask(Drawable icon){
        if(iconMask!=null&&iconMask.getWidth()==getWidth()&&iconMask.getHeight()==getHeight())return;
        if(getWidth()<=0||getHeight()<=0)return;
        Bitmap colorMask=Bitmap.createBitmap(getWidth(),getHeight(),Bitmap.Config.ARGB_8888);
        Canvas maskCanvas=new Canvas(colorMask);Drawable.Callback callback=icon.getCallback();icon.setCallback(null);icon.setColorFilter(Color.WHITE,PorterDuff.Mode.SRC_IN);icon.draw(maskCanvas);icon.setColorFilter(null);icon.setCallback(callback);
        iconMask=colorMask.extractAlpha();colorMask.recycle();
        metal=new LinearGradient(-getWidth()*.35f,0,getWidth()*.85f,getHeight(),new int[]{Color.rgb(55,57,62),Color.WHITE,Color.rgb(151,156,164),Color.rgb(20,21,24),Color.rgb(245,247,250),Color.rgb(214,10,0),Color.WHITE},new float[]{0f,.14f,.30f,.45f,.60f,.73f,1f},Shader.TileMode.MIRROR);
    }

    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);iconMask=null;metal=null;}

    @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
}
