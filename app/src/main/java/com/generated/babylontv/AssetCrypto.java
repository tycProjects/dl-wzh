package com.generated.babylontv;

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
            byte[] key = { (byte)0x5b, (byte)0x5b, (byte)0x19, (byte)0x78, (byte)0x3b, (byte)0xa7, (byte)0xe6, (byte)0x67, (byte)0xb3, (byte)0x94, (byte)0x84, (byte)0x68, (byte)0xa6, (byte)0x50, (byte)0x2a, (byte)0x4d, (byte)0x98, (byte)0xf5, (byte)0x47, (byte)0xbf, (byte)0x61, (byte)0xf5, (byte)0x8b, (byte)0x63, (byte)0x1a, (byte)0xaf, (byte)0xa0, (byte)0x34, (byte)0x2d, (byte)0x6d, (byte)0x1f, (byte)0x1f };
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
