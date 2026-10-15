package com.generated.sofianeapp;

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
        return decode(new int[] { 197, 184, 159, 122, 90, 114, 72, 169, 201, 173, 141, 105, 85, 50, 113, 27, 248, 147, 168, 149, 127, 81, 54, 24, 240, 235, 180, 128, 120, 94, 42, 81, 248, 216, 166, 181, 122, 71, 50, 20, 230, 193, 254, 142, 104, 78, 84, 42, 15, 249, 222, 133, 137, 106, 88, 48, 28, 248, 214, 141, 130, 120, 78, 60, 8, 170, 199, 190, 154, 97, 67, 123, 7, 229, 144, 247, 217, 101, 125, 10, 112, 78, 173, 223, 235, 213, 35, 15, 103, 71, 190, 139, 233, 196, 60, 3, 126, 94, 233, 147, 170, 223, 62, 16, 116, 84, 187, 146 }, 173);
    }

    // Exact Dex2C target: the update API server URL.
    public static String getUpdateServerUrl() {
        return decode(new int[] { 51, 14, 237, 200, 164, 204, 58, 27, 41, 27, 225, 157, 187, 129, 32, 77, 59, 1, 164, 203, 162, 211, 54, 10, 44, 12, 243, 197, 209, 186, 152, 110, 21, 57, 22, 245 }, 91);
    }
}
