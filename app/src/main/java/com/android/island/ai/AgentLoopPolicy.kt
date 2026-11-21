package com.android.island.ai

/**
 * Decides whether the observe-act loop continues. Pure logic, no Android types, so it is unit tested.
 * Order matters: a finished task or an empty answer always wins over the limits.
 */
object AgentLoopPolicy {
    // STAGE 72: persistence budget raised. Failed scenarios were dominated by early aborts
    // ("batas langkah" / "macet") while real app navigation needs observation time: slow app
    // first paints, keyboard animations, search-result loads. 24 steps / 4 minutes give the
    // recovery ladder room to try its alternate routes without ever becoming infinite.
    const val MAX_STEPS = 24
    const val MAX_MS = 240_000L
    const val MAX_REPEAT_FAILURES = 4
    /** Actions run per observation step. Small on purpose: the screen is read again after each step. */
    const val MAX_ACTIONS_PER_STEP = 1

    /** A done flag cannot finish a turn that still contains actions whose results are unknown. */
    fun shouldStopAfterActions(modelSaysDone: Boolean, actionsWereRun: Boolean): Boolean =
        modelSaysDone && !actionsWereRun

    /** How many times a premature stop may be pushed back with "try another route" before it is accepted. */
    const val MAX_CHALLENGES = 4

    private val GIVE_UP_PHRASES = listOf(
        "tidak bisa", "tidak dapat", "belum bisa", "tidak mampu", "tidak memiliki akses",
        "silakan tekan", "silakan ketuk", "silakan klik", "tolong tekan", "tekan sendiri",
        "anda perlu menekan", "anda bisa menekan", "kamu bisa menekan", "secara manual",
        "please press", "please tap", "you can press", "you need to press", "manually",
        "i can't", "i cannot", "unable to"
    )

    fun looksLikeGivingUp(reply: String): Boolean {
        val text = reply.lowercase()
        return GIVE_UP_PHRASES.any { it in text }
    }

    /**
     * True when the model is about to end the task without actions although the goal is not proven
     * done: it said nothing at all, its last step failed, or its reply hands the work back to the user.
     * The caller then re-reads the screen and tells the model to try a different route instead.
     * Order in [decide] is untouched; this check runs before it.
     */
    fun shouldChallengeStop(
        stepsDone: Int,
        challengesUsed: Int,
        modelSaysDone: Boolean,
        hasActions: Boolean,
        lastStepFailed: Boolean,
        reply: String
    ): Boolean {
        if (hasActions || challengesUsed >= MAX_CHALLENGES) return false
        val givingUp = looksLikeGivingUp(reply)
        if (stepsDone == 0 && !givingUp) return false
        // A model that simply stops with a normal sentence has reached the goal in its own view: accept
        // it. Pushing it on ("coba rute lain") made the agent keep acting after the destination was
        // reached. Only an empty reply (nothing was said), a failed last step or a hand-back to the
        // user is still challenged.
        return (!modelSaysDone && reply.isBlank()) || lastStepFailed || givingUp
    }

    sealed class Decision {
        object Continue : Decision()
        data class Stop(val reason: String) : Decision()
    }

    fun decide(
        stepsDone: Int,
        elapsedMs: Long,
        modelSaysDone: Boolean,
        hasActions: Boolean,
        repeatedFailures: Int,
        maxSteps: Int = MAX_STEPS,
        maxMs: Long = MAX_MS
    ): Decision = when {
        modelSaysDone && !hasActions -> Decision.Stop("selesai")
        !hasActions -> Decision.Stop("tanpa aksi")
        stepsDone >= maxSteps -> Decision.Stop("batas langkah")
        elapsedMs >= maxMs -> Decision.Stop("batas waktu")
        repeatedFailures >= MAX_REPEAT_FAILURES -> Decision.Stop("macet")
        else -> Decision.Continue
    }

    private val CONTINUATION = Regex("(?i)\\b(lalu|kemudian|setelah|terus|sambil|habis itu|then|after)\\b")

    /** True when the command has no follow-up step ("lalu", "kemudian", ...), so one successful action completes it. */
    fun isSingleGoal(userText: String): Boolean = !CONTINUATION.containsMatchIn(userText)

    /** Loop budget for one user turn: deep research legally needs more steps and more time. */
    fun budgetFor(isDeepResearch: Boolean): Pair<Int, Long> =
        if (isDeepResearch) ResearchPlanner.RESEARCH_MAX_STEPS to ResearchPlanner.RESEARCH_MAX_MS
        else MAX_STEPS to MAX_MS
}
