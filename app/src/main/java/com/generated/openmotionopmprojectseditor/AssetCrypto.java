package com.generated.openmotionopmprojectseditor;

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
            byte[] key = { (byte)0x97, (byte)0x47, (byte)0x71, (byte)0xe7, (byte)0xf0, (byte)0x18, (byte)0x3c, (byte)0x51, (byte)0x34, (byte)0xde, (byte)0x9c, (byte)0xa6, (byte)0x70, (byte)0x0c, (byte)0xcd, (byte)0xdc, (byte)0x41, (byte)0x4f, (byte)0x39, (byte)0xac, (byte)0xe6, (byte)0x5c, (byte)0xe1, (byte)0xa9, (byte)0xb9, (byte)0xee, (byte)0x74, (byte)0x12, (byte)0xa8, (byte)0xab, (byte)0x78, (byte)0xcb };
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
