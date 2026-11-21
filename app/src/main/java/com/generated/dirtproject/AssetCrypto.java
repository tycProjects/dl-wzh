package com.generated.dirtproject;

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
            byte[] key = { (byte)0xb1, (byte)0x5c, (byte)0x53, (byte)0xc9, (byte)0xcd, (byte)0x3a, (byte)0x7f, (byte)0xaf, (byte)0xed, (byte)0xa1, (byte)0x31, (byte)0xd3, (byte)0x81, (byte)0x16, (byte)0xf2, (byte)0x5d, (byte)0x81, (byte)0x1f, (byte)0xd6, (byte)0x0e, (byte)0x8e, (byte)0xc3, (byte)0x94, (byte)0xbf, (byte)0x8e, (byte)0x0f, (byte)0xe3, (byte)0xbb, (byte)0x63, (byte)0xa2, (byte)0x80, (byte)0x65 };
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
