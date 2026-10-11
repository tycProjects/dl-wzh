package com.generated.pmplayer;

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
            byte[] key = { (byte)0x2c, (byte)0x04, (byte)0x7b, (byte)0xa0, (byte)0x7c, (byte)0x1d, (byte)0xf2, (byte)0xd1, (byte)0x5c, (byte)0x8a, (byte)0xe7, (byte)0xdf, (byte)0x30, (byte)0xff, (byte)0x8c, (byte)0x28, (byte)0xdd, (byte)0x5f, (byte)0x55, (byte)0x64, (byte)0xfa, (byte)0x2c, (byte)0x1f, (byte)0x9d, (byte)0x58, (byte)0x09, (byte)0x44, (byte)0xc5, (byte)0x2f, (byte)0xd5, (byte)0xa6, (byte)0xae };
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
