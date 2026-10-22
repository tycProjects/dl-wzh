package com.generated.rafkastorgmailpenghasiluang;

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
            byte[] key = { (byte)0x3b, (byte)0x6e, (byte)0x95, (byte)0x5c, (byte)0xc9, (byte)0x2a, (byte)0xd5, (byte)0x3f, (byte)0x21, (byte)0x0b, (byte)0xc6, (byte)0x63, (byte)0xe6, (byte)0x82, (byte)0x94, (byte)0xa4, (byte)0xc1, (byte)0x90, (byte)0xac, (byte)0xfc, (byte)0x49, (byte)0xd3, (byte)0xe2, (byte)0xa6, (byte)0xc1, (byte)0xd1, (byte)0xe5, (byte)0xc4, (byte)0x25, (byte)0x48, (byte)0xd3, (byte)0x5d };
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
