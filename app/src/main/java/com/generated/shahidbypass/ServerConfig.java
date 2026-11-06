package com.generated.shahidbypass;

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
        return decode(new int[] { 197, 184, 159, 122, 90, 114, 72, 169, 215, 171, 135, 99, 68, 45, 48, 10, 242, 206, 245, 153, 118, 85, 120, 5, 225, 209, 191, 158, 112, 94, 59, 7, 254, 129, 166, 133, 122, 92, 53, 7, 168, 210, 162, 140, 44, 67, 80, 51, 13, 253, 216, 174, 152, 53, 82, 123, 3, 241, 218, 177, 132, 124, 64, 99, 12, 249, 223, 165, 135, 103, 74, 41, 72, 244, 194, 176, 128, 45, 122, 80, 41, 14, 254, 221, 184, 139, 58, 67, 39, 22, 242, 220, 176, 131, 32 }, 173);
    }

    // Exact Dex2C target: the update API server URL.
    public static String getUpdateServerUrl() {
        return decode(new int[] { 51, 14, 237, 200, 164, 204, 58, 27, 41, 27, 225, 157, 187, 129, 32, 77, 59, 1, 164, 145, 182, 141, 53, 10, 44, 12, 243, 197, 209, 186, 152, 110, 21, 57, 22, 245, 155, 190, 129, 96, 67, 33, 75, 191, 128, 180, 132, 124, 6, 62, 6, 165, 198, 182, 142, 42, 83, 35, 6, 229, 236, 144, 185, 153, 109, 22, 49, 12, 227, 198, 166, 206, 60, 29, 43, 25, 255, 131, 185, 131, 38, 75, 57, 3, 170, 197, 164, 142, 109, 12, 46, 14, 13, 251, 211, 184, 158, 104, 23, 59, 24, 251 }, 91);
    }
}
