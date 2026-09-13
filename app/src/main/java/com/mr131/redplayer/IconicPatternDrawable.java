package com.mr131.redplayer;

import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Warm daylight or burgundy-night background shared by every Red Player screen. */
public final class IconicPatternDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final ThemePalette colors;
    private final float density;
    private int drawableAlpha = 255;

    public IconicPatternDrawable(Context context) {
        colors = ThemePalette.from(context);
        density = context.getResources().getDisplayMetrics().density;
    }

    @Override public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        float width = bounds.width();
        float height = bounds.height();
        int farCorner = colors.night ? colors.strongSurface : colors.surface;

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, width, height,
                new int[]{colors.background, colors.raisedSurface, colors.surface, farCorner},
                new float[]{0f, .34f, .72f, 1f}, Shader.TileMode.CLAMP));
        paint.setAlpha(drawableAlpha);
        canvas.drawRect(bounds, paint);
        paint.setShader(null);

        // Soft diagonal bands add depth without competing with media or text.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dp(colors.night ? 18f : 24f));
        paint.setColor(alpha(colors.red, colors.night ? 24 : 18));
        canvas.drawLine(-width * .15f, height * .31f, width * 1.12f, height * .08f, paint);
        paint.setStrokeWidth(dp(colors.night ? 12f : 17f));
        paint.setColor(alpha(colors.orange, colors.night ? 20 : 24));
        canvas.drawLine(-width * .10f, height * .75f, width * 1.14f, height * .49f, paint);
        paint.setStrokeWidth(dp(8f));
        paint.setColor(alpha(colors.purple, colors.night ? 15 : 13));
        canvas.drawLine(width * .18f, height, width * 1.05f, height * .68f, paint);

        // Sparse clay-color diamonds echo the new freestanding icon family.
        int[] accents = {colors.red, colors.orange, colors.yellow, colors.lime, colors.purple};
        for (int row = 0; row < 8; row++) {
            float y = height * (.10f + row * .12f);
            float x = row % 2 == 0 ? width * .91f : width * .09f;
            drawDiamond(canvas, x, y, dp(row % 3 == 0 ? 5.5f : 3.8f), accents[row % accents.length]);
        }
    }

    private void drawDiamond(Canvas canvas, float x, float y, float size, int color) {
        path.reset();
        path.moveTo(x, y - size);
        path.lineTo(x + size, y);
        path.lineTo(x, y + size);
        path.lineTo(x - size, y);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(alpha(color, colors.night ? 76 : 66));
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1f));
        paint.setColor(alpha(colors.highlight, colors.night ? 100 : 145));
        canvas.drawPath(path, paint);
    }

    private int alpha(int color, int alpha) {
        return Color.argb(alpha * drawableAlpha / 255, Color.red(color), Color.green(color), Color.blue(color));
    }

    private float dp(float value) { return value * density; }

    @Override public void setAlpha(int alpha) {
        drawableAlpha = Math.max(0, Math.min(255, alpha));
        invalidateSelf();
    }

    @Override public void setColorFilter(@Nullable ColorFilter filter) {
        paint.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
