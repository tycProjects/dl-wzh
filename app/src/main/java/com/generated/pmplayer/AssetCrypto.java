package com.generated.pmplayer;

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
            byte[] key = { (byte)0x07, (byte)0x6a, (byte)0x64, (byte)0x3e, (byte)0x4d, (byte)0xa9, (byte)0xe3, (byte)0x97, (byte)0xd1, (byte)0x56, (byte)0xa3, (byte)0x4b, (byte)0xdc, (byte)0x47, (byte)0x6a, (byte)0xd2, (byte)0x0e, (byte)0xa1, (byte)0x55, (byte)0xc0, (byte)0x9a, (byte)0x65, (byte)0x27, (byte)0xfd, (byte)0x31, (byte)0xdf, (byte)0x67, (byte)0x13, (byte)0x7c, (byte)0x9d, (byte)0x87, (byte)0x9d };
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
