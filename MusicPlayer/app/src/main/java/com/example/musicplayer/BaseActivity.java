package com.example.musicplayer;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(ThemeManager.getActivityTheme(this));
        super.onCreate(savedInstanceState);
    }
}
