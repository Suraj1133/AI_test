package com.example.musicplayer;

import android.app.Application;

public class MusicPlayerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applySavedTheme(this);
    }
}
