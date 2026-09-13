package com.mr131.redplayer;

import android.content.Context;
import android.content.res.Configuration;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

/** One source of truth for the warm, blue-free Red Player color system. */
public final class ThemePalette {
    public final boolean night;
    @ColorInt public final int background, surface, raisedSurface, strongSurface;
    @ColorInt public final int textPrimary, textSecondary;
    @ColorInt public final int red, orange, yellow, lime, purple, cream, shadow, highlight;
    private final int[] accents;

    private ThemePalette(Context context) {
        night = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        background = themed(context, R.attr.redPlayerBackgroundColor, R.color.app_background);
        surface = themed(context, R.attr.redPlayerSurfaceColor, R.color.surface);
        raisedSurface = themed(context, R.attr.redPlayerRaisedSurfaceColor, R.color.surface_raised);
        strongSurface = themed(context, R.attr.redPlayerStrongSurfaceColor, R.color.surface_strong);
        textPrimary = themed(context, R.attr.redPlayerTextPrimaryColor, R.color.text_primary);
        textSecondary = themed(context, R.attr.redPlayerTextSecondaryColor, R.color.text_secondary);
        shadow = themed(context, R.attr.redPlayerWarmShadowColor, R.color.warm_shadow);
        highlight = themed(context, R.attr.redPlayerPatternHighlightColor, R.color.pattern_highlight);
        red = ContextCompat.getColor(context, R.color.red_player);
        orange = ContextCompat.getColor(context, R.color.accent_orange);
        yellow = ContextCompat.getColor(context, R.color.accent_yellow);
        lime = ContextCompat.getColor(context, R.color.accent_lime);
        purple = ContextCompat.getColor(context, R.color.accent_purple);
        cream = ContextCompat.getColor(context, R.color.accent_cream);
        accents = new int[]{red, orange, yellow, lime, purple, cream};
    }

    public static ThemePalette from(Context context) { return new ThemePalette(context); }

    @ColorInt public int accentFor(Object seed) {
        int hash = seed == null ? 0 : seed.hashCode();
        return accents[(hash & Integer.MAX_VALUE) % accents.length];
    }

    @ColorInt public int claySide(@ColorInt int face) {
        return ColorUtils.blendARGB(face, shadow, night ? .62f : .48f);
    }

    @ColorInt public int withAlpha(@ColorInt int color, int alpha) {
        return ColorUtils.setAlphaComponent(color, Math.max(0, Math.min(255, alpha)));
    }

    @ColorInt private static int themed(Context context, @AttrRes int attribute, @ColorRes int fallback) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attribute, value, true)) {
            if (value.resourceId != 0) return ContextCompat.getColor(context, value.resourceId);
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) return value.data;
        }
        return ContextCompat.getColor(context, fallback);
    }
}
