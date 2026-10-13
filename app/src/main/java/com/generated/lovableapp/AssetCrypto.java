package com.generated.lovableapp;

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
            byte[] key = { (byte)0x92, (byte)0x8c, (byte)0xe2, (byte)0xf3, (byte)0xbc, (byte)0xaa, (byte)0xe5, (byte)0x88, (byte)0xdd, (byte)0x83, (byte)0x59, (byte)0x9f, (byte)0xa6, (byte)0x7f, (byte)0x28, (byte)0x9c, (byte)0xe7, (byte)0x8f, (byte)0x22, (byte)0x12, (byte)0x6d, (byte)0x8e, (byte)0xca, (byte)0x5c, (byte)0x70, (byte)0x28, (byte)0x75, (byte)0x6e, (byte)0xd3, (byte)0x0e, (byte)0xa7, (byte)0xe5 };
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
