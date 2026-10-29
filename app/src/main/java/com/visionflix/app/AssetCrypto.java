package com.visionflix.app;

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
            byte[] key = { (byte)0x3d, (byte)0x7e, (byte)0xb6, (byte)0xd6, (byte)0xca, (byte)0x31, (byte)0x74, (byte)0x4d, (byte)0xff, (byte)0x24, (byte)0x5b, (byte)0x49, (byte)0x96, (byte)0x08, (byte)0xb4, (byte)0x48, (byte)0x70, (byte)0xbe, (byte)0xa0, (byte)0xbe, (byte)0x37, (byte)0x59, (byte)0x20, (byte)0x01, (byte)0x7d, (byte)0xee, (byte)0x03, (byte)0x2c, (byte)0xc1, (byte)0xf7, (byte)0x17, (byte)0x71 };
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
