package com.softcraft.autocourier;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {

    public static final int CAMERA_PERMISSION_CODE = 100;
    public static final int GALLERY_PERMISSION_CODE = 101;
    public static final int LOCATION_PERMISSION_CODE = 102;

    public static boolean hasCameraPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasGalleryPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasLocationPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_CODE);
    }

    public static void requestGalleryPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                GALLERY_PERMISSION_CODE);
    }

    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_CODE);
    }
}