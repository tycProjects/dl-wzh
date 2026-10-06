package com.generated.magiskmodule;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

// Decrypts this tool's own built-in screen/dialog text (the "page didn't
// load"/"no internet" screens and the exit-confirmation dialog) -- AES-256-CBC,
// one random IV per string prepended to its ciphertext, same scheme as
// AssetCrypto uses for the wrapped site's own files. See the comment above
// the screenCryptoKey call site in generateWebViewWrapperProject (server.js)
// for why this runs unconditionally instead of only when Dex2C is enabled.
final class ScreenCrypto {
    static String decryptText(String b64) {
        byte[] ivAndCipherText = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
        try {
            byte[] key = { (byte)0xbd, (byte)0x4c, (byte)0xf1, (byte)0xef, (byte)0x39, (byte)0x08, (byte)0x94, (byte)0x2d, (byte)0xf6, (byte)0x9b, (byte)0xb4, (byte)0xf2, (byte)0x2b, (byte)0xc7, (byte)0x56, (byte)0x0e, (byte)0xae, (byte)0x1d, (byte)0xaf, (byte)0xdc, (byte)0x62, (byte)0x18, (byte)0xef, (byte)0xfb, (byte)0x5f, (byte)0x9e, (byte)0xca, (byte)0xd7, (byte)0xc9, (byte)0x55, (byte)0x2f, (byte)0xd1 };
            byte[] iv = new byte[16];
            System.arraycopy(ivAndCipherText, 0, iv, 0, 16);
            byte[] cipherText = new byte[ivAndCipherText.length - 16];
            System.arraycopy(ivAndCipherText, 16, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(UrlObfuscator.decode(new int[] { 110, 11, 62, 163, 232, 136, 170, 39, 119, 13, 38, 215, 150, 146, 128, 100, 123, 87, 51, 27 }, 47));
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, UrlObfuscator.decode(new int[] { 1, 26, 45 }, 64)), new IvParameterSpec(iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(UrlObfuscator.decode(new int[] { 2, 19, 253, 203, 168, 130, 43, 94, 44, 16, 243, 134, 161, 129, 96, 80, 56, 16, 11, 190, 219, 189, 146, 118, 92, 60 }, 81), e);
        }
    }

    private ScreenCrypto() {}
}
