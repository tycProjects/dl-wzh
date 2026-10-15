package com.generated.party2;

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
            byte[] key = { (byte)0xbd, (byte)0x7c, (byte)0x2c, (byte)0xa2, (byte)0xdf, (byte)0xf6, (byte)0x7f, (byte)0xb4, (byte)0x7d, (byte)0xdf, (byte)0xde, (byte)0xa5, (byte)0xf5, (byte)0x1f, (byte)0x99, (byte)0xbe, (byte)0xeb, (byte)0x96, (byte)0xc2, (byte)0x59, (byte)0xb3, (byte)0xc4, (byte)0x42, (byte)0xdf, (byte)0x3e, (byte)0xe3, (byte)0xeb, (byte)0x8b, (byte)0x46, (byte)0xb6, (byte)0x94, (byte)0x14 };
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
