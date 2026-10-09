package com.generated.lkptshj;

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
            byte[] key = { (byte)0xde, (byte)0x7f, (byte)0x55, (byte)0x86, (byte)0x25, (byte)0x7b, (byte)0xbf, (byte)0xe7, (byte)0x07, (byte)0xd5, (byte)0x85, (byte)0xb8, (byte)0x47, (byte)0x94, (byte)0xd1, (byte)0x3d, (byte)0xf5, (byte)0xc8, (byte)0x0f, (byte)0x52, (byte)0xa4, (byte)0xa3, (byte)0xce, (byte)0x3c, (byte)0xe1, (byte)0x6a, (byte)0xd7, (byte)0x00, (byte)0x61, (byte)0x60, (byte)0x0b, (byte)0x09 };
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
