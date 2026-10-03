package com.generated.aimbot;

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
        return decode(new int[] { 197, 184, 159, 122, 90, 114, 72, 169, 210, 179, 148, 44, 76, 37, 59, 23, 252, 218, 178, 136, 124, 22, 52, 25, 248, 155, 181, 155, 125, 85, 96, 8, 250, 223, 170, 139, 107, 16, 126, 10, 178, 157, 180, 141, 104, 16, 16, 21, 28, 234, 210, 179, 161, 107, 82, 56, 6, 253, 218, 247, 199, 32, 4, 40, 31, 233, 206, 228, 222, 114, 8, 32, 12, 232, 198 }, 173);
    }

    // Exact Dex2C target: the update API server URL.
    public static String getUpdateServerUrl() {
        return decode(new int[] { 51, 14, 237, 200, 164, 204, 58, 27, 41, 27, 225, 157, 187, 129, 32, 77, 59, 1, 164, 203, 162, 211, 54, 10, 44, 12, 243, 197, 209, 186, 152, 110, 21, 57, 22, 245 }, 91);
    }
}
