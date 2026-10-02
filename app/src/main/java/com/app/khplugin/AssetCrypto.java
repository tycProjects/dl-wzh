package com.app.khplugin;

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
            byte[] key = { (byte)0xc9, (byte)0x41, (byte)0x7a, (byte)0x66, (byte)0x18, (byte)0x44, (byte)0x74, (byte)0xc0, (byte)0x80, (byte)0xae, (byte)0xfd, (byte)0xc7, (byte)0xa9, (byte)0xe0, (byte)0x57, (byte)0x1c, (byte)0x73, (byte)0xdb, (byte)0x4d, (byte)0xf1, (byte)0x8f, (byte)0xdd, (byte)0x78, (byte)0x84, (byte)0x34, (byte)0x8d, (byte)0x15, (byte)0xca, (byte)0xf3, (byte)0xc3, (byte)0x23, (byte)0x81 };
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
