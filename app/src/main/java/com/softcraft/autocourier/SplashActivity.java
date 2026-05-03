package com.softcraft.autocourier;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

public class SplashActivity extends AppCompatActivity {

    private ImageView logo;
    private TextView appName;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        logo = findViewById(R.id.logo);
        appName = findViewById(R.id.appName);
        progressBar = findViewById(R.id.progressBar);
        mAuth = FirebaseAuth.getInstance();

        // Start fade-in animation on logo and text
        startLogoAnimation();
    }

    private void startLogoAnimation() {
        ObjectAnimator fadeInLogo = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);
        fadeInLogo.setDuration(800);
        fadeInLogo.start();

        ObjectAnimator fadeInText = ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f);
        fadeInText.setDuration(800);
        fadeInText.start();

        // After animation, check auth state
        new Handler().postDelayed(this::checkAuthState, 1500);
    }

    private void checkAuthState() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // No user signed in, go to welcome screen
            goToWelcomeScreen();
            return;
        }

        // User exists, validate token by refreshing it
        progressBar.setVisibility(View.VISIBLE);
        currentUser.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
            @Override
            public void onComplete(@NonNull Task<GetTokenResult> task) {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    // Token valid
                    // Check if email is verified (for email/password users)
                    // For now, just proceed to dashboard or verification screen later
                    goToDashboard();
                } else {
                    // Token invalid or error
                    Toast.makeText(SplashActivity.this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    goToWelcomeScreen();
                }
            }
        });
    }

    private void goToWelcomeScreen() {
        // Check if first launch
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("isFirstLaunch", true);
        if (isFirstLaunch) {
            Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
        }
        finish();
    }

    private void goToDashboard() {
        // Later we'll add email verification check
        Intent intent = new Intent(SplashActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}