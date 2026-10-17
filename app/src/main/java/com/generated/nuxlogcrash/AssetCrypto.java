package com.generated.nuxlogcrash;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// Decrypts EmbeddedAssets' bundled site content (AES-256-CBC, one random
// IV per file prepended to that file's ciphertext). The key intentionally
// lives inside decrypt()'s method body -- see the comment in
// embedAssetsAsCode (server.js) for exactly why that placement matters
// once Dex2C hardening runs on this class.
final class AssetCrypto {
    static byte[] decrypt(byte[] ivAndCipherText) {
        try {
            byte[] key = { (byte)0x89, (byte)0x10, (byte)0xb6, (byte)0xa7, (byte)0x3f, (byte)0xa8, (byte)0xbc, (byte)0x2c, (byte)0x6c, (byte)0x5b, (byte)0xe2, (byte)0xb9, (byte)0x3c, (byte)0xa3, (byte)0xfe, (byte)0x1d, (byte)0xb5, (byte)0xb2, (byte)0xcf, (byte)0x6e, (byte)0x45, (byte)0xe7, (byte)0x1a, (byte)0xde, (byte)0x54, (byte)0x30, (byte)0x09, (byte)0x44, (byte)0xa0, (byte)0x72, (byte)0x69, (byte)0x49 };
            byte[] iv = new byte[16];
            System.arraycopy(ivAndCipherText, 0, iv, 0, 16);
            byte[] cipherText = new byte[ivAndCipherText.length - 16];
            System.arraycopy(ivAndCipherText, 16, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Asset decrypt failed", e);
        }
    }

    private AssetCrypto() {}
}
