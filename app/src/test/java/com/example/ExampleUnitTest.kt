package com.example

import com.example.crypto.CryptoEngine
import com.example.crypto.SecretFormat
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testEmojiEncodingAndDecodingEnglish() {
        val original = "Hello World! Secret meeting at midnight."
        val encoded = CryptoEngine.encodeEmoji(original)
        assertTrue(encoded.startsWith(CryptoEngine.PREFIX_EMOJI))
        assertTrue(encoded.endsWith(CryptoEngine.SUFFIX_EMOJI))

        val decoded = CryptoEngine.decodeEmoji(encoded)
        assertTrue(decoded.isSuccess)
        assertEquals(original, decoded.getOrNull())
    }

    @Test
    fun testEmojiEncodingAndDecodingBengali() {
        val original = "গোপন বার্তা: আজ রাত ৮টায় দেখা হবে!"
        val encoded = CryptoEngine.encodeEmoji(original)
        val decoded = CryptoEngine.decodeEmoji(encoded)
        assertTrue(decoded.isSuccess)
        assertEquals(original, decoded.getOrNull())
    }

    @Test
    fun testCharacterCipher() {
        val original = "Top secret authorization token 12345"
        val encoded = CryptoEngine.encodeCharacter(original)
        assertTrue(encoded.startsWith(CryptoEngine.PREFIX_CHAR))

        val decoded = CryptoEngine.decodeCharacter(encoded)
        assertTrue(decoded.isSuccess)
        assertEquals(original, decoded.getOrNull())
    }

    @Test
    fun testAes256GcmCorrectPassword() {
        val original = "Confidential military message with high security"
        val password = "SuperSecretPassword#2026!"
        val encrypted = CryptoEngine.encryptAes256Gcm(original, password)

        assertTrue(encrypted.startsWith(CryptoEngine.PREFIX_AES))
        assertTrue(encrypted.endsWith(CryptoEngine.SUFFIX_AES))

        val decrypted = CryptoEngine.decryptAes256Gcm(encrypted, password)
        assertTrue(decrypted.isSuccess)
        assertEquals(original, decrypted.getOrNull())
    }

    @Test
    fun testAes256GcmWrongPasswordFails() {
        val original = "Classified intelligence"
        val password = "CorrectPassword123"
        val encrypted = CryptoEngine.encryptAes256Gcm(original, password)

        val decrypted = CryptoEngine.decryptAes256Gcm(encrypted, "WrongPassword999")
        assertFalse(decrypted.isSuccess)
    }

    @Test
    fun testFormatDetection() {
        val emojiMsg = CryptoEngine.encodeEmoji("Test")
        assertEquals(SecretFormat.EMOJI, CryptoEngine.detectFormat(emojiMsg))

        val charMsg = CryptoEngine.encodeCharacter("Test")
        assertEquals(SecretFormat.CHARACTER, CryptoEngine.detectFormat(charMsg))

        val aesMsg = CryptoEngine.encryptAes256Gcm("Test", "pwd")
        assertEquals(SecretFormat.AES256_GCM, CryptoEngine.detectFormat(aesMsg))
    }

    @Test
    fun testSteganographyBengali() {
        val carrier = "🤔"
        val message = "বলদ"
        val encoded = CryptoEngine.encodeSteganography(carrier, message)

        assertTrue(CryptoEngine.hasSteganography(encoded))
        assertEquals(carrier, CryptoEngine.extractCarrier(encoded))

        val decoded = CryptoEngine.decodeSteganography(encoded)
        assertTrue(decoded.isSuccess)
        assertEquals(message, decoded.getOrNull())
    }

    @Test
    fun testSteganographyCustomEmojiAndLongText() {
        val customCarrier = "🦁"
        val longMessage = "যত গুলো শব্দ লেখি না কেন একটাই ইমুজি আর নিজের মতো ইমুজি দেওয়া যাবে! গোপন তথ্য: পাসওয়ার্ড ১২৩৪৫৬৭৮"
        val encoded = CryptoEngine.encodeSteganography(customCarrier, longMessage)

        assertTrue(CryptoEngine.hasSteganography(encoded))
        assertEquals(customCarrier, CryptoEngine.extractCarrier(encoded))

        val decoded = CryptoEngine.decodeSteganography(encoded)
        assertTrue(decoded.isSuccess)
        assertEquals(longMessage, decoded.getOrNull())
    }
}
