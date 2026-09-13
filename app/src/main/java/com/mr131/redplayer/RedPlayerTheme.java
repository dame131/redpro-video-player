package com.mr131.redplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

/** Persists and applies System, Bright, or Night appearance without changing playback state. */
public final class RedPlayerTheme {
    public static final String MODE_SYSTEM = "system";
    public static final String MODE_BRIGHT = "bright";
    public static final String MODE_NIGHT = "night";
    public static final String PREFERENCE_KEY = "appearance_mode";
    private static final String PREFERENCES = "red_player";

    private RedPlayerTheme() {}

    public static void applySavedMode(Context context) { applyMode(readMode(context)); }

    public static String readMode(Context context) {
        String mode = preferences(context).getString(PREFERENCE_KEY, MODE_SYSTEM);
        if (MODE_BRIGHT.equals(mode) || MODE_NIGHT.equals(mode)) return mode;
        return MODE_SYSTEM;
    }

    public static void saveAndApply(Context context, String mode) {
        String safeMode = MODE_BRIGHT.equals(mode) || MODE_NIGHT.equals(mode) ? mode : MODE_SYSTEM;
        preferences(context).edit().putString(PREFERENCE_KEY, safeMode).apply();
        applyMode(safeMode);
    }

    public static boolean isNight(Context context) {
        return (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private static void applyMode(String mode) {
        int delegateMode = MODE_BRIGHT.equals(mode) ? AppCompatDelegate.MODE_NIGHT_NO
                : MODE_NIGHT.equals(mode) ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (AppCompatDelegate.getDefaultNightMode() != delegateMode) AppCompatDelegate.setDefaultNightMode(delegateMode);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
}
