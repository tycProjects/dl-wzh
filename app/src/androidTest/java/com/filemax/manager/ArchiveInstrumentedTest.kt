package com.filemax.manager

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

/** Device tests: they exercise the native 7-Zip-JBinding engine, Keystore and split ZIP, which cannot run on the JVM. */
@RunWith(AndroidJUnit4::class)
class ArchiveInstrumentedTest {
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun workDir() = File(ctx.cacheDir, "it_${System.nanoTime()}").apply { mkdirs() }
    private fun sample(root: File): List<File> {
        val d = File(root, "dir").apply { mkdirs() }
        File(d, "a.txt").writeText("hello"); File(d, "sub").mkdirs(); File(d, "sub/b.bin").writeBytes(ByteArray(5000) { it.toByte() })
        return listOf(d)
    }

    @Test fun encryptedSevenZipRoundTrip() {
        val root = workDir()
        try {
            val out = File(root, "enc.7z")
            ArchiveWriter.create(sample(root), out, CreateOptions(format = "7z", password = "secret".toCharArray()))
            // names are encrypted: listing without the password must fail
            val noPw = runCatching { ArchiveReader.list(out) }.exceptionOrNull()
            assertTrue(noPw is PasswordException || noPw is IOException)
            val wrong = runCatching { ArchiveReader.list(out, "nope".toCharArray()) }.exceptionOrNull()
            assertTrue(wrong is PasswordException || wrong is IOException)
            assertTrue(ArchiveReader.list(out, "secret".toCharArray()).any { it.path.endsWith("sub/b.bin") })
            val dest = File(root, "x")
            ArchiveReader.extract(out, dest, "secret".toCharArray())
            assertEquals("hello", File(dest, "dir/a.txt").readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun nativeSplitZip() {
        val root = workDir()
        try {
            val out = File(root, "big.zip")
            val big = File(root, "big.bin").apply { writeBytes(ByteArray(300_000) { (it * 31).toByte() }) }
            ArchiveWriter.create(listOf(big), out, CreateOptions(format = "zip", level = 0, splitBytes = 100_000))
            assertTrue(File(root, "big.z01").exists())
            val dest = File(root, "x")
            ArchiveReader.extract(out, dest)
            assertArrayEquals(big.readBytes(), File(dest, "big.bin").readBytes())
        } finally { root.deleteRecursively() }
    }

    @Test fun secureStoreRoundTrip() {
        SecureStore.save(ctx, "t.7z", "p@ss".toCharArray())
        assertEquals("p@ss", String(SecureStore.load(ctx, "t.7z")!!))
        SecureStore.forget(ctx, "t.7z")
        assertNull(SecureStore.load(ctx, "t.7z"))
    }

    @Test fun cabAndLzhAreRecognised() {
        assertEquals(ArchiveKind.CAB, ArchiveReader.kindByName("x.cab"))
        assertEquals(ArchiveKind.LZH, ArchiveReader.kindByName("x.lzh"))
    }
}
