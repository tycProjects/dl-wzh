package com.generated.avitoprk;

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
            byte[] key = { (byte)0x0a, (byte)0x71, (byte)0xe5, (byte)0xe5, (byte)0x8e, (byte)0x76, (byte)0x09, (byte)0x49, (byte)0xa9, (byte)0xb9, (byte)0x0d, (byte)0x82, (byte)0x93, (byte)0x02, (byte)0xb0, (byte)0xa4, (byte)0xc5, (byte)0x3f, (byte)0xb1, (byte)0x14, (byte)0x8b, (byte)0x87, (byte)0x2f, (byte)0x61, (byte)0x6f, (byte)0xe0, (byte)0x2a, (byte)0xce, (byte)0xcb, (byte)0xe1, (byte)0x57, (byte)0xb3 };
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
