package com.filemax.manager

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ArchiveFormatsTest {
    private fun sample(root: File): List<File> {
        val d = File(root, "dir").apply { mkdirs() }
        File(d, "a.txt").writeText("hello"); File(d, "sub").mkdirs(); File(d, "sub/b.bin").writeBytes(ByteArray(5000) { it.toByte() })
        return listOf(d)
    }

    @Test fun roundTripAllCreatableFormats() {
        val root = Files.createTempDirectory("fm-fmt").toFile()
        try {
            val inputs = sample(root)
            for (fmt in listOf("zip", "7z", "tar", "tar.gz", "tar.bz2", "tar.xz")) {
                val out = File(root, "t.$fmt")
                ArchiveWriter.create(inputs, out, CreateOptions(format = fmt))
                assertTrue(ArchiveReader.list(out).any { it.path.endsWith("sub/b.bin") })
                assertEquals(ArchiveReader.test(out), ArchiveReader.list(out).size)
                val dest = File(root, "x_$fmt")
                ArchiveReader.extract(out, dest)
                assertEquals("hello", File(dest, "dir/a.txt").readText())
            }
        } finally { root.deleteRecursively() }
    }

    @Test fun passwordZipRequiresAndRejectsPassword() {
        val root = Files.createTempDirectory("fm-pw").toFile()
        try {
            val out = File(root, "p.zip")
            ArchiveWriter.create(sample(root), out, CreateOptions(password = "secret".toCharArray()))
            assertThrows(PasswordException::class.java) { ArchiveReader.list(out) }
            assertEquals(3, ArchiveReader.list(out, "secret".toCharArray()).count { !it.isDir } + 1)
        } finally { root.deleteRecursively() }
    }

    @Test fun selectedExtractionAndSplitJoin() {
        val root = Files.createTempDirectory("fm-split").toFile()
        try {
            val out = File(root, "s.zip")
            ArchiveWriter.create(sample(root), out, CreateOptions(level = 0, splitBytes = 65536))
            val first = File(root, "s.zip.001"); assertTrue(first.exists() || !out.exists())
            val joined = ArchiveReader.joinVolumes(if (first.exists()) first else out, File(root, "cache"))
            val dest = File(root, "sel")
            ArchiveReader.extract(joined, dest, selection = setOf("dir/a.txt"))
            assertTrue(File(dest, "dir/a.txt").exists()); assertFalse(File(dest, "dir/sub/b.bin").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun detectsByMagicNotExtension() {
        val root = Files.createTempDirectory("fm-magic").toFile()
        try {
            val out = File(root, "real.zip"); ArchiveWriter.create(sample(root), out, CreateOptions())
            val fake = File(root, "fake.7z"); out.copyTo(fake)
            assertEquals(ArchiveKind.ZIP, ArchiveReader.detect(fake)); assertTrue(ArchiveReader.extensionMismatch(fake))
        } finally { root.deleteRecursively() }
    }

    @Test fun detectsCabAndLzhByMagic() {
        val root = Files.createTempDirectory("fm-magic").toFile()
        try {
            val cab = File(root, "x.bin").apply { writeBytes(byteArrayOf(0x4D, 0x53, 0x43, 0x46) + ByteArray(40)) }
            assertEquals(ArchiveKind.CAB, ArchiveReader.detect(cab))
            val lzh = File(root, "y.bin").apply { writeBytes(byteArrayOf(0x20, 0x00) + "-lh5-".toByteArray() + ByteArray(40)) }
            assertEquals(ArchiveKind.LZH, ArchiveReader.detect(lzh))
            assertEquals(ArchiveKind.LZH, ArchiveReader.kindByName("a.LHA"))
            assertEquals(ArchiveKind.CAB, ArchiveReader.kindByName("setup.cab"))
        } finally { root.deleteRecursively() }
    }
}
