package com.generated.jkl;

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
        return decode(new int[] { 197, 184, 159, 122, 90, 114, 72, 169, 200, 165, 141, 106, 84, 33, 42, 13, 179, 223, 180, 151, 54, 85, 54, 24, 242, 213, 254, 149, 116, 94, 61, 11, 162, 193, 170, 132, 97, 95, 38, 73 }, 173);
    }

    // Exact Dex2C target: the update API server URL.
    public static String getUpdateServerUrl() {
        return decode(new int[] { 51, 14, 237, 200, 164, 204, 58, 27, 41, 27, 225, 157, 187, 129, 32, 77, 59, 1, 164, 203, 162, 211, 54, 10, 44, 12, 243, 197, 209, 186, 152, 110, 21, 57, 22, 245 }, 91);
    }
}
