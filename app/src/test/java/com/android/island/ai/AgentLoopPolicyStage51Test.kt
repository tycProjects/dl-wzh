package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentLoopPolicyStage51Test {
    @Test fun agentHasEnoughRoomForLongTasksWithoutStoppingAfterOneFailure() {
        assertEquals(20, AgentLoopPolicy.MAX_STEPS)
        assertEquals(180_000L, AgentLoopPolicy.MAX_MS)
        assertEquals(4, AgentLoopPolicy.MAX_REPEAT_FAILURES)
    }

    @Test fun actionMustBeObservedBeforeDoneCanFinish() {
        assertTrue(!AgentLoopPolicy.shouldStopAfterActions(modelSaysDone = true, actionsWereRun = true))
        assertTrue(AgentLoopPolicy.shouldStopAfterActions(modelSaysDone = true, actionsWereRun = false))
    }
}
