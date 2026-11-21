package com.android.island.ai

import com.android.island.ai.SearchNavigationPolicy.Move
import com.android.island.ai.SearchNavigationPolicy.Observed
import com.android.island.ai.SearchNavigationPolicy.Progress
import org.junit.Assert.*
import org.junit.Test

class SearchNavigationPolicyTest {
    private fun obs(app: Boolean = true, field: Boolean = false, control: Boolean = false, home: Boolean = false) =
        Observed(inTargetApp = app, hasSearchField = field, hasSearchControl = control, hasHomeTab = home)

    @Test fun usesSearchWhenVisible() {
        assertEquals(Move.USE_SEARCH_FIELD, SearchNavigationPolicy.next(obs(field = true), Progress()))
        assertEquals(Move.TAP_SEARCH_CONTROL, SearchNavigationPolicy.next(obs(control = true), Progress()))
    }

    @Test fun insideAnotherPageGoesBackBeforeSearching() {
        assertEquals(Move.PRESS_BACK, SearchNavigationPolicy.next(obs(), Progress()))
        assertEquals(Move.TAP_HOME_TAB, SearchNavigationPolicy.next(obs(home = true), Progress()))
        assertEquals(Move.PRESS_BACK, SearchNavigationPolicy.next(obs(home = true), Progress(homeTabTapped = true)))
    }

    @Test fun relaunchesOnlyAfterBackIsExhausted() {
        val exhausted = Progress(backs = SearchNavigationPolicy.MAX_BACKS)
        assertEquals(Move.RELAUNCH_APP, SearchNavigationPolicy.next(obs(), exhausted))
        assertEquals(Move.GIVE_UP, SearchNavigationPolicy.next(obs(), exhausted.copy(relaunched = true)))
    }

    @Test fun leavingTheAppRelaunchesOnce() {
        assertEquals(Move.RELAUNCH_APP, SearchNavigationPolicy.next(obs(app = false), Progress()))
        assertEquals(Move.GIVE_UP, SearchNavigationPolicy.next(obs(app = false), Progress(relaunched = true)))
    }

    @Test fun neverLoopsForever() {
        var p = Progress()
        var moves = 0
        while (moves < 50) {
            val m = SearchNavigationPolicy.next(obs(), p)
            if (m == Move.GIVE_UP) break
            p = SearchNavigationPolicy.progressAfter(m, p)
            moves++
        }
        assertTrue(moves <= SearchNavigationPolicy.MAX_ATTEMPTS)
    }
}
