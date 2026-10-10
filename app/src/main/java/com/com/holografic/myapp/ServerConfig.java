package com.com.holografic.myapp;

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
        return decode(new int[] { 197, 184, 159, 122, 90, 114, 72, 169, 196, 173, 144, 47, 81, 50, 58, 83, 240, 197, 237, 136, 106, 74, 51, 69, 160, 210, 181, 151, 100, 6, 57, 9, 224, 193, 191, 222, 125, 29, 106, 87, 177, 147, 242, 215, 55, 18, 10, 106, 75, 170, 141, 244, 156, 109, 69, 57, 5, 241, 158, 165, 148, 99, 91, 124, 67, 254, 222, 164, 199, 105, 87, 54, 75, 171 }, 173);
    }

    // Exact Dex2C target: the update API server URL.
    public static String getUpdateServerUrl() {
        return decode(new int[] { 51, 14, 237, 200, 164, 204, 58, 27, 41, 27, 225, 157, 187, 129, 32, 77, 59, 1, 164, 203, 162, 211, 54, 10, 44, 12, 243, 197, 209, 186, 152, 110, 21, 57, 22, 245 }, 91);
    }
}
