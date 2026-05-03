package com.softcraft.autocourier;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvUserEmail;
    private TextView tvTotalOrders, tvSuccessRate, tvTotalRevenue, tvTotalCOD;
    private Button btnProfile, btnSettings, btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews();
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvSuccessRate = findViewById(R.id.tvSuccessRate);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalCOD = findViewById(R.id.tvTotalCOD);
        btnProfile = findViewById(R.id.btnProfile);
        btnSettings = findViewById(R.id.btnSettings);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void loadUserData() {
        if (currentUser == null) {
            logout();
            return;
        }

        // Display name and email
        String displayName = currentUser.getDisplayName();
        String email = currentUser.getEmail();
        tvWelcome.setText("Welcome, " + (displayName != null ? displayName : "User") + "!");
        tvUserEmail.setText(email);

        // Load stats from Firestore
        firestore.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> stats = (Map<String, Object>) documentSnapshot.get("stats");
                        if (stats != null) {
                            Object orders = stats.get("totalOrders");
                            Object rate = stats.get("successRate");
                            Object revenue = stats.get("totalRevenue");
                            Object cod = stats.get("totalCOD");

                            tvTotalOrders.setText(String.valueOf(orders != null ? orders : 0));
                            tvSuccessRate.setText(String.valueOf(rate != null ? rate : 0) + "%");
                            tvTotalRevenue.setText("$" + (revenue != null ? revenue : 0));
                            tvTotalCOD.setText("$" + (cod != null ? cod : 0));
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load stats", Toast.LENGTH_SHORT).show());
    }

    private void setupClickListeners() {
        btnProfile.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, ProfileActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, SettingsActivity.class)));
        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
        finish();
    }
}