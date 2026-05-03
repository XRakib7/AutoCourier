package com.softcraft.autocourier;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView tvName, tvUsername, tvEmail, tvPhone;
    private TextView tvBusinessName, tvBusinessEmail;
    private TextView tvAddress;
    private Button btnBackToDashboard;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews();
        loadProfileData();
        setClickListeners();
    }

    private void initViews() {
        profileImage = findViewById(R.id.profileImage);
        tvName = findViewById(R.id.tvName);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvBusinessName = findViewById(R.id.tvBusinessName);
        tvBusinessEmail = findViewById(R.id.tvBusinessEmail);
        tvAddress = findViewById(R.id.tvAddress);
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard);
    }

    private void loadProfileData() {
        if (currentUser == null) return;

        // Basic auth info
        tvEmail.setText("Email: " + currentUser.getEmail());
        tvName.setText("Name: " + (currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Not set"));

        firestore.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> profile = (Map<String, Object>) documentSnapshot.get("profile");
                        if (profile != null) {
                            String username = (String) profile.get("username");
                            String phone = (String) profile.get("phone");
                            String businessName = (String) profile.get("businessName");
                            String businessEmail = (String) profile.get("businessEmail");
                            String photoUrl = (String) profile.get("profilePhotoUrl");

                            tvUsername.setText("Username: " + (username != null ? username : ""));
                            tvPhone.setText("Phone: " + (phone != null ? phone : ""));
                            tvBusinessName.setText("Business Name: " + (businessName != null ? businessName : ""));
                            tvBusinessEmail.setText("Business Email: " + (businessEmail != null ? businessEmail : ""));

                            // Address
                            Map<String, String> address = (Map<String, String>) profile.get("address");
                            if (address != null) {
                                String addr = (address.get("apartment") != null ? address.get("apartment") + ", " : "") +
                                        (address.get("street") != null ? address.get("street") + ", " : "") +
                                        (address.get("city") != null ? address.get("city") + ", " : "") +
                                        (address.get("district") != null ? address.get("district") + ", " : "") +
                                        (address.get("zip") != null ? address.get("zip") : "");
                                tvAddress.setText("Address: " + addr);
                            }

                            // Profile photo (Base64)
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                // photoUrl is like "data:image/jpeg;base64,...."
                                try {
                                    Glide.with(ProfileActivity.this)
                                            .load(photoUrl)
                                            .placeholder(R.drawable.ic_default_avatar)
                                            .into(profileImage);
                                } catch (Exception e) {
                                    profileImage.setImageResource(R.drawable.ic_default_avatar);
                                }
                            } else {
                                profileImage.setImageResource(R.drawable.ic_default_avatar);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void setClickListeners() {
        btnBackToDashboard.setOnClickListener(v -> finish());
    }
}