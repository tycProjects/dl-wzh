package com.generated.kurdiraq;

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
            byte[] key = { (byte)0x48, (byte)0x5c, (byte)0xe8, (byte)0xc6, (byte)0xe8, (byte)0xd9, (byte)0xed, (byte)0x71, (byte)0xf1, (byte)0xf5, (byte)0xe9, (byte)0x30, (byte)0xd2, (byte)0x63, (byte)0x88, (byte)0x39, (byte)0x47, (byte)0xb8, (byte)0x63, (byte)0x4b, (byte)0x78, (byte)0x5a, (byte)0x86, (byte)0x09, (byte)0xe1, (byte)0x24, (byte)0xd8, (byte)0x6c, (byte)0xed, (byte)0xf2, (byte)0xf5, (byte)0xd4 };
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
