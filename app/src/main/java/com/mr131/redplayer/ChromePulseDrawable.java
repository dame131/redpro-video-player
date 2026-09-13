package com.mr131.redplayer;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Borderless press/active treatment for freestanding 3D icons. */
public final class ChromePulseDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF line = new RectF();
    private float pulse;
    private boolean active;
    private ValueAnimator animator;

    public ChromePulseDrawable(boolean active) {
        this.active = active;
    }

    @Override public void draw(@NonNull Canvas canvas) {
        float w = getBounds().width();
        float h = getBounds().height();
        float left = getBounds().left + w * .19f;
        float right = getBounds().right - w * .19f;
        float bottom = getBounds().bottom - Math.max(2f, h * .055f);
        float weight = Math.max(3f, h * .065f);
        float intensity = active ? Math.max(.42f, pulse) : pulse;
        if (intensity <= .01f) return;

        // A warm underline supplies state feedback without putting a circle behind the icon.
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.argb((int) (80 + 155 * intensity), 255, 73, 51));
        line.set(left, bottom - weight, right, bottom + weight);
        canvas.drawRoundRect(line, weight, weight, paint);
        paint.setColor(Color.argb((int) (45 + 105 * intensity), 255, 179, 50));
        line.set(left + weight, bottom - weight * .35f, right - weight, bottom + weight * .35f);
        canvas.drawRoundRect(line, weight, weight, paint);
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
