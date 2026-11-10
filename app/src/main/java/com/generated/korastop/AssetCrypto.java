package com.generated.korastop;

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
            byte[] key = { (byte)0xa4, (byte)0xcb, (byte)0x32, (byte)0x38, (byte)0x18, (byte)0x95, (byte)0xbd, (byte)0x53, (byte)0xca, (byte)0xaf, (byte)0xff, (byte)0x51, (byte)0xce, (byte)0x80, (byte)0x3a, (byte)0xe3, (byte)0x4d, (byte)0x64, (byte)0xe3, (byte)0x2a, (byte)0x36, (byte)0x87, (byte)0x95, (byte)0x64, (byte)0x0c, (byte)0xb2, (byte)0xfb, (byte)0x0c, (byte)0x9d, (byte)0xb2, (byte)0xbf, (byte)0x7e };
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
