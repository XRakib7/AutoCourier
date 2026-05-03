package com.softcraft.autocourier;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class WelcomeActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnSkip, btnNext, btnGetStarted;
    private LinearLayout dotsContainer;
    private IntroSliderAdapter adapter;
    private List<IntroSlide> slides;
    private int currentPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Check if first launch
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("isFirstLaunch", true);
        if (!isFirstLaunch) {
            // Already shown, go to login
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupSlides();
        setupViewPager();
        setupClickListeners();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        btnSkip = findViewById(R.id.btnSkip);
        btnNext = findViewById(R.id.btnNext);
        btnGetStarted = findViewById(R.id.btnGetStarted);
        dotsContainer = findViewById(R.id.dotsContainer);
    }

    private void setupSlides() {
        slides = new ArrayList<>();
        slides.add(new IntroSlide(R.drawable.ic_intro_delivery,
                "Fast Delivery",
                "Get your packages delivered quickly and reliably with Auto Courier."));
        slides.add(new IntroSlide(R.drawable.ic_intro_tracking,
                "Real-Time Tracking",
                "Track your orders in real-time from pickup to delivery."));
        slides.add(new IntroSlide(R.drawable.ic_intro_support,
                "24/7 Support",
                "Our support team is always here to help you with any issues."));
    }

    private void setupViewPager() {
        adapter = new IntroSliderAdapter(slides);
        viewPager.setAdapter(adapter);

        // Add dots
        updateDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                updateDots(position);

                // Show/hide Get Started button on last slide
                if (position == slides.size() - 1) {
                    btnNext.setVisibility(View.GONE);
                    btnGetStarted.setVisibility(View.VISIBLE);
                } else {
                    btnNext.setVisibility(View.VISIBLE);
                    btnGetStarted.setVisibility(View.GONE);
                }
            }
        });
    }

    private void updateDots(int position) {
        dotsContainer.removeAllViews();
        for (int i = 0; i < slides.size(); i++) {
            View dot = new View(this);
            int size = getResources().getDimensionPixelSize(R.dimen.dot_size);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == position ? R.drawable.dot_selected : R.drawable.dot_unselected);
            dotsContainer.addView(dot);
        }
    }

    private void setupClickListeners() {
        btnSkip.setOnClickListener(v -> finishWelcomeAndGoToLogin());
        btnNext.setOnClickListener(v -> {
            if (currentPosition < slides.size() - 1) {
                viewPager.setCurrentItem(currentPosition + 1);
            }
        });
        btnGetStarted.setOnClickListener(v -> finishWelcomeAndGoToLogin());
    }

    private void finishWelcomeAndGoToLogin() {
        // Mark first launch as false
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("isFirstLaunch", false).apply();

        startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        finish();
    }
}