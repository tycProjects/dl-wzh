package com.example.floatingwindow;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_OVERLAY_PERMISSION = 1001;
    private boolean serviceRunning = false;

    private TextView tvStatus;
    private Button btnToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        btnToggle = findViewById(R.id.btnToggle);

        btnToggle.setOnClickListener(v -> onToggleClicked());
    }

    private void onToggleClicked() {
        if (!serviceRunning) {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission();
                return;
            }
            startFloatingService();
        } else {
            stopFloatingService();
        }
    }

    private void requestOverlayPermission() {
        Toast.makeText(this, R.string.permission_needed, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
        );
        startActivityForResult(intent, REQ_OVERLAY_PERMISSION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService();
            } else {
                Toast.makeText(this, R.string.permission_needed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startFloatingService() {
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        serviceRunning = true;
        updateStatus();
    }

    private void stopFloatingService() {
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        stopService(serviceIntent);
        serviceRunning = false;
        updateStatus();
    }

    private void updateStatus() {
        if (serviceRunning) {
            tvStatus.setText("Status: aktif");
            btnToggle.setText(R.string.stop_service);
        } else {
            tvStatus.setText("Status: tidak aktif");
            btnToggle.setText(R.string.start_service);
        }
    }
}
