package com.generated.app;

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
            byte[] key = { (byte)0xfa, (byte)0x7d, (byte)0x98, (byte)0xc0, (byte)0x36, (byte)0xdd, (byte)0x97, (byte)0xaa, (byte)0x4f, (byte)0x5d, (byte)0x69, (byte)0x26, (byte)0x5f, (byte)0xf6, (byte)0x7e, (byte)0xc3, (byte)0x8e, (byte)0xb7, (byte)0xa8, (byte)0xe1, (byte)0x8e, (byte)0xef, (byte)0xc0, (byte)0x45, (byte)0x94, (byte)0x88, (byte)0x39, (byte)0x6e, (byte)0xac, (byte)0xd6, (byte)0xdf, (byte)0x9e };
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
