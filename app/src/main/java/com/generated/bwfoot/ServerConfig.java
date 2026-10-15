package com.generated.bwfoot;

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
        return decode(new int[] { 197, 184, 159, 122, 90, 114, 72, 169, 210, 165, 144, 99, 85, 40, 62, 83, 251, 206, 163, 136, 55, 79, 63, 23, 246, 223, 178, 220, 112, 64, 63, 65, 174, 131, 191, 143, 104, 69, 52, 73, 160, 224, 251, 199, 67, 20, 26, 26, 69, 185, 250, 226, 220, 92, 15, 115, 52, 163, 150, 150, 201, 53, 110, 118, 72, 190, 155, 239, 173, 48, 2, 4, 92, 161, 231, 251, 196, 56, 94, 27, 25, 69, 190, 130, 239, 221, 37, 6, 112, 48, 171, 151, 144, 199, 42, 106, 116, 73, 179, 158, 236, 172, 62, 3, 125, 87, 166, 230, 249, 197, 190, 38, 24, 24, 66, 191, 129, 153, 210, 82, 13, 113, 49, 163, 148, 148, 215, 43, 108, 117 }, 173);
    }

    // Exact Dex2C target: the update API server URL.
    public static String getUpdateServerUrl() {
        return decode(new int[] { 51, 14, 237, 200, 164, 204, 58, 27, 41, 27, 225, 157, 187, 129, 32, 77, 59, 1, 164, 203, 162, 211, 54, 10, 44, 12, 243, 197, 209, 186, 152, 110, 21, 57, 22, 245 }, 91);
    }
}
