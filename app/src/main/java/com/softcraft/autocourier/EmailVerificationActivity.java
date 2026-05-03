package com.softcraft.autocourier;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class EmailVerificationActivity extends AppCompatActivity {

    private TextView tvMessage, tvEmailSentTo;
    private Button btnRefreshStatus, btnResendEmail, btnLogout;
    private RelativeLayout loadingOverlay;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private Handler autoCheckHandler;
    private Runnable autoCheckRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        initViews();
        setupClickListeners();
        updateEmailDisplay();

        // Start auto-check every 5 seconds
        startAutoCheck();
    }

    private void initViews() {
        tvMessage = findViewById(R.id.tvMessage);
        tvEmailSentTo = findViewById(R.id.tvEmailSentTo);
        btnRefreshStatus = findViewById(R.id.btnRefreshStatus);
        btnResendEmail = findViewById(R.id.btnResendEmail);
        btnLogout = findViewById(R.id.btnLogout);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }

    private void updateEmailDisplay() {
        if (user != null && user.getEmail() != null) {
            tvEmailSentTo.setText("Verification sent to: " + user.getEmail());
        } else {
            tvEmailSentTo.setText("No email found.");
        }
    }

    private void setupClickListeners() {
        btnRefreshStatus.setOnClickListener(v -> checkEmailVerification());
        btnResendEmail.setOnClickListener(v -> resendVerificationEmail());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void checkEmailVerification() {
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        showLoading(true);
        user.reload().addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                if (user.isEmailVerified()) {
                    // Update Firestore isVerified field to true
                    updateFirestoreVerificationStatus();
                    Toast.makeText(EmailVerificationActivity.this, "Email verified! Redirecting...", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(EmailVerificationActivity.this, DashboardActivity.class));
                    finish();
                } else {
                    Toast.makeText(EmailVerificationActivity.this, "Email not verified yet. Please check your inbox.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(EmailVerificationActivity.this, "Failed to check verification status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFirestoreVerificationStatus() {
        // Optionally update Firestore field isVerified = true
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .update("isVerified", true)
                .addOnFailureListener(e -> e.printStackTrace());
    }

    private void resendVerificationEmail() {
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }
        showLoading(true);
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(EmailVerificationActivity.this, "Verification email resent. Check your inbox.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(EmailVerificationActivity.this, "Failed to resend: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(EmailVerificationActivity.this, LoginActivity.class));
        finish();
    }

    private void startAutoCheck() {
        autoCheckHandler = new Handler();
        autoCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (user != null && !user.isEmailVerified()) {
                    user.reload().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && user.isEmailVerified()) {
                            updateFirestoreVerificationStatus();
                            Toast.makeText(EmailVerificationActivity.this, "Email verified! Redirecting...", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(EmailVerificationActivity.this, DashboardActivity.class));
                            finish();
                        } else {
                            // Schedule next check
                            autoCheckHandler.postDelayed(this, 5000);
                        }
                    });
                } else if (user != null && user.isEmailVerified()) {
                    // Already verified, go to dashboard
                    startActivity(new Intent(EmailVerificationActivity.this, DashboardActivity.class));
                    finish();
                } else {
                    // user null, logout
                    logout();
                }
            }
        };
        autoCheckHandler.postDelayed(autoCheckRunnable, 5000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoCheckHandler != null && autoCheckRunnable != null) {
            autoCheckHandler.removeCallbacks(autoCheckRunnable);
        }
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRefreshStatus.setEnabled(!show);
        btnResendEmail.setEnabled(!show);
        btnLogout.setEnabled(!show);
    }
}