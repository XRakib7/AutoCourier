package com.softcraft.autocourier;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private Fragment currentFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            switchToFragment(item.getItemId());
            return true;
        });

        // Load default fragment
        if (savedInstanceState == null) {
            switchToFragment(R.id.navigation_dashboard);
        }
    }

    private void switchToFragment(int itemId) {
        Fragment fragment = null;
        String title = "";
        if (itemId == R.id.navigation_dashboard) {
            fragment = new DashboardFragment();
            title = "Dashboard";
        } else if (itemId == R.id.navigation_orders) {
            fragment = new OrdersFragment();
            title = "My Orders";
        } else if (itemId == R.id.navigation_profile) {
            fragment = new ProfileFragment();
            title = "Profile";
        } else if (itemId == R.id.navigation_settings) {
            fragment = new SettingsFragment();
            title = "Settings";
        }

        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }
            currentFragment = fragment;
        }
    }
}