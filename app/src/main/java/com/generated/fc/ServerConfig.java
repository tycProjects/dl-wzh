package com.generated.fc;

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
        return decode(new int[] { 197, 184, 159, 122, 90, 114, 72, 169, 210, 179, 148, 44, 68, 33, 113, 29, 242, 209, 244, 155, 107, 23, 48, 23, 248, 209, 160, 221, 116, 81, 98, 29, 253, 195, 185, 158, 122, 5, 33, 5, 170, 214, 162, 150, 104, 78, 88, 45, 82, 236, 212, 169, 144, 108, 94, 57, 27, 231, 158, 160, 144, 100, 70, 32, 10, 255, 132, 169, 140, 102, 83, 35, 23, 169, 206, 171, 133, 102, 118, 91, 49, 24, 254, 200, 246, 201, 35 }, 173);
    }

    // Exact Dex2C target: the update API server URL.
    public static String getUpdateServerUrl() {
        return decode(new int[] { 51, 14, 237, 200, 164, 204, 58, 27, 41, 27, 225, 157, 187, 129, 32, 77, 59, 1, 164, 145, 182, 141, 53, 10, 44, 12, 243, 197, 209, 186, 152, 110, 21, 57, 22, 245, 155, 190, 129, 96, 67, 33, 75, 191, 128, 180, 132, 124, 6, 62, 6, 165, 198, 182, 142, 42, 83, 35, 6, 229, 236, 144, 185, 153, 109 }, 91);
    }
}
