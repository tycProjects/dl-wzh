package com.example.crypto

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    private fun base64Encode(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    private fun base64Decode(str: String): ByteArray {
        return try {
            Base64.decode(str, Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getDecoder().decode(str.trim())
        }
    }
    const val ZW_ZERO = '\u200B'
    const val ZW_ONE = '\u200C'
    const val ZW_MARKER_START = '\u2060'
    const val ZW_MARKER_END = '\uFEFF'

    // ==========================================
    // ZERO-WIDTH STEGANOGRAPHY (Single Emoji Carrier)
    // ==========================================

    fun encodeSteganography(carrier: String, secretMessage: String): String {
        if (secretMessage.isEmpty()) return carrier
        val safeCarrier = if (carrier.isEmpty()) "🤔" else carrier
        val bytes = secretMessage.toByteArray(StandardCharsets.UTF_8)
        val sb = StringBuilder()
        sb.append(safeCarrier)
        sb.append(ZW_MARKER_START)
        for (b in bytes) {
            val unsigned = b.toInt() and 0xFF
            for (bitIdx in 7 downTo 0) {
                val bit = (unsigned shr bitIdx) and 1
                sb.append(if (bit == 0) ZW_ZERO else ZW_ONE)
            }
        }
        sb.append(ZW_MARKER_END)
        return sb.toString()
    }

    fun hasSteganography(rawText: String): Boolean {
        return rawText.any { it == ZW_ZERO || it == ZW_ONE || it == ZW_MARKER_START || it == ZW_MARKER_END }
    }

    fun extractCarrier(rawText: String): String {
        val filtered = rawText.filter { it != ZW_ZERO && it != ZW_ONE && it != ZW_MARKER_START && it != ZW_MARKER_END }
        return if (filtered.isEmpty()) "🤔" else filtered.trim()
    }

    fun decodeSteganography(rawText: String): Result<String> {
        try {
            if (!hasSteganography(rawText)) {
                return Result.failure(IllegalArgumentException("কোনো গোপন বার্তা পাওয়া যায়নি"))
            }

            var bitString = ""
            if (rawText.contains(ZW_MARKER_START) && rawText.contains(ZW_MARKER_END)) {
                val payload = rawText.substringAfter(ZW_MARKER_START).substringBefore(ZW_MARKER_END)
                bitString = payload.filter { it == ZW_ZERO || it == ZW_ONE }
            }
            if (bitString.isEmpty()) {
                bitString = rawText.filter { it == ZW_ZERO || it == ZW_ONE }
            }

            if (bitString.isEmpty() || bitString.length < 8) {
                return Result.failure(IllegalArgumentException("কোনো গোপন বার্তা পাওয়া যায়নি"))
            }

            val byteCount = bitString.length / 8
            val bytes = ByteArray(byteCount)
            for (i in 0 until byteCount) {
                var b = 0
                for (j in 0 until 8) {
                    val bitChar = bitString[i * 8 + j]
                    b = (b shl 1) or (if (bitChar == ZW_ONE) 1 else 0)
                }
                bytes[i] = b.toByte()
            }

            return Result.success(String(bytes, StandardCharsets.UTF_8))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    const val PREFIX_EMOJI = "🔐[SC-EMOJI] "
    const val SUFFIX_EMOJI = " [SC-END]"

    const val PREFIX_CHAR = "⚡[SC-CHAR] "
    const val SUFFIX_CHAR = " [SC-END]"

    const val PREFIX_AES = "🛡️[SC-AES256] "
    const val SUFFIX_AES = " [SC-END]"

    // 16 distinct, cross-platform emojis corresponding to hex characters 0 to F
    val EMOJI_HEX_LIST = listOf(
        "🔮", "✨", "🌙", "⭐",
        "⚡", "🔥", "💧", "🌿",
        "💎", "🗝️", "🪐", "🌌",
        "🌀", "🛸", "👾", "🛡️"
    )

    private val EMOJI_TO_HEX_MAP: Map<String, Char> by lazy {
        val map = mutableMapOf<String, Char>()
        val hexChars = "0123456789abcdef"
        for (i in EMOJI_HEX_LIST.indices) {
            val emoji = EMOJI_HEX_LIST[i]
            val hexChar = hexChars[i]
            map[emoji] = hexChar
            // Also handle key variation selector (\uFE0F) if stripped by some apps
            val stripped = emoji.replace("\uFE0F", "")
            if (stripped != emoji) {
                map[stripped] = hexChar
            }
        }
        map
    }

    private val secureRandom = SecureRandom()

    // ==========================================
    // 1. EMOJI ENCODING / DECODING
    // ==========================================

    fun encodeEmoji(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val bytes = plainText.toByteArray(StandardCharsets.UTF_8)
        val sb = StringBuilder()
        for (b in bytes) {
            val unsigned = b.toInt() and 0xFF
            val highNibble = (unsigned shr 4) and 0x0F
            val lowNibble = unsigned and 0x0F
            sb.append(EMOJI_HEX_LIST[highNibble])
            sb.append(EMOJI_HEX_LIST[lowNibble])
        }
        return "$PREFIX_EMOJI$sb$SUFFIX_EMOJI"
    }

    fun decodeEmoji(rawText: String): Result<String> {
        try {
            var content = rawText.trim()
            if (content.contains(PREFIX_EMOJI)) {
                content = content.substringAfter(PREFIX_EMOJI)
            }
            if (content.contains(SUFFIX_EMOJI)) {
                content = content.substringBefore(SUFFIX_EMOJI)
            }
            content = content.replace(" ", "").replace("\n", "").replace("\r", "")

            // Extract all emojis matching our map
            val hexBuilder = StringBuilder()
            var index = 0
            while (index < content.length) {
                var matched = false
                // Check 2 code units first (surrogate pair or variation selector)
                for (length in 3 downTo 1) {
                    if (index + length <= content.length) {
                        val sub = content.substring(index, index + length)
                        val hexChar = EMOJI_TO_HEX_MAP[sub]
                        if (hexChar != null) {
                            hexBuilder.append(hexChar)
                            index += length
                            matched = true
                            break
                        }
                    }
                }
                if (!matched) {
                    index++ // Skip unrecognized character or spacing
                }
            }

            val hexString = hexBuilder.toString()
            if (hexString.isEmpty() || hexString.length % 2 != 0) {
                return Result.failure(IllegalArgumentException("Invalid or incomplete emoji secret message."))
            }

            val byteCount = hexString.length / 2
            val bytes = ByteArray(byteCount)
            for (i in 0 until byteCount) {
                val hexByte = hexString.substring(i * 2, i * 2 + 2)
                bytes[i] = hexByte.toInt(16).toByte()
            }

            return Result.success(String(bytes, StandardCharsets.UTF_8))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    // ==========================================
    // 2. CHARACTER CIPHER ENCODING / DECODING
    // ==========================================

    fun encodeCharacter(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val bytes = plainText.toByteArray(StandardCharsets.UTF_8)
        val base64 = base64Encode(bytes)
        // Light alphanumeric inversion for cyber cipher look
        val cipherBuilder = StringBuilder()
        for (ch in base64) {
            when {
                ch in 'a'..'m' -> cipherBuilder.append(ch + 13)
                ch in 'n'..'z' -> cipherBuilder.append(ch - 13)
                ch in 'A'..'M' -> cipherBuilder.append(ch + 13)
                ch in 'N'..'Z' -> cipherBuilder.append(ch - 13)
                else -> cipherBuilder.append(ch)
            }
        }
        return "$PREFIX_CHAR$cipherBuilder$SUFFIX_CHAR"
    }

    fun decodeCharacter(rawText: String): Result<String> {
        try {
            var content = rawText.trim()
            if (content.contains(PREFIX_CHAR)) {
                content = content.substringAfter(PREFIX_CHAR)
            }
            if (content.contains(SUFFIX_CHAR)) {
                content = content.substringBefore(SUFFIX_CHAR)
            }
            content = content.trim()

            // Invert back
            val b64Builder = StringBuilder()
            for (ch in content) {
                when {
                    ch in 'a'..'m' -> b64Builder.append(ch + 13)
                    ch in 'n'..'z' -> b64Builder.append(ch - 13)
                    ch in 'A'..'M' -> b64Builder.append(ch + 13)
                    ch in 'N'..'Z' -> b64Builder.append(ch - 13)
                    else -> b64Builder.append(ch)
                }
            }
            val bytes = base64Decode(b64Builder.toString())
            return Result.success(String(bytes, StandardCharsets.UTF_8))
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Invalid character cipher message."))
        }
    }

    // ==========================================
    // 3. MILITARY-GRADE AES-256-GCM ENCRYPTION
    // ==========================================

    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    fun encryptAes256Gcm(plainText: String, password: String): String {
        require(plainText.isNotEmpty()) { "Plaintext cannot be empty" }
        require(password.isNotEmpty()) { "Password is required for AES-256 encryption" }

        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)

        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        // Derive AES-256 key using PBKDF2
        val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        // Encrypt with AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        val saltB64 = base64Encode(salt)
        val ivB64 = base64Encode(iv)
        val cipherB64 = base64Encode(cipherBytes)

        val payload = "$saltB64:$ivB64:$cipherB64"
        return "$PREFIX_AES$payload$SUFFIX_AES"
    }

    fun decryptAes256Gcm(rawText: String, password: String): Result<String> {
        try {
            if (password.isEmpty()) {
                return Result.failure(IllegalArgumentException("Please enter the decryption password."))
            }

            var content = rawText.trim()
            if (content.contains(PREFIX_AES)) {
                content = content.substringAfter(PREFIX_AES)
            }
            if (content.contains(SUFFIX_AES)) {
                content = content.substringBefore(SUFFIX_AES)
            }
            content = content.trim()

            val parts = content.split(":")
            if (parts.size != 3) {
                return Result.failure(IllegalArgumentException("Malformed AES-256 encrypted payload."))
            }

            val salt = base64Decode(parts[0])
            val iv = base64Decode(parts[1])
            val cipherBytes = base64Decode(parts[2])

            if (salt.size != SALT_LENGTH_BYTES || iv.size != IV_LENGTH_BYTES) {
                return Result.failure(IllegalArgumentException("Corrupted salt or initialization vector."))
            }

            val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
            val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedBytes = cipher.doFinal(cipherBytes)
            return Result.success(String(decryptedBytes, StandardCharsets.UTF_8))
        } catch (e: javax.crypto.AEADBadTagException) {
            return Result.failure(IllegalArgumentException("Incorrect password or tampered message! Decryption failed."))
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Decryption error: ${e.localizedMessage ?: "Invalid payload"}"))
        }
    }

    // ==========================================
    // 4. AUTOMATIC FORMAT DETECTION
    // ==========================================

    fun detectFormat(text: String): SecretFormat? {
        val trimmed = text.trim()
        if (hasSteganography(trimmed)) {
            return SecretFormat.STEGANOGRAPHY
        }
        if (trimmed.contains(PREFIX_AES) || (trimmed.contains(":") && trimmed.split(":").size == 3 && !trimmed.contains(" "))) {
            return SecretFormat.AES256_GCM
        }
        if (trimmed.contains(PREFIX_CHAR)) {
            return SecretFormat.CHARACTER
        }
        if (trimmed.contains(PREFIX_EMOJI)) {
            return SecretFormat.EMOJI
        }

        // Heuristic detection: does it consist primarily of our secret emojis?
        var emojiMatches = 0
        for (emoji in EMOJI_HEX_LIST) {
            if (trimmed.contains(emoji)) {
                emojiMatches++
            }
        }
        if (emojiMatches >= 3 || (trimmed.isNotEmpty() && EMOJI_HEX_LIST.any { trimmed.startsWith(it) })) {
            return SecretFormat.EMOJI
        }

        return null
    }
}
