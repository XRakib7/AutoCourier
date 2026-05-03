package com.softcraft.autocourier;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SignupActivity extends AppCompatActivity {

    // UI Components
    private CircleImageView profileImage;
    private EditText etFullName, etUsername, etPhone, etEmail, etPassword, etConfirmPassword;
    private EditText etBusinessName, etBusinessEmail, etDob, etApartment, etStreet, etCity, etDistrict, etZip;
    private MaterialCheckBox cbTerms;
    private Button btnSignup, btnPickPhoto;
    private TextView tvUsernameStatus, tvPasswordStrength, tvSignupError;
    private ProgressBar passwordStrengthBar;
    private RelativeLayout loadingOverlay;
    private MaterialCardView errorCard;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private DeviceInfoCollector deviceInfoCollector;

    private String encodedProfileImage = null;
    private Handler usernameCheckHandler = new Handler();
    private boolean isUsernameValid = false;
    private boolean isUsernameChecking = false;

    // Google pre-fill data
    private String googleName = null;
    private String googleEmail = null;
    private boolean isGoogleSignUp = false;

    // ActivityResultLaunchers
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        deviceInfoCollector = new DeviceInfoCollector(this);

        // Get Google pre-fill if any
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("google_name") && intent.hasExtra("google_email")) {
            googleName = intent.getStringExtra("google_name");
            googleEmail = intent.getStringExtra("google_email");
            isGoogleSignUp = true;
        }

        initViews();
        preFillFromGoogle();
        setupTextWatchers();
        setupPhotoPicker();
        setClickListeners();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PermissionHelper.GALLERY_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initViews() {
        profileImage = findViewById(R.id.profileImage);
        btnPickPhoto = findViewById(R.id.btnPickPhoto);
        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        tvUsernameStatus = findViewById(R.id.tvUsernameStatus);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        passwordStrengthBar = findViewById(R.id.passwordStrengthBar);
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        etBusinessName = findViewById(R.id.etBusinessName);
        etBusinessEmail = findViewById(R.id.etBusinessEmail);
        etDob = findViewById(R.id.etDob);
        etApartment = findViewById(R.id.etApartment);
        etStreet = findViewById(R.id.etStreet);
        etCity = findViewById(R.id.etCity);
        etDistrict = findViewById(R.id.etDistrict);
        etZip = findViewById(R.id.etZip);
        cbTerms = findViewById(R.id.cbTerms);
        btnSignup = findViewById(R.id.btnSignup);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        tvSignupError = findViewById(R.id.tvSignupError);
        errorCard = findViewById(R.id.errorCard);
    }

    private void preFillFromGoogle() {
        if (isGoogleSignUp) {
            if (!TextUtils.isEmpty(googleName)) {
                etFullName.setText(googleName);
            }
            if (!TextUtils.isEmpty(googleEmail)) {
                etEmail.setText(googleEmail);
                etEmail.setEnabled(false);
            }
        }
    }

    private void setupTextWatchers() {
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pwd = s.toString();
                int strength = PasswordStrengthUtil.calculateStrength(pwd);
                passwordStrengthBar.setProgress(strength);
                tvPasswordStrength.setText(PasswordStrengthUtil.getStrengthText(strength));
                passwordStrengthBar.getProgressDrawable().setColorFilter(
                        PasswordStrengthUtil.getStrengthColor(strength), android.graphics.PorterDuff.Mode.SRC_IN);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etUsername.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                usernameCheckHandler.removeCallbacksAndMessages(null);
                String username = s.toString().trim();
                if (username.length() < 3) {
                    tvUsernameStatus.setText("Username must be at least 3 characters");
                    tvUsernameStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                    isUsernameValid = false;
                    return;
                }
                usernameCheckHandler.postDelayed(() -> checkUsernameAvailability(username), 500);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void checkUsernameAvailability(String username) {
        isUsernameChecking = true;
        firestore.collection("users").whereEqualTo("profile.username", username)
                .get()
                .addOnCompleteListener(task -> {
                    isUsernameChecking = false;
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            tvUsernameStatus.setText("Username available");
                            tvUsernameStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                            isUsernameValid = true;
                        } else {
                            tvUsernameStatus.setText("Username already taken");
                            tvUsernameStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                            isUsernameValid = false;
                        }
                    } else {
                        tvUsernameStatus.setText("Error checking username");
                        isUsernameValid = false;
                    }
                });
    }

    private void setupPhotoPicker() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            android.graphics.Bitmap photo = (android.graphics.Bitmap) extras.get("data");
                            if (photo != null) {
                                profileImage.setImageBitmap(photo);
                                encodedProfileImage = encodeImageToBase64(photo);
                            }
                        }
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                                profileImage.setImageBitmap(bitmap);
                                encodedProfileImage = encodeImageToBase64(bitmap);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    private String encodeImageToBase64(android.graphics.Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void setClickListeners() {
        btnPickPhoto.setOnClickListener(v -> showPhotoPickerDialog());
        btnSignup.setOnClickListener(v -> validateAndSignup());
        findViewById(R.id.tvLoginLink).setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void showPhotoPickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Profile Photo");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                if (PermissionHelper.hasCameraPermission(this)) {
                    openCamera();
                } else {
                    PermissionHelper.requestCameraPermission(this);
                }
            } else {
                if (PermissionHelper.hasGalleryPermission(this)) {
                    openGallery();
                } else {
                    PermissionHelper.requestGalleryPermission(this);
                }
            }
        });
        builder.show();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(pickPhotoIntent);
    }

    private void validateAndSignup() {
        // Clear previous error
        errorCard.setVisibility(View.GONE);
        tvSignupError.setText("");

        List<String> errors = new ArrayList<>();

        // Extract values
        String fullName = etFullName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String street = etStreet.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String district = etDistrict.getText().toString().trim();
        String zip = etZip.getText().toString().trim();

        // Validate Full Name
        if (TextUtils.isEmpty(fullName)) {
            errors.add("• Full name is required");
            etFullName.setError("Required");
        } else {
            etFullName.setError(null);
        }

        // Validate Username
        if (TextUtils.isEmpty(username)) {
            errors.add("• Username is required");
            etUsername.setError("Required");
        } else if (username.length() < 3) {
            errors.add("• Username must be at least 3 characters");
            etUsername.setError("Minimum 3 characters");
        } else if (isUsernameChecking) {
            errors.add("• Please wait, checking username availability...");
        } else if (!isUsernameValid) {
            errors.add("• Username is not available or invalid");
            etUsername.setError("Username unavailable");
        } else {
            etUsername.setError(null);
        }

        // Validate Phone
        if (TextUtils.isEmpty(phone)) {
            errors.add("• Phone number is required");
            etPhone.setError("Required");
        } else if (!Patterns.PHONE.matcher(phone).matches()) {
            errors.add("• Please enter a valid phone number");
            etPhone.setError("Invalid phone number");
        } else {
            etPhone.setError(null);
        }

        // Validate Email
        if (TextUtils.isEmpty(email)) {
            errors.add("• Email address is required");
            etEmail.setError("Required");
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errors.add("• Please enter a valid email address");
            etEmail.setError("Invalid email");
        } else {
            etEmail.setError(null);
        }

        // Password validation (depends on Google sign-up)
        boolean isPasswordRequiredForGoogle = false;
        if (isGoogleSignUp) {
            // For Google sign-up, password is optional but if either field has text, both must be valid
            boolean passwordFieldFilled = !TextUtils.isEmpty(password);
            boolean confirmFieldFilled = !TextUtils.isEmpty(confirmPassword);
            if (passwordFieldFilled || confirmFieldFilled) {
                isPasswordRequiredForGoogle = true;
            }
        }

        if (!isGoogleSignUp || isPasswordRequiredForGoogle) {
            if (TextUtils.isEmpty(password)) {
                errors.add("• Password is required");
                etPassword.setError("Required");
            } else if (PasswordStrengthUtil.calculateStrength(password) < 40) {
                errors.add("• Password is too weak (use at least 8 characters with letters, numbers, and symbols)");
                etPassword.setError("Weak password");
            } else {
                etPassword.setError(null);
            }

            if (TextUtils.isEmpty(confirmPassword)) {
                errors.add("• Please confirm your password");
                etConfirmPassword.setError("Required");
            } else if (!password.equals(confirmPassword)) {
                errors.add("• Passwords do not match");
                etConfirmPassword.setError("Does not match");
            } else {
                etConfirmPassword.setError(null);
            }
        } else {
            // Clear password fields errors if not required
            etPassword.setError(null);
            etConfirmPassword.setError(null);
        }

        // Validate Date of Birth
        if (TextUtils.isEmpty(dob)) {
            errors.add("• Date of birth is required");
            etDob.setError("Required");
        } else {
            etDob.setError(null);
        }

        // Validate Address mandatory fields
        if (TextUtils.isEmpty(street)) {
            errors.add("• Street address is required");
            etStreet.setError("Required");
        } else {
            etStreet.setError(null);
        }
        if (TextUtils.isEmpty(city)) {
            errors.add("• City is required");
            etCity.setError("Required");
        } else {
            etCity.setError(null);
        }
        if (TextUtils.isEmpty(district)) {
            errors.add("• District is required");
            etDistrict.setError("Required");
        } else {
            etDistrict.setError(null);
        }
        if (TextUtils.isEmpty(zip)) {
            errors.add("• ZIP code is required");
            etZip.setError("Required");
        } else {
            etZip.setError(null);
        }

        // Validate Terms
        if (!cbTerms.isChecked()) {
            errors.add("• You must accept the Terms & Privacy Policy");
        }

        // If errors exist, show summary and return
        if (!errors.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Please fix the following issues:\n\n");
            for (String error : errors) {
                errorMessage.append(error).append("\n");
            }
            tvSignupError.setText(errorMessage.toString());
            errorCard.setVisibility(View.VISIBLE);

            // Scroll to error card
            findViewById(R.id.scrollView).post(() ->
                    ((View) errorCard.getParent()).requestFocus());

            // Optional: subtle vibration for error (requires VIBRATE permission)
            // Uncomment if permission is declared
            // android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            // if (vibrator != null) vibrator.vibrate(50);

            return;
        }

        // If we get here, all validations passed
        performSignup(fullName, username, phone, email, password, dob, street, city, district, zip);
    }

    private void performSignup(String fullName, String username, String phone, String email, String password,
                               String dob, String street, String city, String district, String zip) {
        showLoading(true);

        String deviceId = deviceInfoCollector.getDeviceId();
        String model = deviceInfoCollector.getModel();
        String brand = deviceInfoCollector.getBrand();
        String androidVersion = deviceInfoCollector.getAndroidVersion();
        String os = deviceInfoCollector.getOs();
        String ip = deviceInfoCollector.getIpAddress();

        Task<Location> locationTask = deviceInfoCollector.getLastKnownLocation();
        if (locationTask != null) {
            locationTask.addOnCompleteListener(locationResult -> {
                proceedWithAuth(fullName, username, phone, email, password, dob, street, city, district, zip,
                        deviceId, model, brand, androidVersion, os, ip,
                        locationResult.isSuccessful() ? locationResult.getResult() : null);
            });
        } else {
            proceedWithAuth(fullName, username, phone, email, password, dob, street, city, district, zip,
                    deviceId, model, brand, androidVersion, os, ip, null);
        }
    }

    private void proceedWithAuth(String fullName, String username, String phone, String email, String password,
                                 String dob, String street, String city, String district, String zip,
                                 String deviceId, String model, String brand, String androidVersion, String os, String ip,
                                 Location location) {
        if (isGoogleSignUp) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null && currentUser.getEmail().equals(email)) {
                if (!TextUtils.isEmpty(fullName) && (currentUser.getDisplayName() == null || !currentUser.getDisplayName().equals(fullName))) {
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build();
                    currentUser.updateProfile(profileUpdates);
                }

                if (!TextUtils.isEmpty(password)) {
                    AuthCredential emailCredential = EmailAuthProvider.getCredential(email, password);
                    currentUser.linkWithCredential(emailCredential)
                            .addOnCompleteListener(linkTask -> {
                                if (linkTask.isSuccessful()) {
                                    Toast.makeText(SignupActivity.this, "Password linked successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(SignupActivity.this, "Password linking failed: " + linkTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                                createFirestoreDocument(currentUser.getUid(), fullName, username, phone, email,
                                        dob, street, city, district, zip, deviceId, model, brand,
                                        androidVersion, os, ip, location);
                            });
                } else {
                    createFirestoreDocument(currentUser.getUid(), fullName, username, phone, email,
                            dob, street, city, district, zip, deviceId, model, brand,
                            androidVersion, os, ip, location);
                }
            } else {
                showLoading(false);
                Toast.makeText(SignupActivity.this, "Google authentication error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        } else {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(fullName)
                                        .build();
                                user.updateProfile(profileUpdates);
                                user.sendEmailVerification();
                                createFirestoreDocument(user.getUid(), fullName, username, phone, email, dob, street, city, district, zip,
                                        deviceId, model, brand, androidVersion, os, ip, location);
                            } else {
                                showLoading(false);
                                Toast.makeText(SignupActivity.this, "Account creation failed.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            showLoading(false);
                            Toast.makeText(SignupActivity.this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void createFirestoreDocument(String uid, String fullName, String username, String phone, String email,
                                         String dob, String street, String city, String district, String zip,
                                         String deviceId, String model, String brand, String androidVersion, String os, String ip,
                                         Location location) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", fullName);
        profile.put("username", username);
        profile.put("businessName", TextUtils.isEmpty(etBusinessName.getText().toString().trim()) ? "" : etBusinessName.getText().toString().trim());
        profile.put("businessEmail", TextUtils.isEmpty(etBusinessEmail.getText().toString().trim()) ? "" : etBusinessEmail.getText().toString().trim());
        profile.put("phone", phone);
        profile.put("dob", dob);
        profile.put("address", new HashMap<String, String>() {{
            put("apartment", etApartment.getText().toString().trim());
            put("street", street);
            put("city", city);
            put("district", district);
            put("zip", zip);
        }});
        profile.put("profilePhotoUrl", encodedProfileImage != null ? "data:image/jpeg;base64," + encodedProfileImage : "");
        profile.put("onlineStatus", true);
        profile.put("createdAt", new Date());
        profile.put("lastSeen", new Date());

        Map<String, Object> device = new HashMap<>();
        device.put("fcmToken", "");
        device.put("deviceId", deviceId);
        device.put("ipAddress", ip);
        device.put("os", os);
        device.put("androidVersion", androidVersion);
        device.put("model", model);
        device.put("brand", brand);
        if (location != null) {
            Map<String, Double> loc = new HashMap<>();
            loc.put("lat", location.getLatitude());
            loc.put("lng", location.getLongitude());
            device.put("location", loc);
        } else {
            device.put("location", null);
        }

        Map<String, Object> settings = new HashMap<>();
        settings.put("defaultNote", "");
        settings.put("biometricEnabled", false);
        settings.put("darkMode", false);
        settings.put("language", "en");
        settings.put("notificationsEnabled", true);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", 0);
        stats.put("successRate", 0.0);
        stats.put("totalRevenue", 0.0);
        stats.put("totalCOD", 0.0);
        stats.put("totalReturned", 0);

        Map<String, Object> userData = new HashMap<>();
        userData.put("profile", profile);
        userData.put("isVerified", isGoogleSignUp);
        userData.put("device", device);
        userData.put("settings", settings);
        userData.put("stats", stats);

        firestore.collection("users").document(uid)
                .set(userData)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(SignupActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        if (isGoogleSignUp) {
                            startActivity(new Intent(SignupActivity.this, DashboardActivity.class));
                        } else {
                            startActivity(new Intent(SignupActivity.this, EmailVerificationActivity.class));
                        }
                        finish();
                    } else {
                        Toast.makeText(SignupActivity.this, "Failed to save user data: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        if (mAuth.getCurrentUser() != null) {
                            mAuth.getCurrentUser().delete();
                        }
                    }
                });
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSignup.setEnabled(!show);
        btnPickPhoto.setEnabled(!show);
    }
}