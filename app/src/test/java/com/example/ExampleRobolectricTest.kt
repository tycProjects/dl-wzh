package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.EduRepository
import com.example.data.MessageType
import com.example.data.NotificationItem
import com.example.data.NotificationLevel
import com.example.data.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("منصة حمو التعليمية", appName)
    }

    @Test
    fun `test video repository progress and completion`() {
        val firstVideo = EduRepository.videos.value.first()
        EduRepository.updateVideoProgress(firstVideo.id, 1200, true)
        val updated = EduRepository.videos.value.find { it.id == firstVideo.id }
        assertNotNull(updated)
        assertTrue(updated!!.isCompleted)
        assertEquals(1200, updated.currentPositionSeconds)
    }

    @Test
    fun `test exam submission and scoring`() {
        val firstExam = EduRepository.exams.value.first()
        EduRepository.submitExamAttempt(firstExam.id, 90, 5, 4)
        val updatedExam = EduRepository.exams.value.find { it.id == firstExam.id }
        assertNotNull(updatedExam)
        assertTrue(updatedExam!!.isSolved)
        assertEquals(90, updatedExam.lastScore)
    }

    @Test
    fun `test messaging and admin reply`() {
        EduRepository.sendStudentMessage(
            type = MessageType.INQUIRY,
            subject = "سؤال تجريبي",
            body = "تفاصيل السؤال"
        )
        val newMsg = EduRepository.messages.value.first()
        assertEquals("سؤال تجريبي", newMsg.subject)

        EduRepository.replyToMessage(newMsg.id, "رد المالك النموذجي")
        val repliedMsg = EduRepository.messages.value.find { it.id == newMsg.id }
        assertNotNull(repliedMsg)
        assertTrue(repliedMsg!!.replies.isNotEmpty())
    }
}
