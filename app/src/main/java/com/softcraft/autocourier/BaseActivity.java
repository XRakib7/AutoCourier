package com.softcraft.autocourier;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Apply theme before super.onCreate
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
        super.onCreate(savedInstanceState);
    }
}