package com.softcraft.autocourier;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private Button btnLogoutSettings, btnBackToDashboardSettings;
    private RadioGroup themeRadioGroup;
    private SwitchCompat notifSwitch;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize SharedPreferences FIRST
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);

        // Now apply saved theme before inflating layout
        applyThemeFromPreference();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();

        btnLogoutSettings = findViewById(R.id.btnLogoutSettings);
        btnBackToDashboardSettings = findViewById(R.id.btnBackToDashboardSettings);
        themeRadioGroup = findViewById(R.id.themeRadioGroup);
        notifSwitch = findViewById(R.id.notifSwitch);

        // Load saved notification preference
        boolean notifEnabled = sharedPreferences.getBoolean("notifications", true);
        notifSwitch.setChecked(notifEnabled);

        // Load saved theme preference and check appropriate radio button
        int savedTheme = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        checkThemeRadioButton(savedTheme);

        // Theme change listener
        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int newMode;
            if (checkedId == R.id.themeLight) {
                newMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.themeDark) {
                newMode = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                newMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }
            saveThemePreference(newMode);
            AppCompatDelegate.setDefaultNightMode(newMode);
            // Recreate activity to apply theme changes
            recreate();
        });

        // Notifications switch listener
        notifSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("notifications", isChecked);
            editor.apply();
            Toast.makeText(this, isChecked ? "Notifications enabled" : "Notifications disabled", Toast.LENGTH_SHORT).show();
        });

        btnLogoutSettings.setOnClickListener(v -> logout());
        btnBackToDashboardSettings.setOnClickListener(v -> finish());
    }

    private void applyThemeFromPreference() {
        int themeMode = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    private void saveThemePreference(int mode) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("theme_mode", mode);
        editor.apply();
    }

    private void checkThemeRadioButton(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            themeRadioGroup.check(R.id.themeLight);
        } else if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            themeRadioGroup.check(R.id.themeDark);
        } else {
            themeRadioGroup.check(R.id.themeSystem);
        }
    }

    private void logout() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
        finish();
    }
}