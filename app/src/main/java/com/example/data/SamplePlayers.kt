package com.example.data

import com.example.data.world.TransferMarketCatalog
import com.example.model.Formation
import com.example.model.Player

/**
 * Lineup assignment utilities and transfer market facade backed by [TransferMarketCatalog].
 */
object SamplePlayers {

    fun marketPool(): List<Player> = TransferMarketCatalog.createMarketPool()

    /**
     * Picks the highest-overall available starting XI matching [formation] slots and places the rest on the bench.
     * Healthy & unsuspended players are prioritized ahead of unavailable players.
     */
    fun autoPickLineup(squad: List<Player>, formation: Formation): Pair<List<String>, List<String>> {
        val available = squad
            .filter { it.injuredMatches == 0 && it.suspendedMatches == 0 }
            .sortedByDescending { it.overall }
        val unavailable = squad
            .filter { it.injuredMatches > 0 || it.suspendedMatches > 0 }
            .sortedByDescending { it.overall }

        val remaining = (available + unavailable).toMutableList()
        val starters = MutableList<String?>(11) { null }

        formation.slots.forEachIndexed { idx, slot ->
            val bestExactAvailable = remaining.firstOrNull {
                it.position == slot.position && it.injuredMatches == 0 && it.suspendedMatches == 0
            } ?: remaining.firstOrNull { it.position == slot.position }
            if (bestExactAvailable != null) {
                starters[idx] = bestExactAvailable.id
                remaining.remove(bestExactAvailable)
            }
        }
        for (idx in starters.indices) {
            if (starters[idx] == null && remaining.isNotEmpty()) {
                val fallback = remaining.removeAt(0)
                starters[idx] = fallback.id
            }
        }

        val xiIds = starters.filterNotNull()
        val benchIds = remaining.map { it.id }
        return xiIds to benchIds
    }
}
