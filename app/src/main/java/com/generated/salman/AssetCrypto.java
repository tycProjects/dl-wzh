package com.generated.salman;

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
            byte[] key = { (byte)0x0c, (byte)0x18, (byte)0x6e, (byte)0xf9, (byte)0x72, (byte)0x86, (byte)0x92, (byte)0xd1, (byte)0x34, (byte)0x38, (byte)0xa2, (byte)0x97, (byte)0xb0, (byte)0x54, (byte)0x27, (byte)0xe1, (byte)0x81, (byte)0x1a, (byte)0xab, (byte)0x68, (byte)0xaf, (byte)0x52, (byte)0xf4, (byte)0x73, (byte)0x91, (byte)0xba, (byte)0x45, (byte)0x74, (byte)0xb1, (byte)0x69, (byte)0x94, (byte)0xaa };
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
