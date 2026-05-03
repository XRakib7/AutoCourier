package com.softcraft.autocourier;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSendReset, btnBackToLogin;
    private RelativeLayout loadingOverlay;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        btnSendReset = findViewById(R.id.btnSendReset);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        btnSendReset.setOnClickListener(v -> sendResetEmail());
        btnBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void sendResetEmail() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email required");
            return;
        }

        showLoading(true);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Reset email sent. Check your inbox.", Toast.LENGTH_LONG).show();
                        // Go back to login after a short delay
                        new android.os.Handler().postDelayed(() -> {
                            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                            finish();
                        }, 2000);
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSendReset.setEnabled(!show);
        btnBackToLogin.setEnabled(!show);
    }
}