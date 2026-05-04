package com.softcraft.autocourier;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiHelper {
    private static final String TAG = "AiHelper";
    private final OkHttpClient client;
    private final AiConfigManager configManager;

    public interface ExtractionCallback {
        void onSuccess(String name, String phone, String address, String product, String note);
        void onError(String errorMessage);
    }

    public AiHelper(Context context) {
        this.client = new OkHttpClient();
        this.configManager = AiConfigManager.getInstance(context);
    }

    public void extractOrderDetails(String customerMessage, ExtractionCallback callback) {
        AiConfigManager.AiConfig config = configManager.getCurrentConfig();
        if (config == null) {
            callback.onError("AI configuration not loaded. Please refresh and try again.");
            return;
        }
        if (!config.enabled) {
            callback.onError("AI extraction is disabled by admin.");
            return;
        }

        // Use system prompt exactly as stored (no fallback)
        String systemPrompt = config.systemPrompt;
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            callback.onError("System prompt is missing in Firestore config.");
            return;
        }

        String userMessage = "Message: \"" + customerMessage + "\"";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", config.model);
            jsonBody.put("messages", new org.json.JSONArray()
                    .put(new JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(new JSONObject().put("role", "user").put("content", userMessage)));
            jsonBody.put("temperature", config.temperature);
            // No 'n' parameter to avoid errors
        } catch (JSONException e) {
            callback.onError("Failed to build API request: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody.toString()
        );

        Request request = new Request.Builder()
                .url(config.apiUrl)
                .addHeader("Authorization", "Bearer " + config.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    String errorMsg = "API error: " + response.code();
                    try {
                        JSONObject errorJson = new JSONObject(responseBody);
                        if (errorJson.has("error")) {
                            errorMsg += " - " + errorJson.getJSONObject("error").optString("message");
                        }
                    } catch (JSONException ignored) {}
                    callback.onError(errorMsg);
                    return;
                }

                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String content = jsonResponse.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                    content = content.replace("```json", "").replace("```", "").trim();
                    JSONObject extracted = new JSONObject(content);
                    callback.onSuccess(
                            extracted.optString("name", ""),
                            extracted.optString("phone", ""),
                            extracted.optString("address", ""),
                            extracted.optString("product", ""),
                            extracted.optString("note", "")
                    );
                } catch (JSONException e) {
                    callback.onError("Failed to parse AI response: " + e.getMessage());
                }
            }
        });
    }

    public void refreshConfig(AiConfigManager.ConfigCallback callback) {
        configManager.refreshConfig(callback);
    }
}