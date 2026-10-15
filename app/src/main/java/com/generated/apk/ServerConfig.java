package com.generated.apk;

public final class ServerConfig {
    private ServerConfig() {}

    private static String decode(int[] data, int salt) {
        if (data == null || data.length == 0) return "";
        char[] out = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (char) (data[i] ^ ((salt + i * 31) & 0xff));
        }
        return new String(out);
    }

    // Exact Dex2C target: the app's remote WebView start URL.
    public static String getBaseUrl() {
        return decode(new int[] { 197, 184, 159, 122, 90, 114, 72, 169, 193, 161, 149, 103, 77, 47, 47, 27, 239, 207, 245, 157, 118, 87, 48, 26, 240, 154, 176, 157, 124, 31, 34, 15, 253, 223, 228, 142, 102, 75, 50, 11, 224, 202, 183, 131, 117, 73, 80, 48, 82, 246, 218, 172, 152, 107, 84, 36, 28, 228, 199, 253, 150, 117, 64, 34, 2, 239, 202, 190, 128, 103, 73, 121, 13, 232, 158, 163, 147 }, 173);
    }

    // Exact Dex2C target: the update API server URL.
    public static String getUpdateServerUrl() {
        return decode(new int[] { 51, 14, 237, 200, 164, 204, 58, 27, 41, 27, 225, 157, 187, 129, 32, 77, 59, 1, 164, 203, 162, 211, 54, 10, 44, 12, 243, 197, 209, 186, 152, 110, 21, 57, 22, 245 }, 91);
    }
}
