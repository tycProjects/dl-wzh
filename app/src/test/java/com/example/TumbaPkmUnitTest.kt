package com.example

import com.example.data.model.AppSettings
import com.example.data.model.BackupData
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteLink
import com.example.data.model.NoteTemplate
import com.example.data.model.Tag
import com.example.utils.TextFormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
class TumbaPkmUnitTest {

    @Test
    fun testInternalLinkExtraction() {
        val content = """
            Here is a link to [[Second Note]] and another to [[Project Ideas]].
            Also repeated link to [[Second Note]].
            And ignore broken [[ or unclosed [[links.
        """.trimIndent()

        val links = TextFormatUtils.extractInternalLinks(content)
        assertEquals(2, links.size)
        assertTrue(links.contains("Second Note"))
        assertTrue(links.contains("Project Ideas"))
    }

    @Test
    fun testChecklistToggling() {
        val original = """
            [ ] First task
            [x] Second task
            Normal paragraph
        """.trimIndent()

        val toggled1 = TextFormatUtils.toggleChecklistLine(original, 0)
        assertTrue(toggled1.contains("[x] First task"))

        val toggled2 = TextFormatUtils.toggleChecklistLine(toggled1, 1)
        assertTrue(toggled2.contains("[ ] Second task"))
    }

    @Test
    fun testSimpleDiff() {
        val oldText = "Line 1\nLine 2\nLine 3"
        val newText = "Line 1\nLine 2 edited\nLine 3\nLine 4"

        val diff = TextFormatUtils.computeSimpleDiff(oldText, newText)
        assertTrue(diff.isNotEmpty())
        assertTrue(diff.any { it.type == TextFormatUtils.DiffType.SAME && it.text == "Line 1" })
        assertTrue(diff.any { it.type == TextFormatUtils.DiffType.ADDED })
    }

    @Test
    fun testBackupRoundTripV2() {
        val note = Note(
            id = "n-1",
            title = "Test Note",
            content = "Content with [[Target Note]]",
            createdAt = 1000L,
            updatedAt = 2000L,
            color = "#262A33"
        )
        val folder = Folder(id = "f-1", name = "Test Folder")
        val tag = Tag(id = "t-1", name = "productivity")
        val link = NoteLink(id = "l-1", sourceNoteId = "n-1", targetNoteId = "n-2")
        val tpl = NoteTemplate(id = "tpl-1", title = "Sprint", content = "Tasks: [ ]")

        val backup = BackupData(
            version = "2.0.0",
            notes = listOf(note),
            folders = listOf(folder),
            tags = listOf(tag),
            noteTags = listOf(Pair("n-1", "t-1")),
            settings = AppSettings(
                theme = "dim",
                language = "uk",
                profileNickname = "Alex",
                profileAvatarPath = "/data/user_avatar.jpg",
                isGridView = false
            ),
            templates = listOf(tpl),
            links = listOf(link)
        )

        val json = TextFormatUtils.backupToJson(backup)
        val parsed = TextFormatUtils.parseBackupJson(json)

        assertEquals("2.0.0", parsed.version)
        assertEquals(1, parsed.notes.size)
        assertEquals("Test Note", parsed.notes[0].title)
        assertEquals("#262A33", parsed.notes[0].color)
        assertEquals(1, parsed.folders.size)
        assertEquals(1, parsed.tags.size)
        assertEquals(1, parsed.templates.size)
        assertEquals(1, parsed.links.size)
        assertEquals("dim", parsed.settings.theme)
        assertEquals("uk", parsed.settings.language)
        assertEquals("Alex", parsed.settings.profileNickname)
        assertEquals("/data/user_avatar.jpg", parsed.settings.profileAvatarPath)
        assertFalse(parsed.settings.isGridView)
    }

    @Test
    fun testBackwardsCompatibilityV1() {
        val v1Json = """
            {
              "version": "1.0.0",
              "timestamp": 1600000000000,
              "notes": [
                {
                  "id": "old-1",
                  "title": "Legacy Note",
                  "content": "Old content",
                  "folderId": null,
                  "isFavorite": true,
                  "isPinned": false,
                  "isArchived": false,
                  "inTrash": false,
                  "createdAt": 1000,
                  "updatedAt": 2000
                }
              ],
              "folders": [],
              "tags": [],
              "noteTags": [],
              "settings": {
                "theme": "dark",
                "language": "en"
              }
            }
        """.trimIndent()

        val parsed = TextFormatUtils.parseBackupJson(v1Json)
        assertEquals("1.0.0", parsed.version)
        assertEquals(1, parsed.notes.size)
        assertEquals("Legacy Note", parsed.notes[0].title)
        assertEquals(0, parsed.templates.size)
        assertEquals(0, parsed.links.size)
        assertEquals("en", parsed.settings.language)
    }
}
