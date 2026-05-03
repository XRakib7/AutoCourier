package com.softcraft.autocourier;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DeviceInfoCollector {

    private Context context;
    private FusedLocationProviderClient fusedLocationClient;

    public DeviceInfoCollector(Context context) {
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    // Get Android device ID (unique)
    public String getDeviceId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    // Get device model and brand
    public String getModel() {
        return Build.MODEL;
    }

    public String getBrand() {
        return Build.BRAND;
    }

    // Get Android version
    public String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    // Get OS name (always "Android")
    public String getOs() {
        return "Android";
    }

    // Get IP address (first non-loopback IPv4)
    public String getIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0.0.0.0";
    }

    // Get location if permission granted, otherwise returns null (don't force permission)
    public Task<Location> getLastKnownLocation() {
        // Check if location permission is granted (coarse or fine)
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            return fusedLocationClient.getLastLocation();
        } else {
            return null; // No permission, don't collect
        }
    }

    // Get FCM token (asynchronously)
    public void getFcmToken(FcmTokenCallback callback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        callback.onTokenReceived(task.getResult());
                    } else {
                        callback.onTokenReceived(null);
                    }
                });
    }

    public interface FcmTokenCallback {
        void onTokenReceived(String token);
    }
}