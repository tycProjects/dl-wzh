package com.android.island.ai

/**
 * Decides the next navigation move when the agent must reach an app's search UI.
 * The app may be on any page (inside a chat, a video, a profile). The rule is: use search if it is
 * visible; otherwise go back toward the app's main page, and only relaunch as the last resort.
 * Pure logic, unit tested; the Android side only executes the returned [Move].
 */
object SearchNavigationPolicy {
    enum class Move { USE_SEARCH_FIELD, TAP_SEARCH_CONTROL, TAP_HOME_TAB, PRESS_BACK, RELAUNCH_APP, GIVE_UP }

    const val MAX_BACKS = 4
    const val MAX_ATTEMPTS = 10

    data class Observed(
        val inTargetApp: Boolean,
        val hasSearchField: Boolean,
        val hasSearchControl: Boolean,
        val hasHomeTab: Boolean,
        val keyboardOpen: Boolean = false
    )

    data class Progress(
        val attempts: Int = 0,
        val backs: Int = 0,
        val homeTabTapped: Boolean = false,
        val relaunched: Boolean = false
    )

    fun next(seen: Observed, done: Progress): Move = when {
        done.attempts >= MAX_ATTEMPTS -> Move.GIVE_UP
        !seen.inTargetApp -> if (!done.relaunched) Move.RELAUNCH_APP else Move.GIVE_UP
        seen.hasSearchField -> Move.USE_SEARCH_FIELD
        seen.hasSearchControl -> Move.TAP_SEARCH_CONTROL
        seen.hasHomeTab && !done.homeTabTapped -> Move.TAP_HOME_TAB
        // An open keyboard hides the toolbar of many apps; closing it is also what Back does first.
        done.backs < MAX_BACKS -> Move.PRESS_BACK
        !done.relaunched -> Move.RELAUNCH_APP
        else -> Move.GIVE_UP
    }

    fun progressAfter(move: Move, p: Progress): Progress {
        val n = p.copy(attempts = p.attempts + 1)
        return when (move) {
            Move.PRESS_BACK -> n.copy(backs = n.backs + 1)
            Move.TAP_HOME_TAB -> n.copy(homeTabTapped = true)
            Move.RELAUNCH_APP -> n.copy(relaunched = true, backs = 0, homeTabTapped = false)
            else -> n
        }
    }
}
