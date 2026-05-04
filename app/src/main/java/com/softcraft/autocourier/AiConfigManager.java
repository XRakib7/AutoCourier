package com.softcraft.autocourier;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AiConfigManager {
    private static final String CONFIG_COLLECTION = "config";
    private static final String CONFIG_DOCUMENT = "ai_settings";
    private static final String PREFS_NAME = "ai_config_cache";

    private static final String KEY_API_URL = "apiUrl";
    private static final String KEY_API_KEY = "apiKey";
    private static final String KEY_MODEL = "model";
    private static final String KEY_SYSTEM_PROMPT = "systemPrompt";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_ENABLED = "enabled";

    private static AiConfigManager instance;
    private final FirebaseFirestore firestore;
    private final SharedPreferences prefs;
    private AiConfig cachedConfig;
    private boolean isFetching = false;

    private AiConfigManager(Context context) {
        firestore = FirebaseFirestore.getInstance();
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadCachedConfig(); // may be null
    }

    public static synchronized AiConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new AiConfigManager(context);
        }
        return instance;
    }

    private void loadCachedConfig() {
        String apiUrl = prefs.getString(KEY_API_URL, null);
        String apiKey = prefs.getString(KEY_API_KEY, null);
        String model = prefs.getString(KEY_MODEL, null);
        String systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, null);
        float temperature = prefs.getFloat(KEY_TEMPERATURE, 0.2f);
        boolean enabled = prefs.getBoolean(KEY_ENABLED, true);

        if (apiUrl != null && apiKey != null && model != null) {
            cachedConfig = new AiConfig(apiUrl, apiKey, model, systemPrompt, temperature, enabled);
        } else {
            cachedConfig = null; // No fallback
        }
    }

    public interface ConfigCallback {
        void onConfigLoaded(@NonNull AiConfig config);
        void onError(Exception e);
        void onConfigMissing(); // Firestore document doesn't exist or missing required fields
    }

    public void refreshConfig(ConfigCallback callback) {
        if (isFetching) {
            if (callback != null) callback.onError(new Exception("Already fetching config"));
            return;
        }
        isFetching = true;
        firestore.collection(CONFIG_COLLECTION).document(CONFIG_DOCUMENT)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isFetching = false;
                    if (!documentSnapshot.exists()) {
                        if (callback != null) callback.onConfigMissing();
                        return;
                    }

                    String apiUrl = documentSnapshot.getString(KEY_API_URL);
                    String apiKey = documentSnapshot.getString(KEY_API_KEY);
                    String model = documentSnapshot.getString(KEY_MODEL);
                    String systemPrompt = documentSnapshot.getString(KEY_SYSTEM_PROMPT);
                    Double tempDouble = documentSnapshot.getDouble(KEY_TEMPERATURE);
                    float temperature = tempDouble != null ? tempDouble.floatValue() : 0.2f;
                    Boolean enabled = documentSnapshot.getBoolean(KEY_ENABLED);
                    if (enabled == null) enabled = true;

                    if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty() || model == null || model.isEmpty()) {
                        if (callback != null) callback.onConfigMissing();
                        return;
                    }

                    cachedConfig = new AiConfig(apiUrl, apiKey, model, systemPrompt, temperature, enabled);
                    // Cache
                    prefs.edit()
                            .putString(KEY_API_URL, apiUrl)
                            .putString(KEY_API_KEY, apiKey)
                            .putString(KEY_MODEL, model)
                            .putString(KEY_SYSTEM_PROMPT, systemPrompt == null ? "" : systemPrompt)
                            .putFloat(KEY_TEMPERATURE, temperature)
                            .putBoolean(KEY_ENABLED, enabled)
                            .apply();

                    if (callback != null) callback.onConfigLoaded(cachedConfig);
                })
                .addOnFailureListener(e -> {
                    isFetching = false;
                    if (callback != null) callback.onError(e);
                });
    }

    @Nullable
    public AiConfig getCurrentConfig() {
        return cachedConfig;
    }

    public static class AiConfig {
        public final String apiUrl;
        public final String apiKey;
        public final String model;
        public final String systemPrompt;
        public final float temperature;
        public final boolean enabled;

        public AiConfig(String apiUrl, String apiKey, String model, String systemPrompt, float temperature, boolean enabled) {
            this.apiUrl = apiUrl;
            this.apiKey = apiKey;
            this.model = model;
            this.systemPrompt = systemPrompt;
            this.temperature = temperature;
            this.enabled = enabled;
        }
    }
}