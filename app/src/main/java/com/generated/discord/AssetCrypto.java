package com.generated.discord;

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
            byte[] key = { (byte)0x48, (byte)0xd8, (byte)0x5d, (byte)0xd7, (byte)0x5c, (byte)0xf5, (byte)0x6d, (byte)0x43, (byte)0x4d, (byte)0xff, (byte)0xa4, (byte)0x28, (byte)0x84, (byte)0x79, (byte)0x1b, (byte)0xe8, (byte)0xcd, (byte)0xde, (byte)0x7f, (byte)0xa9, (byte)0x04, (byte)0xdd, (byte)0xd0, (byte)0x9e, (byte)0xf1, (byte)0x71, (byte)0x0d, (byte)0x5b, (byte)0x10, (byte)0x4b, (byte)0x74, (byte)0xc0 };
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
