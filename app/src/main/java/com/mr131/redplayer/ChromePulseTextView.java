package com.mr131.redplayer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/** Chrome letters with a moving crimson highlight. */
public final class ChromePulseTextView extends AppCompatTextView {
    private final Matrix chromeMotion = new Matrix();
    private LinearGradient chrome;
    private ValueAnimator pulse;

    public ChromePulseTextView(Context context) { super(context); prepare(); }
    public ChromePulseTextView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); prepare(); }
    public ChromePulseTextView(Context context, @Nullable AttributeSet attrs, int style) { super(context, attrs, style); prepare(); }

    private void prepare() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setShadowLayer(4.5f, 1.5f, 2.5f, Color.rgb(95, 0, 0));
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0) return;
        chrome = new LinearGradient(-w * .45f, 0, w * .55f, 0,
                new int[]{Color.rgb(105,108,114), Color.WHITE, Color.rgb(196,199,204), Color.rgb(214,10,0), Color.WHITE, Color.rgb(92,94,99)},
                new float[]{0f,.17f,.38f,.53f,.70f,1f}, Shader.TileMode.MIRROR);
        getPaint().setShader(chrome);
        startPulse(w);
    }

    private void startPulse(int width) {
        if (pulse != null) pulse.cancel();
        pulse = ValueAnimator.ofFloat(0f, width * 1.35f);
        pulse.setDuration(2200);
        pulse.setStartDelay(220);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        pulse.addUpdateListener(a -> {
            if (chrome == null) return;
            chromeMotion.setTranslate((float) a.getAnimatedValue(), 0f);
            chrome.setLocalMatrix(chromeMotion);
            invalidate();
        });
        pulse.start();
    }

    @Override protected void onDraw(Canvas canvas) {
        Shader face = getPaint().getShader();
        int faceColor = getCurrentTextColor();
        Paint.Style faceStyle = getPaint().getStyle();

        getPaint().setShader(null);
        getPaint().clearShadowLayer();
        getPaint().setStyle(Paint.Style.FILL);
        for (int layer = 6; layer >= 1; layer--) {
            int red = 38 + layer * 7;
            getPaint().setColor(Color.rgb(red, 3, 2));
            canvas.save();
            canvas.translate(layer * .85f, layer * .85f);
            super.onDraw(canvas);
            canvas.restore();
        }

        getPaint().setShader(null);
        getPaint().setColor(Color.rgb(12, 12, 14));
        getPaint().setStyle(Paint.Style.STROKE);
        getPaint().setStrokeWidth(3.2f);
        super.onDraw(canvas);

        getPaint().setStyle(faceStyle);
        getPaint().setColor(faceColor);
        getPaint().setShader(face);
        getPaint().setShadowLayer(4.5f, 1.5f, 2.5f, Color.rgb(95, 0, 0));
        super.onDraw(canvas);
    }

    @Override protected void onDetachedFromWindow() {
        if (pulse != null) pulse.cancel();
        super.onDetachedFromWindow();
    }
}
