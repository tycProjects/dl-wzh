package com.generated.insta;

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
            byte[] key = { (byte)0x87, (byte)0xc5, (byte)0x71, (byte)0xbb, (byte)0x15, (byte)0x0e, (byte)0xf7, (byte)0x9c, (byte)0x8c, (byte)0x3b, (byte)0xae, (byte)0xd4, (byte)0x77, (byte)0xef, (byte)0x05, (byte)0x05, (byte)0xd4, (byte)0xbc, (byte)0x6b, (byte)0xcd, (byte)0x09, (byte)0x07, (byte)0xb0, (byte)0x02, (byte)0x96, (byte)0xce, (byte)0x77, (byte)0xe1, (byte)0x7a, (byte)0x2e, (byte)0xa0, (byte)0x02 };
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
