package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.MathEvaluator
import org.junit.Assert.assertEquals
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
        assertEquals("Sharp Calculator", appName)
    }

    @Test
    fun `math evaluator basic operations`() {
        val evaluator = MathEvaluator()
        val res = evaluator.evaluate("12 + 8 * 2")
        assertTrue(res is MathEvaluator.EvalResult.Success)
        val success = res as MathEvaluator.EvalResult.Success
        assertEquals(28.0, success.value, 0.0001)
    }

    @Test
    fun `math evaluator trigonometric and power functions`() {
        val evaluator = MathEvaluator(isRadianMode = false)
        val res = evaluator.evaluate("sin(30)")
        assertTrue(res is MathEvaluator.EvalResult.Success)
        val success = res as MathEvaluator.EvalResult.Success
        assertEquals(0.5, success.value, 0.0001)

        val powRes = evaluator.evaluate("2^3 + sqrt(16)")
        assertTrue(powRes is MathEvaluator.EvalResult.Success)
        assertEquals(12.0, (powRes as MathEvaluator.EvalResult.Success).value, 0.0001)
    }
}
