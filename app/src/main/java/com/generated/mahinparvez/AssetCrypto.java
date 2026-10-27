package com.generated.mahinparvez;

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
            byte[] key = { (byte)0xff, (byte)0x9c, (byte)0x88, (byte)0x33, (byte)0xfc, (byte)0x75, (byte)0x2b, (byte)0x3e, (byte)0x58, (byte)0xd5, (byte)0x1c, (byte)0xef, (byte)0x8f, (byte)0xd2, (byte)0xe5, (byte)0x00, (byte)0x3a, (byte)0x08, (byte)0x7a, (byte)0xe5, (byte)0xa9, (byte)0xb9, (byte)0x7d, (byte)0x0e, (byte)0x66, (byte)0x0c, (byte)0x37, (byte)0x49, (byte)0xaa, (byte)0x35, (byte)0xc7, (byte)0x26 };
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
