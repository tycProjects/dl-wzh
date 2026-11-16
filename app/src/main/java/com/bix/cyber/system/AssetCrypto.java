package com.bix.cyber.system;

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
            byte[] key = { (byte)0x58, (byte)0x5f, (byte)0x3b, (byte)0xbe, (byte)0x07, (byte)0xd3, (byte)0x79, (byte)0x36, (byte)0x9e, (byte)0x6c, (byte)0xca, (byte)0x7f, (byte)0x30, (byte)0xbe, (byte)0x15, (byte)0xdd, (byte)0x68, (byte)0xab, (byte)0x0d, (byte)0x32, (byte)0xa2, (byte)0x58, (byte)0xeb, (byte)0x30, (byte)0xd0, (byte)0xcd, (byte)0x6b, (byte)0x83, (byte)0xd6, (byte)0xf4, (byte)0x24, (byte)0xce };
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
