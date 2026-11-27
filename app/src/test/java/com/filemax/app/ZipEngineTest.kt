package com.filemax.app

import com.filemax.app.archive.ZipEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipEngineTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun createsAndExtractsZip() {
        val source = temp.newFile("note.txt").apply { writeText("FILE MAX test") }
        val archive = File(temp.root, "bundle.zip")
        ZipEngine.create(listOf(source), archive)
        val destination = File(temp.root, "out")
        assertEquals(1, ZipEngine.extract(archive, destination))
        assertEquals("FILE MAX test", File(destination, "note.txt").readText())
    }

    @Test fun rejectsPathTraversal() {
        val archive = File(temp.root, "malicious.zip")
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("../outside.txt")); zip.write("bad".toByteArray()); zip.closeEntry()
        }
        val destination = File(temp.root, "safe")
        val result = runCatching { ZipEngine.extract(archive, destination) }
        assertTrue(result.isFailure)
        assertTrue(!File(temp.root, "outside.txt").exists())
    }
}
