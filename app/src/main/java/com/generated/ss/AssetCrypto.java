package com.generated.ss;

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
            byte[] key = { (byte)0x57, (byte)0x73, (byte)0x36, (byte)0x85, (byte)0x57, (byte)0x03, (byte)0x56, (byte)0xe0, (byte)0x05, (byte)0xef, (byte)0xb4, (byte)0x4a, (byte)0xd2, (byte)0x12, (byte)0x34, (byte)0xcc, (byte)0x38, (byte)0xe8, (byte)0x66, (byte)0xe7, (byte)0x6d, (byte)0x7f, (byte)0x40, (byte)0xfe, (byte)0x60, (byte)0xd1, (byte)0xe0, (byte)0xf5, (byte)0xdc, (byte)0x6d, (byte)0x77, (byte)0x2b };
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
