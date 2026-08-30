package com.example.musicplayer;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {
    public static final int SYSTEM = 0;
    public static final int LIGHT = 1;
    public static final int DARK = 2;
    private static final String PREFS = "appearance";
    private static final String KEY_THEME = "theme";

    private ThemeManager() {}

    public static void applySavedTheme(Context context) {
        applyMode(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_THEME, SYSTEM));
    }

    public static int getSavedTheme(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_THEME, SYSTEM);
    }

    public static void setTheme(Context context, int mode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_THEME, mode).apply();
        applyMode(mode);
    }

    private static void applyMode(int mode) {
        int nightMode = mode == LIGHT
                ? AppCompatDelegate.MODE_NIGHT_NO
                : mode == DARK
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}
