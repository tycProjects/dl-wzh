package com.glassdex.analyzer.analysis

import java.io.InputStream
import java.security.MessageDigest

object Hashing {
    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }

    fun all(input: InputStream): Hashes {
        val md5 = MessageDigest.getInstance("MD5")
        val sha1 = MessageDigest.getInstance("SHA-1")
        val sha256 = MessageDigest.getInstance("SHA-256")
        val buf = ByteArray(64 * 1024)
        input.use {
            while (true) {
                val n = it.read(buf)
                if (n < 0) break
                md5.update(buf, 0, n); sha1.update(buf, 0, n); sha256.update(buf, 0, n)
            }
        }
        return Hashes(hex(md5.digest()), hex(sha1.digest()), hex(sha256.digest()))
    }

    fun sha256(bytes: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(bytes))
}
