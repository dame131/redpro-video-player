package com.mr131.redplayer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Shared carbon-and-color background used across every Red Player screen. */
public final class IconicPatternDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        float w = b.width();
        float h = b.height();

        paint.setShader(new LinearGradient(0, 0, w, h,
                new int[]{Color.rgb(3,3,4), Color.rgb(17,9,12), Color.rgb(5,3,9), Color.BLACK},
                new float[]{0f,.34f,.68f,1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(b, paint);
        paint.setShader(null);

        // Quiet carbon weave: visible enough to feel designed, dark enough for white type.
        paint.setStrokeWidth(dp(1.2f));
        for (float x = -h; x < w; x += dp(24)) {
            paint.setColor(Color.argb(35, 255, 255, 255));
            canvas.drawLine(x, 0, x + h, h, paint);
            paint.setColor(Color.argb(24, 214, 10, 0));
            canvas.drawLine(x + dp(8), 0, x + h + dp(8), h, paint);
        }

        // Large automotive-emblem sweeps.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dp(3));
        paint.setColor(Color.argb(100, 214, 10, 0));
        canvas.drawArc(-w*.28f, h*.04f, w*.78f, h*.47f, -55, 225, false, paint);
        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.argb(120, 255, 188, 54));
        canvas.drawArc(-w*.24f, h*.06f, w*.74f, h*.45f, -55, 225, false, paint);
        paint.setStrokeWidth(dp(4));
        paint.setColor(Color.argb(72, 171, 45, 255));
        canvas.drawArc(w*.48f, h*.48f, w*1.34f, h*.82f, 120, 205, false, paint);
        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.argb(105, 232, 232, 238));
        canvas.drawArc(w*.51f, h*.49f, w*1.31f, h*.80f, 120, 205, false, paint);

        // Small chrome badge diamonds repeat down the page.
        paint.setStyle(Paint.Style.FILL);
        for (int row = 0; row < 9; row++) {
            float cy = h * (.10f + row * .105f);
            float cx = (row % 2 == 0) ? w*.88f : w*.12f;
            drawDiamond(canvas, cx, cy, dp(6), row % 3);
        }
    }

    private void drawDiamond(Canvas canvas, float cx, float cy, float size, int colorIndex) {
        path.reset();
        path.moveTo(cx, cy-size); path.lineTo(cx+size, cy);
        path.lineTo(cx, cy+size); path.lineTo(cx-size, cy); path.close();
        int[] colors = {Color.rgb(214,10,0), Color.rgb(255,188,54), Color.rgb(171,45,255)};
        paint.setColor(Color.argb(125, Color.red(colors[colorIndex]), Color.green(colors[colorIndex]), Color.blue(colors[colorIndex])));
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1)); paint.setColor(Color.argb(150, 240,240,245));
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private float dp(float value) { return value * getCallbackDensity(); }
    private float getCallbackDensity() {
        return android.content.res.Resources.getSystem().getDisplayMetrics().density;
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
