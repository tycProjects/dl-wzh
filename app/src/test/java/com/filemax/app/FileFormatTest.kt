package com.filemax.app

import com.filemax.app.util.FileFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class FileFormatTest {
    @Test fun formatsBytes() { assertEquals("512 B", FileFormat.size(512)) }
    @Test fun formatsKilobytes() { assertEquals("1.0 KB", FileFormat.size(1024)) }
}
