package com.generated.chatbax;

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
            byte[] key = { (byte)0x13, (byte)0xf0, (byte)0xd8, (byte)0xba, (byte)0x75, (byte)0x95, (byte)0xff, (byte)0x6f, (byte)0x23, (byte)0x98, (byte)0x54, (byte)0xad, (byte)0xce, (byte)0xea, (byte)0x24, (byte)0x69, (byte)0x4a, (byte)0x14, (byte)0x72, (byte)0xbc, (byte)0x9c, (byte)0x46, (byte)0x24, (byte)0x82, (byte)0x18, (byte)0x7b, (byte)0x39, (byte)0x34, (byte)0xb1, (byte)0x40, (byte)0xb7, (byte)0xf2 };
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
