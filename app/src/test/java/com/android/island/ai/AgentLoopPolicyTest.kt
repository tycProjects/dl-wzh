package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentLoopPolicyTest {
    @Test fun stopsWhenModelSaysDoneWithoutPendingActions() {
        val d = AgentLoopPolicy.decide(1, 1_000, modelSaysDone = true, hasActions = false, repeatedFailures = 0)
        assertEquals(AgentLoopPolicy.Decision.Stop("selesai"), d)
    }

    @Test fun stillRunsActionsWhenModelAlsoMarksTaskDone() {
        val d = AgentLoopPolicy.decide(1, 1_000, modelSaysDone = true, hasActions = true, repeatedFailures = 0)
        assertEquals(AgentLoopPolicy.Decision.Continue, d)
    }
    @Test fun stopsWhenNoActionsAndNotDone() {
        val d = AgentLoopPolicy.decide(1, 1_000, modelSaysDone = false, hasActions = false, repeatedFailures = 0)
        assertTrue(d is AgentLoopPolicy.Decision.Stop)
    }
    @Test fun observesAfterExactlyOneActionPerStep() {
        assertEquals(1, AgentLoopPolicy.MAX_ACTIONS_PER_STEP)
    }

    @Test fun continuesUntilThirtySixStepLimit() {
        assertEquals(
            AgentLoopPolicy.Decision.Continue,
            AgentLoopPolicy.decide(AgentLoopPolicy.MAX_STEPS - 1, 1_000, false, true, 0)
        )
    }

    @Test fun stopsAtThirtySixStepLimit() {
        assertEquals(
            AgentLoopPolicy.Decision.Stop("batas langkah"),
            AgentLoopPolicy.decide(AgentLoopPolicy.MAX_STEPS, 1_000, false, true, 0)
        )
    }

    @Test fun continuesUntilFiveMinuteTimeLimit() {
        assertEquals(
            AgentLoopPolicy.Decision.Continue,
            AgentLoopPolicy.decide(2, AgentLoopPolicy.MAX_MS - 1, false, true, 0)
        )
    }

    @Test fun stopsAtFiveMinuteTimeLimit() {
        assertEquals(
            AgentLoopPolicy.Decision.Stop("batas waktu"),
            AgentLoopPolicy.decide(2, AgentLoopPolicy.MAX_MS, false, true, 0)
        )
    }
    @Test fun stopsOnlyAfterFourRepeatedFailures() {
        assertEquals(AgentLoopPolicy.Decision.Continue, AgentLoopPolicy.decide(2, 1_000, false, true, 2))
        assertEquals(AgentLoopPolicy.Decision.Continue, AgentLoopPolicy.decide(2, 1_000, false, true, 3))
        assertTrue(AgentLoopPolicy.decide(2, 1_000, false, true, 4) is AgentLoopPolicy.Decision.Stop)
    }
    @Test fun actionResultsMustBeObservedEvenWhenModelSetsDone() {
        assertEquals(false, AgentLoopPolicy.shouldStopAfterActions(modelSaysDone = true, actionsWereRun = true))
        assertEquals(true, AgentLoopPolicy.shouldStopAfterActions(modelSaysDone = true, actionsWereRun = false))
        assertEquals(false, AgentLoopPolicy.shouldStopAfterActions(modelSaysDone = false, actionsWereRun = true))
    }

    @Test fun continuesWhenWorkRemains() {
        assertEquals(AgentLoopPolicy.Decision.Continue, AgentLoopPolicy.decide(2, 1_000, false, true, 0))
    }
    @Test fun plainPlayCommandIsSingleGoal() {
        assertTrue(AgentLoopPolicy.isSingleGoal("putar lagu Sheila on 7"))
    }
    @Test fun commandWithFollowUpIsNotSingleGoal() {
        assertTrue(!AgentLoopPolicy.isSingleGoal("putar lagu lalu keraskan volume"))
    }
}
