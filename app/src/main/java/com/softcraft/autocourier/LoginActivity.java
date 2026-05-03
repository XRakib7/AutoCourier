package com.softcraft.autocourier;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore; // Added Firestore Import

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoogleSignIn;
    private TextView tvForgotPassword, tvSignupLink;
    private RelativeLayout loadingOverlay;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore; // Added Firestore instance
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance(); // Initialized Firestore

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initViews();
        setClickListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignupLink = findViewById(R.id.tvSignupLink);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }

    private void setClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        tvForgotPassword.setOnClickListener(v -> {
            // Open ForgotPasswordActivity
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
        tvSignupLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            finish();
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password required");
            return;
        }

        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && !user.isEmailVerified()) {
                                // Email not verified, redirect to verification screen
                                Toast.makeText(LoginActivity.this, "Please verify your email first", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(LoginActivity.this, EmailVerificationActivity.class));
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                finish();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void signInWithGoogle() {
        showLoading(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                showLoading(false);
                Toast.makeText(this, "Google Sign-In Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            checkAndRedirectAfterGoogleSignIn(user); // Updated to check Firestore
                        } else {
                            Toast.makeText(LoginActivity.this, "Google Sign-In Authentication Failed", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Added method to handle Firestore check and redirection
    private void checkAndRedirectAfterGoogleSignIn(FirebaseUser user) {
        if (user != null) {
            firestore.collection("users").document(user.getUid()).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            // User exists, go to dashboard
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();
                        } else {
                            // User does not exist, need to complete signup
                            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                            intent.putExtra("google_name", user.getDisplayName());
                            intent.putExtra("google_email", user.getEmail());
                            startActivity(intent);
                            finish();
                        }
                    });
        }
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            // Disable all input fields and buttons implicitly by overlay blocking touches
            // Also prevent back button press if needed (optional)
        }
    }

    @Override
    public void onBackPressed() {
        if (loadingOverlay.getVisibility() == View.VISIBLE) {
            // Optionally prevent back while loading
            return;
        }
        super.onBackPressed();
    }
}