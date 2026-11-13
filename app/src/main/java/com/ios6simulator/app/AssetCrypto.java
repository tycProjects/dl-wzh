package com.ios6simulator.app;

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
            byte[] key = { (byte)0x3c, (byte)0xac, (byte)0x37, (byte)0xc7, (byte)0x33, (byte)0x44, (byte)0x84, (byte)0x02, (byte)0x65, (byte)0x6c, (byte)0x03, (byte)0xbe, (byte)0x19, (byte)0x36, (byte)0x3c, (byte)0xd2, (byte)0x5a, (byte)0x70, (byte)0x0a, (byte)0x0e, (byte)0x92, (byte)0x80, (byte)0xe7, (byte)0xe6, (byte)0x65, (byte)0x55, (byte)0xf6, (byte)0x5d, (byte)0x76, (byte)0x74, (byte)0x4b, (byte)0x1a };
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
