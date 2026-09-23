package com.hoang.proxy;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.SecureRandom;

public final class KeyManager {
    private static final String PREF = "hoang_proxy_key";
    private static final String KEY = "key";
    private static final String CREATED = "created";
    private static final long VALID_MS = 12L * 60L * 60L * 1000L;
    public static final String VIP = "HOANG-PREMIUM";

    private KeyManager() {}

    public static String getOrCreate(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String old = p.getString(KEY, null);
        long created = p.getLong(CREATED, 0L);
        long now = System.currentTimeMillis();

        if (old != null && now - created < VALID_MS) {
            return old;
        }

        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder out = new StringBuilder();
        for (int group = 0; group < 3; group++) {
            if (group > 0) out.append("-");
            for (int i = 0; i < 4; i++) {
                out.append(chars.charAt(random.nextInt(chars.length())));
            }
        }

        String key = out.toString();
        p.edit().putString(KEY, key).putLong(CREATED, now).apply();
        return key;
    }

    public static boolean isValid(Context context, String input) {
        if (VIP.equals(input.trim())) return true;

        SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String key = p.getString(KEY, null);
        long created = p.getLong(CREATED, 0L);

        return key != null
                && key.equals(input.trim())
                && System.currentTimeMillis() - created < VALID_MS;
    }

    public static long remainingMs(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        long created = p.getLong(CREATED, 0L);
        return Math.max(0L, VALID_MS - (System.currentTimeMillis() - created));
    }
}
