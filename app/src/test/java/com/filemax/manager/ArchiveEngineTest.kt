package com.filemax.manager

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ArchiveEngineTest {
    @Test fun safeTargetRejectsTraversal() {
        val root = Files.createTempDirectory("fm-root").toFile()
        try {
            assertThrows(SecurityException::class.java) { ArchiveEngine.safeTarget(root, "../outside.txt") }
            assertEquals(File(root, "inside.txt").canonicalFile, ArchiveEngine.safeTarget(root, "inside.txt"))
        } finally { root.deleteRecursively() }
    }

    @Test fun zipRoundTripPreservesBytes() {
        val root = Files.createTempDirectory("fm-zip").toFile()
        try {
            val source = File(root, "source.txt").apply { writeBytes(byteArrayOf(0, 1, 2, 3, 127, -1)) }
            val zip = File(root, "sample.zip")
            ArchiveEngine.createZip(listOf(source), zip)
            val output = File(root, "out")
            ArchiveEngine.extractZip(zip, output)
            assertArrayEquals(source.readBytes(), File(output, "source.txt").readBytes())
        } finally { root.deleteRecursively() }
    }
}
