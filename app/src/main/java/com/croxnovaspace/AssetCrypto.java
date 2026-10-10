package com.croxnovaspace;

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
            byte[] key = { (byte)0xe2, (byte)0xbc, (byte)0x3e, (byte)0x24, (byte)0x29, (byte)0x4b, (byte)0xed, (byte)0xad, (byte)0xad, (byte)0x3c, (byte)0xfb, (byte)0xef, (byte)0xb8, (byte)0x3d, (byte)0xa3, (byte)0xfc, (byte)0xa3, (byte)0x4b, (byte)0x9e, (byte)0x21, (byte)0x9d, (byte)0xbb, (byte)0x7f, (byte)0x52, (byte)0xbc, (byte)0x71, (byte)0x7e, (byte)0xc4, (byte)0x05, (byte)0xcc, (byte)0xfb, (byte)0xf0 };
            byte[] iv = new byte[16];
            System.arraycopy(ivAndCipherText, 0, iv, 0, 16);
            byte[] cipherText = new byte[ivAndCipherText.length - 16];
            System.arraycopy(ivAndCipherText, 16, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(UrlObfuscator.decode(new int[] { 110, 11, 62, 163, 232, 136, 170, 39, 119, 13, 38, 215, 150, 146, 128, 100, 123, 87, 51, 27 }, 47));
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, UrlObfuscator.decode(new int[] { 1, 26, 45 }, 64)), new IvParameterSpec(iv));
            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new RuntimeException(UrlObfuscator.decode(new int[] { 16, 3, 252, 203, 185, 204, 111, 79, 42, 26, 254, 214, 177, 196, 101, 67, 40, 12, 26, 250 }, 81), e);
        }
    }

    private AssetCrypto() {}
}
