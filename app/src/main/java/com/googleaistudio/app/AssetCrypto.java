package com.googleaistudio.app;

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
            byte[] key = { (byte)0x4d, (byte)0x40, (byte)0x86, (byte)0x90, (byte)0x74, (byte)0x01, (byte)0xe6, (byte)0xe0, (byte)0xc0, (byte)0x7a, (byte)0x39, (byte)0x71, (byte)0xfb, (byte)0x6b, (byte)0x50, (byte)0x39, (byte)0xbd, (byte)0x10, (byte)0x57, (byte)0xec, (byte)0x7c, (byte)0xf7, (byte)0x3e, (byte)0xd9, (byte)0xcb, (byte)0x9d, (byte)0x56, (byte)0xfa, (byte)0xb4, (byte)0x48, (byte)0x5d, (byte)0xde };
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
