package com.generated.volleyclash;

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
            byte[] key = { (byte)0xd0, (byte)0x0a, (byte)0x36, (byte)0x1d, (byte)0x09, (byte)0xfe, (byte)0xe9, (byte)0x64, (byte)0xc2, (byte)0x19, (byte)0xfc, (byte)0x51, (byte)0xf5, (byte)0x95, (byte)0xcd, (byte)0xe6, (byte)0xd8, (byte)0x15, (byte)0xd6, (byte)0xdb, (byte)0x64, (byte)0xab, (byte)0xaf, (byte)0x88, (byte)0xb7, (byte)0x6d, (byte)0x9b, (byte)0x77, (byte)0x46, (byte)0x1b, (byte)0x9a, (byte)0x3a };
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
