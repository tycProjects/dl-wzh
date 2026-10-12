package com.generated.infimusicplayer;

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
            byte[] key = { (byte)0xe1, (byte)0x75, (byte)0xef, (byte)0x2e, (byte)0xd0, (byte)0xf9, (byte)0x26, (byte)0x08, (byte)0x26, (byte)0x1e, (byte)0x4b, (byte)0x62, (byte)0x0c, (byte)0x87, (byte)0x3b, (byte)0x0a, (byte)0x47, (byte)0x04, (byte)0xa5, (byte)0x53, (byte)0x46, (byte)0x0d, (byte)0xbe, (byte)0xd0, (byte)0x00, (byte)0x1e, (byte)0xf2, (byte)0x8a, (byte)0x01, (byte)0x1d, (byte)0x74, (byte)0xa3 };
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
