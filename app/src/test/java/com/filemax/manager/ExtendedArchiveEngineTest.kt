package com.filemax.manager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ExtendedArchiveEngineTest {
    @Test fun sevenZipRoundTrip() {
        val dir = Files.createTempDirectory("filemax7z").toFile()
        try {
            val source = File(dir, "source.txt").apply { writeText("FILE MAX 7Z test") }
            val archive = File(dir, "test.7z")
            val output = File(dir, "out")
            ExtendedArchiveEngine.create7z(listOf(source), archive)
            ExtendedArchiveEngine.extract7z(archive, output)
            assertEquals(source.readText(), File(output, "source.txt").readText())
        } finally { dir.deleteRecursively() }
    }

    @Test fun tarGzipRoundTrip() {
        val dir = Files.createTempDirectory("filemaxtar").toFile()
        try {
            val source = File(dir, "source.txt").apply { writeText("FILE MAX TAR test") }
            val archive = File(dir, "test.tar.gz")
            val output = File(dir, "out")
            ExtendedArchiveEngine.createTar(listOf(source), archive, true)
            ExtendedArchiveEngine.extractTar(archive, output, true)
            assertEquals(source.readText(), File(output, "source.txt").readText())
        } finally { dir.deleteRecursively() }
    }

    @Test(expected = SecurityException::class)
    fun rejectsTraversalInExtendedArchive() {
        val root = Files.createTempDirectory("filemaxroot").toFile()
        try { ArchiveEngine.safeTarget(root, "../escape.txt") } finally { root.deleteRecursively() }
    }
}
