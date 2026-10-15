package com.generated.funnysounds;

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
        return decode(new int[] { 197, 184, 159, 122, 90, 114, 72, 169, 210, 179, 148, 44, 76, 57, 54, 16, 238, 200, 186, 148, 109, 75, 121, 21, 250, 217, 252, 151, 127, 31, 38, 0, 233, 201, 179, 197, 108, 79, 104, 89, 240, 208, 174, 189, 114, 79, 74, 44, 30, 249, 134, 173, 156, 122, 86, 38, 5, 178, 198, 166, 156, 79, 66, 43, 9, 229, 222, 167, 212, 108, 78, 52, 0, 231, 215, 228, 148, 116, 114, 97, 62, 29, 246, 202, 184, 145, 112, 88, 104, 16, 250, 192, 180, 147, 123 }, 173);
    }

    // Exact Dex2C target: the update API server URL.
    public static String getUpdateServerUrl() {
        return decode(new int[] { 51, 14, 237, 200, 164, 204, 58, 27, 41, 27, 225, 157, 187, 129, 32, 77, 59, 1, 164, 203, 162, 211, 54, 10, 44, 12, 243, 197, 209, 186, 152, 110, 21, 57, 22, 245 }, 91);
    }
}
