package com.generated.crash4devman07com;

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
            byte[] key = { (byte)0x9e, (byte)0x0e, (byte)0x9b, (byte)0x98, (byte)0x31, (byte)0x9c, (byte)0x35, (byte)0x2a, (byte)0xab, (byte)0x63, (byte)0x8d, (byte)0x89, (byte)0x60, (byte)0xd0, (byte)0x42, (byte)0xd8, (byte)0x26, (byte)0xfb, (byte)0x49, (byte)0xac, (byte)0x0e, (byte)0x31, (byte)0x9b, (byte)0x51, (byte)0x49, (byte)0xb4, (byte)0x27, (byte)0xcc, (byte)0x25, (byte)0x1e, (byte)0xc6, (byte)0xa0 };
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
