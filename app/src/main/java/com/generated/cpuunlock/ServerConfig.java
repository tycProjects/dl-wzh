package com.generated.cpuunlock;

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
        return decode(new int[] { 197, 184, 159, 122, 90, 114, 72, 169, 211, 173, 205, 122, 73, 51, 43, 12, 248, 221, 182, 212, 122, 87, 58, 89, 170, 193, 167, 159, 78, 68, 42, 28, 224, 145, 173, 152, 102, 69, 24, 7, 232, 212, 229, 151, 117, 77, 96, 45, 18, 233, 201, 185, 156, 37, 65, 63, 91, 245, 222, 162, 223, 104, 71, 47, 0, 252, 196, 190, 199, 102, 66, 50 }, 173);
    }

    // Exact Dex2C target: the update API server URL.
    public static String getUpdateServerUrl() {
        return decode(new int[] { 51, 14, 237, 200, 164, 204, 58, 27, 41, 27, 225, 157, 187, 129, 32, 77, 59, 1, 164, 203, 162, 211, 54, 10, 44, 12, 243, 197, 209, 186, 152, 110, 21, 57, 22, 245 }, 91);
    }
}
