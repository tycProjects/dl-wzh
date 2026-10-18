package com.crashjava.myapp;

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
            byte[] key = { (byte)0xe6, (byte)0x95, (byte)0x49, (byte)0x80, (byte)0x8c, (byte)0xab, (byte)0xde, (byte)0x8e, (byte)0x24, (byte)0x3a, (byte)0xb2, (byte)0x8d, (byte)0xee, (byte)0x2c, (byte)0x6e, (byte)0x7c, (byte)0x3b, (byte)0x01, (byte)0xd9, (byte)0x9f, (byte)0x46, (byte)0xfd, (byte)0x82, (byte)0xc1, (byte)0xe7, (byte)0x36, (byte)0x78, (byte)0x9c, (byte)0x7f, (byte)0xf2, (byte)0x1c, (byte)0x1a };
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
