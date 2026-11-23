package com.example.airquality;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView nav;

    private final ActivityResultLauncher<String> notifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);   // the design is a light theme
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Long-lived ViewModels: storage, forecasts and alerts run whichever tab is open.
        ViewModelProvider p = new ViewModelProvider(this);
        p.get(ReadingViewModel.class);
        p.get(OutdoorViewModel.class);

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }

        nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment f;
            if (id == R.id.nav_dashboard) f = new DashboardFragment();
            else if (id == R.id.nav_trends) f = new TrendsFragment();
            else if (id == R.id.nav_map) f = new MapFragment();
            else f = new AlertsFragment();
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, f).commit();
            return true;
        });

        if (savedInstanceState == null) nav.setSelectedItemId(R.id.nav_dashboard);
    }

    public void selectTab(int menuId) {
        nav.setSelectedItemId(menuId);
    }

    public void openSettings() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SettingsFragment())
                .addToBackStack("settings").commit();
    }

    public void openReport() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ReportFragment())
                .addToBackStack("report").commit();
    }
}
