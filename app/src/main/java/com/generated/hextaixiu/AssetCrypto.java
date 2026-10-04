package com.generated.hextaixiu;

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
            byte[] key = { (byte)0x91, (byte)0xcd, (byte)0x55, (byte)0xdc, (byte)0x00, (byte)0xed, (byte)0xb9, (byte)0xb1, (byte)0xed, (byte)0x28, (byte)0x7a, (byte)0x0e, (byte)0xae, (byte)0x21, (byte)0xcd, (byte)0x38, (byte)0xf3, (byte)0x87, (byte)0x7d, (byte)0x49, (byte)0x1f, (byte)0x5c, (byte)0x8d, (byte)0xf6, (byte)0xdd, (byte)0x96, (byte)0x1f, (byte)0x5d, (byte)0x75, (byte)0x22, (byte)0x3f, (byte)0xfc };
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
