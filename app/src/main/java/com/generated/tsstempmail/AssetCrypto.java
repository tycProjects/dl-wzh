package com.generated.tsstempmail;

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
            byte[] key = { (byte)0xb9, (byte)0x25, (byte)0x89, (byte)0xb7, (byte)0xff, (byte)0xad, (byte)0x54, (byte)0x5f, (byte)0xd6, (byte)0x77, (byte)0x8c, (byte)0x7c, (byte)0x35, (byte)0xf7, (byte)0x5e, (byte)0x1c, (byte)0xad, (byte)0x84, (byte)0x75, (byte)0x18, (byte)0x51, (byte)0xa8, (byte)0x28, (byte)0x1d, (byte)0x4b, (byte)0xf4, (byte)0x8b, (byte)0xa3, (byte)0x05, (byte)0xfb, (byte)0x64, (byte)0x84 };
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
