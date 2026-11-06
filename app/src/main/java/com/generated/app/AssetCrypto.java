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
            byte[] key = { (byte)0xd2, (byte)0x11, (byte)0x99, (byte)0xba, (byte)0xe6, (byte)0xba, (byte)0x0d, (byte)0x6c, (byte)0x96, (byte)0x2b, (byte)0xe6, (byte)0x96, (byte)0x5d, (byte)0xf5, (byte)0x2c, (byte)0x67, (byte)0x42, (byte)0x9a, (byte)0xc3, (byte)0x6e, (byte)0x96, (byte)0xe1, (byte)0x58, (byte)0x72, (byte)0x1a, (byte)0xa6, (byte)0x53, (byte)0xb9, (byte)0x67, (byte)0x49, (byte)0xb7, (byte)0x6b };
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
