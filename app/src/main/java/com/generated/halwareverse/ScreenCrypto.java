package com.generated.halwareverse;

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
            byte[] key = { (byte)0x21, (byte)0xf9, (byte)0x90, (byte)0xc4, (byte)0x8e, (byte)0x02, (byte)0x61, (byte)0x46, (byte)0x63, (byte)0xda, (byte)0xde, (byte)0xb0, (byte)0x4d, (byte)0x0e, (byte)0x88, (byte)0xe5, (byte)0x3b, (byte)0xa0, (byte)0x84, (byte)0xb2, (byte)0x59, (byte)0x3c, (byte)0xd4, (byte)0xf9, (byte)0xd7, (byte)0x7d, (byte)0x19, (byte)0x48, (byte)0x0c, (byte)0x51, (byte)0xe4, (byte)0x40 };
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
