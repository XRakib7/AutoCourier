package com.softcraft.autocourier;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        sendTokenToServer(token);
    }

    private void sendTokenToServer(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Map<String, Object> update = new HashMap<>();
            update.put("device.fcmToken", token);
            FirebaseFirestore.getInstance().collection("users")
                    .document(user.getUid())
                    .update(update)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved to Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM token", e));
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        // Handle incoming messages if needed (not required for now)
        Log.d(TAG, "Message received: " + remoteMessage.getNotification());
    }
}