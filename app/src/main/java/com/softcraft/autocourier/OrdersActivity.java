package com.softcraft.autocourier;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class OrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private OrdersAdapter adapter;
    private OrderRepository orderRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        recyclerView = findViewById(R.id.recyclerViewOrders);
        progressBar = findViewById(R.id.progressBarOrders);
        tvEmpty = findViewById(R.id.tvEmptyOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        orderRepository = new OrderRepository();
        adapter = new OrdersAdapter();
        recyclerView.setAdapter(adapter);

        loadOrders();
    }

    private void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);
        Query query = orderRepository.getOrdersQuery();
        query.addSnapshotListener((snapshots, e) -> {
            progressBar.setVisibility(View.GONE);
            if (e != null) {
                Toast.makeText(this, "Failed to load orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (snapshots == null || snapshots.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                return;
            }
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            List<Order> orders = new ArrayList<>();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Order order = doc.toObject(Order.class);
                if (order != null) {
                    order.setId(doc.getId());
                    orders.add(order);
                }
            }
            adapter.setOrders(orders);
        });
    }

    // Simple adapter
    static class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {
        private List<Order> orders = new ArrayList<>();

        public void setOrders(List<Order> orders) {
            this.orders = orders;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            Order order = orders.get(position);
            holder.tvCustomer.setText(order.getCustomerName());
            holder.tvPhone.setText(order.getPhone());
            holder.tvAddress.setText(order.getAddress());
            holder.tvProduct.setText(order.getProduct());
            holder.tvStatus.setText(order.getStatus());
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        static class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvCustomer, tvPhone, tvAddress, tvProduct, tvStatus;
            public OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCustomer = itemView.findViewById(R.id.tvOrderCustomer);
                tvPhone = itemView.findViewById(R.id.tvOrderPhone);
                tvAddress = itemView.findViewById(R.id.tvOrderAddress);
                tvProduct = itemView.findViewById(R.id.tvOrderProduct);
                tvStatus = itemView.findViewById(R.id.tvOrderStatus);
            }
        }
    }

    // Simple Order model
    public static class Order {
        private String id;
        private String customerName;
        private String phone;
        private String address;
        private String product;
        private String note;
        private String status;
        private long createdAt;
        private String merchantId;

        // Getters & setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getProduct() { return product; }
        public void setProduct(String product) { this.product = product; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    }
}