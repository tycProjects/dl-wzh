package com.generated.xxi1;

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
            byte[] key = { (byte)0xdb, (byte)0x45, (byte)0x79, (byte)0xf5, (byte)0x8d, (byte)0xbe, (byte)0xe2, (byte)0x2f, (byte)0xf6, (byte)0x67, (byte)0x52, (byte)0x1e, (byte)0x66, (byte)0x58, (byte)0xdd, (byte)0x26, (byte)0xa1, (byte)0xed, (byte)0x92, (byte)0xab, (byte)0x8c, (byte)0x3d, (byte)0xbf, (byte)0xa6, (byte)0xa9, (byte)0x3c, (byte)0x92, (byte)0x39, (byte)0x89, (byte)0xf7, (byte)0x9d, (byte)0xce };
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
