package com.generated.cai;

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
        return decode(new int[] { 197, 184, 159, 122, 90, 114, 72, 169, 196, 173, 144, 118, 84, 36, 54, 17, 179, 219, 180, 149, 126, 84, 50, 88, 246, 219, 190, 221, 112, 64, 63, 29, 162, 206, 170, 218, 109, 25, 127, 80, 231, 137, 245, 219, 56, 25, 18, 106, 78, 250, 143, 247, 152, 40, 85, 98, 88, 166, 134, 234, 149, 114, 22, 118, 14, 239, 157, 254, 216, 55, 84, 46, 10, 243, 243, 176, 132, 118, 118, 91, 42, 65, 239, 200, 172, 157 }, 173);
    }

    // Exact Dex2C target: the update API server URL.
    public static String getUpdateServerUrl() {
        return decode(new int[] { 51, 14, 237, 200, 164, 204, 58, 27, 41, 27, 225, 157, 187, 129, 32, 77, 59, 1, 164, 203, 162, 211, 54, 10, 44, 12, 243, 197, 209, 186, 152, 110, 21, 57, 22, 245 }, 91);
    }
}
