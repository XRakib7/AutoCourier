package com.softcraft.autocourier;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    // UI components
    private EditText etCustomerMessage;
    private Button btnExtractWithAI, btnViewOrders, btnProfile, btnSettings, btnLogout, btnClearMessage, btnSaveOrder;
    private ProgressBar progressBar;
    private CardView cardResult;
    private TextInputEditText etExtractedName, etExtractedPhone, etExtractedAddress, etExtractedProduct, etExtractedNote;
    private TextView tvWelcome, tvUserEmail, tvTotalOrders, tvSuccessRate, tvTotalRevenue, tvTotalCOD;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;
    private AiHelper aiHelper;
    private OrderRepository orderRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        aiHelper = new AiHelper(this);
        orderRepository = new OrderRepository();

        initViews();
        loadUserStats();
        setupClickListeners();
        loadAiConfig();
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
        btnViewOrders = findViewById(R.id.btnViewOrders);

        etCustomerMessage = findViewById(R.id.etCustomerMessage);
        btnExtractWithAI = findViewById(R.id.btnExtractWithAI);
        progressBar = findViewById(R.id.progressBar);
        cardResult = findViewById(R.id.cardResult);
        etExtractedName = findViewById(R.id.etExtractedName);
        etExtractedPhone = findViewById(R.id.etExtractedPhone);
        etExtractedAddress = findViewById(R.id.etExtractedAddress);
        etExtractedProduct = findViewById(R.id.etExtractedProduct);
        etExtractedNote = findViewById(R.id.etExtractedNote);
        btnSaveOrder = findViewById(R.id.btnSaveOrder);
        btnClearMessage = findViewById(R.id.btnClearMessage);

        cardResult.setVisibility(View.GONE);
        btnExtractWithAI.setEnabled(false);
    }

    private void loadUserStats() {
        if (currentUser == null) return;
        tvWelcome.setText("Welcome, " + (currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User"));
        tvUserEmail.setText(currentUser.getEmail());

        firestore.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Map<String, Object> stats = (Map<String, Object>) doc.get("stats");
                        if (stats != null) {
                            tvTotalOrders.setText(String.valueOf(stats.getOrDefault("totalOrders", 0)));
                            tvSuccessRate.setText(stats.getOrDefault("successRate", 0) + "%");
                            tvTotalRevenue.setText("$" + stats.getOrDefault("totalRevenue", 0));
                            tvTotalCOD.setText("$" + stats.getOrDefault("totalCOD", 0));
                        }
                    }
                });
    }

    private void loadAiConfig() {
        aiHelper.refreshConfig(new AiConfigManager.ConfigCallback() {
            @Override
            public void onConfigLoaded(@NonNull AiConfigManager.AiConfig config) {
                btnExtractWithAI.setEnabled(config.enabled);
                if (!config.enabled) Toast.makeText(DashboardActivity.this, "AI extraction disabled by admin", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(Exception e) {
                btnExtractWithAI.setEnabled(false);
                Toast.makeText(DashboardActivity.this, "AI config error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onConfigMissing() {
                btnExtractWithAI.setEnabled(false);
                Toast.makeText(DashboardActivity.this, "AI configuration missing in Firestore.\nPlease add document 'config/ai_settings'.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        btnExtractWithAI.setOnClickListener(v -> extractWithAI());
        btnSaveOrder.setOnClickListener(v -> saveExtractedOrder());
        btnClearMessage.setOnClickListener(v -> clearForm());
        btnViewOrders.setOnClickListener(v -> startActivity(new Intent(this, OrdersActivity.class)));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnLogout.setOnClickListener(v -> logout());
    }

    private void extractWithAI() {
        String message = etCustomerMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "Please paste customer message", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnExtractWithAI.setEnabled(false);

        aiHelper.extractOrderDetails(message, new AiHelper.ExtractionCallback() {
            @Override
            public void onSuccess(String name, String phone, String address, String product, String note) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnExtractWithAI.setEnabled(true);
                    etExtractedName.setText(name);
                    etExtractedPhone.setText(phone);
                    etExtractedAddress.setText(address);
                    etExtractedProduct.setText(product);
                    etExtractedNote.setText(note);
                    cardResult.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnExtractWithAI.setEnabled(true);
                    Toast.makeText(DashboardActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void saveExtractedOrder() {
        String name = etExtractedName.getText().toString().trim();
        String phone = etExtractedPhone.getText().toString().trim();
        String address = etExtractedAddress.getText().toString().trim();
        String product = etExtractedProduct.getText().toString().trim();
        String note = etExtractedNote.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Name, phone and address are required", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSaveOrder.setEnabled(false);

        orderRepository.saveOrder(name, phone, address, product, note)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveOrder.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Order saved successfully", Toast.LENGTH_SHORT).show();
                        clearForm();
                        loadUserStats(); // refresh order count
                    } else {
                        Toast.makeText(this, "Failed to save: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void clearForm() {
        etCustomerMessage.setText("");
        etExtractedName.setText("");
        etExtractedPhone.setText("");
        etExtractedAddress.setText("");
        etExtractedProduct.setText("");
        etExtractedNote.setText("");
        cardResult.setVisibility(View.GONE);
    }

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}