package com.mr131.redplayer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/** Extra-thick warm clay lettering with a restrained animated highlight. */
public final class ChromePulseTextView extends AppCompatTextView {
    private final Matrix chromeMotion = new Matrix();
    private LinearGradient chrome;
    private ValueAnimator pulse;
    private ThemePalette colors;

    public ChromePulseTextView(Context context) { super(context); prepare(); }
    public ChromePulseTextView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); prepare(); }
    public ChromePulseTextView(Context context, @Nullable AttributeSet attrs, int style) { super(context, attrs, style); prepare(); }

    private void prepare() {
        colors = ThemePalette.from(getContext());
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setShadowLayer(dp(2.2f), dp(1.1f), dp(1.5f), colors.withAlpha(colors.shadow, 185));
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0) return;
        int warmFace = colors.night ? colors.cream : colors.textPrimary;
        chrome = new LinearGradient(-w * .45f, 0, w * .55f, 0,
                new int[]{colors.red, colors.orange, colors.yellow, warmFace, colors.orange, colors.red},
                new float[]{0f, .18f, .38f, .56f, .76f, 1f}, Shader.TileMode.MIRROR);
        getPaint().setShader(chrome);
        startPulse(w);
    }

    private void startPulse(int width) {
        if(!ValueAnimator.areAnimatorsEnabled()||isEmulator()){chromeMotion.setTranslate(width*.42f,0f);chrome.setLocalMatrix(chromeMotion);invalidate();return;}
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

    private boolean isEmulator(){String model=android.os.Build.MODEL.toLowerCase();String hardware=android.os.Build.HARDWARE.toLowerCase();String product=android.os.Build.PRODUCT.toLowerCase();return model.contains("sdk")||model.contains("emulator")||hardware.contains("ranchu")||hardware.contains("goldfish")||product.contains("sdk")||product.contains("emulator");}

    @Override protected void onDraw(Canvas canvas) {
        Shader face = getPaint().getShader();
        int faceColor = getCurrentTextColor();
        Paint.Style faceStyle = getPaint().getStyle();

        getPaint().setShader(null);
        getPaint().clearShadowLayer();
        getPaint().setStyle(Paint.Style.FILL);
        for (int layer = 8; layer >= 1; layer--) {
            getPaint().setColor(colors.claySide(colors.red));
            canvas.save();
            canvas.translate(dp(layer * .42f), dp(layer * .42f));
            super.onDraw(canvas);
            canvas.restore();
        }

        getPaint().setShader(null);
        getPaint().setColor(colors.shadow);
        getPaint().setStyle(Paint.Style.STROKE);
        getPaint().setStrokeWidth(dp(1.25f));
        super.onDraw(canvas);

        getPaint().setStyle(faceStyle);
        getPaint().setColor(faceColor);
        getPaint().setShader(face);
        getPaint().setShadowLayer(dp(2.2f), dp(1.1f), dp(1.5f), colors.withAlpha(colors.shadow, 185));
        super.onDraw(canvas);
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }

    @Override protected void onDetachedFromWindow() {
        if (pulse != null) pulse.cancel();
        super.onDetachedFromWindow();
    }
}
