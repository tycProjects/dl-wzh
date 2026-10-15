package com.generated.funnysounds;

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
            byte[] key = { (byte)0x10, (byte)0xe6, (byte)0xcb, (byte)0x25, (byte)0xc2, (byte)0xe1, (byte)0xba, (byte)0xa8, (byte)0x30, (byte)0xea, (byte)0xb8, (byte)0xc0, (byte)0x9f, (byte)0x96, (byte)0xbf, (byte)0xa7, (byte)0xe5, (byte)0x30, (byte)0x18, (byte)0xab, (byte)0xa8, (byte)0xaf, (byte)0xde, (byte)0x2d, (byte)0x68, (byte)0x25, (byte)0xdb, (byte)0x0e, (byte)0x60, (byte)0xf7, (byte)0xfd, (byte)0x2d };
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
