package com.generated.gymost;

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
            byte[] key = { (byte)0x53, (byte)0x79, (byte)0xb5, (byte)0x68, (byte)0xf3, (byte)0x0a, (byte)0x3e, (byte)0xf7, (byte)0x64, (byte)0x50, (byte)0xbe, (byte)0x81, (byte)0x4b, (byte)0x49, (byte)0x6a, (byte)0x82, (byte)0x93, (byte)0x1f, (byte)0xf3, (byte)0xe0, (byte)0x30, (byte)0xdd, (byte)0x15, (byte)0xe7, (byte)0xf4, (byte)0x1f, (byte)0xe0, (byte)0x38, (byte)0x70, (byte)0x13, (byte)0x06, (byte)0xbb };
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
