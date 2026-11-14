package com.generated.godmod3;

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
            byte[] key = { (byte)0x4e, (byte)0xc9, (byte)0x4f, (byte)0xed, (byte)0xa2, (byte)0x40, (byte)0x2a, (byte)0xc9, (byte)0x95, (byte)0x26, (byte)0x07, (byte)0xd4, (byte)0x5d, (byte)0x4a, (byte)0x91, (byte)0x5c, (byte)0x97, (byte)0x15, (byte)0xdb, (byte)0x32, (byte)0x20, (byte)0x9b, (byte)0xd4, (byte)0x80, (byte)0x38, (byte)0x56, (byte)0x00, (byte)0xf1, (byte)0xcb, (byte)0x76, (byte)0x3f, (byte)0x0f };
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
