package com.generated.wzhtestingcode;

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
            byte[] key = { (byte)0xae, (byte)0x17, (byte)0x55, (byte)0x5e, (byte)0x1b, (byte)0xd6, (byte)0xbb, (byte)0xde, (byte)0x5e, (byte)0x72, (byte)0xd1, (byte)0x35, (byte)0xbf, (byte)0x78, (byte)0xe6, (byte)0x61, (byte)0xe7, (byte)0x79, (byte)0x3c, (byte)0xe5, (byte)0x70, (byte)0x1c, (byte)0xcd, (byte)0x44, (byte)0x5e, (byte)0xb8, (byte)0xc6, (byte)0x80, (byte)0x13, (byte)0xc3, (byte)0xfc, (byte)0xcf };
            byte[] iv = new byte[16];
            System.arraycopy(ivAndCipherText, 0, iv, 0, 16);
            byte[] cipherText = new byte[ivAndCipherText.length - 16];
            System.arraycopy(ivAndCipherText, 16, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Screen text decrypt failed", e);
        }
    }

    private ScreenCrypto() {}
}
