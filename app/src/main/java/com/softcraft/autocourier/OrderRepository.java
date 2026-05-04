package com.softcraft.autocourier;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class OrderRepository {
    private final FirebaseFirestore firestore;
    private final String merchantId;

    public OrderRepository() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("User not logged in");
        this.merchantId = user.getUid();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public Task<Void> saveOrder(String customerName, String phone, String address,
                                String product, String note) {
        Map<String, Object> order = new HashMap<>();
        order.put("customerName", customerName);
        order.put("phone", phone);
        order.put("address", address);
        order.put("product", product);
        order.put("note", note);
        order.put("status", "pending");
        order.put("createdAt", System.currentTimeMillis());
        order.put("merchantId", merchantId);

        return firestore.collection("orders")
                .add(order)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        // Increment totalOrders counter in user's stats
                        firestore.collection("users").document(merchantId)
                                .update("stats.totalOrders", com.google.firebase.firestore.FieldValue.increment(1));
                    }
                    return null;
                });
    }

    public Query getOrdersQuery() {
        return firestore.collection("orders")
                .whereEqualTo("merchantId", merchantId)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }
}