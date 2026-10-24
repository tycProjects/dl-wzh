package com.progamer.clima;

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
        return decode(new int[] { 197, 184, 159, 122, 90, 114, 72, 169, 210, 179, 148, 44, 64, 35, 60, 11, 234, 217, 186, 142, 113, 93, 37, 88, 246, 219, 190, 221, 116, 67, 96, 15, 255, 131, 168, 133, 100, 71, 35, 9, 247, 203, 238, 144, 104, 86, 94, 58, 28, 234, 210, 187, 214, 43, 7, 111, 69, 187, 196, 183, 144, 100, 71, 43, 31, 161, 205, 165, 155, 109, 68, 39, 22, 240, 140, 241, 209, 57, 47 }, 173);
    }

    // Exact Dex2C target: the update API server URL.
    public static String getUpdateServerUrl() {
        return decode(new int[] { 51, 14, 237, 200, 164, 204, 58, 27, 41, 27, 225, 157, 187, 129, 32, 77, 59, 1, 164, 145, 182, 141, 53, 10, 44, 12, 243, 197, 209, 186, 152, 110, 21, 57, 22, 245, 155, 190, 129, 96, 67, 33, 75, 191, 128, 180, 132, 124, 6, 62, 6, 165, 198, 182, 142, 42, 83, 35, 6, 229, 236, 144, 185, 153, 109 }, 91);
    }
}
