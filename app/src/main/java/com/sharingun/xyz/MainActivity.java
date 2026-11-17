package com.sharingun.xyz;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;

public class MainActivity extends Activity {

    private static final int OVERLAY_PERMISSION_REQ = 1234;
    private Switch masterSwitch;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        masterSwitch = findViewById(R.id.masterSwitch);
        statusText   = findViewById(R.id.statusText);

        updateSwitchState();

        masterSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                if (!Settings.canDrawOverlays(this)) {
                    requestOverlayPermission();
                    masterSwitch.setChecked(false);
                } else {
                    startPanel();
                }
            } else {
                stopPanel();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSwitchState();
    }

    private void updateSwitchState() {
        boolean running = OverlayService.isRunning;
        masterSwitch.setChecked(running);
        statusText.setText(running ? "Panel Active ✓" : "Panel Inactive");
    }

    private void startPanel() {
        Intent intent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        statusText.setText("Panel Active ✓");
        Toast.makeText(this, "𝐒𝐡𝐚𝐫𝐢𝐧𝐠𝐮𝐧 𝐗𝐘𝐙 Panel Started", Toast.LENGTH_SHORT).show();
    }

    private void stopPanel() {
        stopService(new Intent(this, OverlayService.class));
        statusText.setText("Panel Inactive");
        Toast.makeText(this, "Panel Stopped", Toast.LENGTH_SHORT).show();
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName())
        );
        startActivityForResult(intent, OVERLAY_PERMISSION_REQ);
        Toast.makeText(this, "Please allow overlay permission", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ) {
            if (Settings.canDrawOverlays(this)) {
                masterSwitch.setChecked(true);
                startPanel();
            } else {
                Toast.makeText(this, "Permission denied. Cannot show panel.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
