package com.example.musicplayer;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {
    public static final int SYSTEM = 0;
    public static final int LIGHT = 1;
    public static final int DARK = 2;

    public static final int LAVENDER = 0;
    public static final int OCEAN = 1;
    public static final int EMERALD = 2;
    public static final int SUNSET = 3;
    public static final int ROSE = 4;

    private static final String PREFS = "appearance";
    private static final String KEY_THEME = "theme";
    private static final String KEY_COLOR = "color";

    private ThemeManager() {}

    public static void applySavedTheme(Context context) {
        applyMode(getSavedTheme(context));
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

    public static int getSavedColor(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_COLOR, LAVENDER);
    }

    public static void setColor(Context context, int color) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_COLOR, color).apply();
    }

    public static int getActivityTheme(Context context) {
        switch (getSavedColor(context)) {
            case OCEAN: return R.style.Theme_MusicPlayer_Ocean;
            case EMERALD: return R.style.Theme_MusicPlayer_Emerald;
            case SUNSET: return R.style.Theme_MusicPlayer_Sunset;
            case ROSE: return R.style.Theme_MusicPlayer_Rose;
            default: return R.style.Theme_MusicPlayer_Lavender;
        }
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
