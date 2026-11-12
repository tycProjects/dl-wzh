package com.generated.changuitotareas;

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
            byte[] key = { (byte)0x2d, (byte)0x05, (byte)0xb3, (byte)0xe5, (byte)0x06, (byte)0xe4, (byte)0x41, (byte)0x51, (byte)0xdd, (byte)0x88, (byte)0xc8, (byte)0xe0, (byte)0xf6, (byte)0x40, (byte)0x65, (byte)0xaf, (byte)0xb6, (byte)0xe3, (byte)0xf3, (byte)0xd7, (byte)0x8a, (byte)0x23, (byte)0xd8, (byte)0x0e, (byte)0x95, (byte)0x63, (byte)0x86, (byte)0x1a, (byte)0x1e, (byte)0x84, (byte)0x00, (byte)0x13 };
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
