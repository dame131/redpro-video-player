package com.mr131.redplayer;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** A real drawn chrome control surface with a crimson energy pulse. */
public final class ChromePulseDrawable extends Drawable {
    private static final int CRIMSON = Color.rgb(214, 10, 0);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private float pulse;
    private boolean active;
    private ValueAnimator animator;

    public ChromePulseDrawable(boolean active) {
        this.active = active;
    }

    @Override public void draw(@NonNull Canvas canvas) {
        float w = getBounds().width();
        float h = getBounds().height();
        float cx = getBounds().left + w / 2f;
        float cy = getBounds().top + h / 2f;
        float r = Math.min(w, h) * .43f;

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        paint.setColor(Color.argb(170, 0, 0, 0));
        canvas.drawCircle(cx, cy + r * .14f, r * 1.08f, paint);

        if (active || pulse > .02f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(3f, r * .10f));
            paint.setColor(Color.argb((int) (70 + 150 * pulse), 255, 18, 5));
            canvas.drawCircle(cx, cy, r * (1.03f + .14f * pulse), paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(cx - r, cy - r, cx + r, cy + r,
                new int[]{Color.rgb(245,245,248), Color.rgb(120,124,130), Color.rgb(25,26,29), Color.rgb(220,222,226)},
                new float[]{0f,.34f,.68f,1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, paint);

        paint.setShader(new LinearGradient(cx, cy-r*.72f, cx, cy+r*.72f,
                new int[]{Color.rgb(52,54,58), Color.rgb(5,5,6), Color.rgb(25,26,29)},
                null, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r * .77f, paint);

        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(2f, r * .075f));
        paint.setColor(Color.argb(active ? 255 : 210, 214, 10, 0));
        oval.set(cx-r*.88f, cy-r*.88f, cx+r*.88f, cy+r*.88f);
        canvas.drawArc(oval, 24f, active ? 245f : 118f, false, paint);

        paint.setStrokeWidth(Math.max(1f, r * .035f));
        paint.setColor(Color.argb(185,255,255,255));
        oval.set(cx-r*.70f, cy-r*.70f, cx+r*.70f, cy+r*.70f);
        canvas.drawArc(oval, 205f, 98f, false, paint);
        paint.setShader(null);
    }

    public void setActive(boolean value) { active = value; invalidateSelf(); }

    public void pulse() {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, 1f, 0f);
        animator.setDuration(620);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> { pulse = (float) a.getAnimatedValue(); invalidateSelf(); });
        animator.start();
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(@Nullable android.graphics.ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
}
