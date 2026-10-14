package com.generated.hgzyreview;

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
            byte[] key = { (byte)0x92, (byte)0x90, (byte)0x25, (byte)0x76, (byte)0x5e, (byte)0x42, (byte)0x7a, (byte)0xd1, (byte)0x90, (byte)0x2c, (byte)0x9a, (byte)0x10, (byte)0x8f, (byte)0x98, (byte)0xde, (byte)0x70, (byte)0x9f, (byte)0x01, (byte)0x3f, (byte)0x60, (byte)0x0f, (byte)0x36, (byte)0x22, (byte)0xb0, (byte)0xc2, (byte)0x2d, (byte)0x88, (byte)0xf7, (byte)0x12, (byte)0xe2, (byte)0xea, (byte)0x25 };
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
