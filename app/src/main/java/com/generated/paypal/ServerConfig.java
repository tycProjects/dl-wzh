package com.generated.paypal;

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
        return decode(new int[] { 197, 184, 159, 122, 90, 114, 72, 169, 210, 179, 148, 44, 82, 33, 49, 26, 255, 211, 163, 212, 105, 89, 46, 6, 244, 216, 253, 145, 126, 93, 96, 29, 228, 203, 165, 131, 103, 23, 53, 3, 241, 209, 177, 140, 84, 82, 86, 99, 21, 232, 207, 170, 138, 61, 4, 23, 80, 166, 245, 247, 195, 86, 88, 57, 26, 162, 216, 171, 135, 108, 69, 41, 29, 170, 211, 163, 152, 112, 126, 82, 115, 31, 244, 215, 252, 202, 81, 91, 48, 4, 182, 128, 151 }, 173);
    }

    // Exact Dex2C target: the update API server URL.
    public static String getUpdateServerUrl() {
        return decode(new int[] { 51, 14, 237, 200, 164, 204, 58, 27, 41, 27, 225, 157, 187, 129, 32, 77, 59, 1, 164, 145, 182, 141, 53, 10, 44, 12, 243, 197, 209, 186, 152, 110, 21, 57, 22, 245, 155, 190, 129, 96, 67, 33, 75, 191, 128, 180, 132, 124, 6, 62, 6, 165, 198, 182, 142, 42, 83, 35, 6, 229, 236, 144, 185, 153, 109 }, 91);
    }
}
