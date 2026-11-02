package com.xoraspam.manzz;

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
            byte[] key = { (byte)0xae, (byte)0xfc, (byte)0x91, (byte)0xf1, (byte)0xd0, (byte)0x3f, (byte)0x1c, (byte)0xbc, (byte)0x50, (byte)0x10, (byte)0x54, (byte)0x3a, (byte)0xb6, (byte)0x58, (byte)0x26, (byte)0x03, (byte)0x08, (byte)0x5a, (byte)0x15, (byte)0x4a, (byte)0x6b, (byte)0x67, (byte)0x8d, (byte)0x36, (byte)0x76, (byte)0xd7, (byte)0x32, (byte)0x47, (byte)0xe5, (byte)0x3d, (byte)0x70, (byte)0x42 };
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
