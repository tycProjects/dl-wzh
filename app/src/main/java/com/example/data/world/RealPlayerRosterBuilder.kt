package com.example.data.world

import com.example.data.SamplePlayers
import com.example.model.BadgeStyle
import com.example.model.Club
import com.example.model.ClubCrestSpec
import com.example.model.CountryCode
import com.example.model.Formation
import com.example.model.Player
import com.example.model.Position
import com.example.model.SquadRole
import kotlin.math.pow
import kotlin.math.roundToInt

data class RealPlayerSeed(
    val name: String,
    val pos: Position,
    val overall: Int,
    val age: Int,
    val nat: String,
)

data class RealClubSeed(
    val id: String,
    val name: String,
    val shortName: String,
    val country: CountryCode,
    val divisionId: String,
    val tier: Int,
    val city: String,
    val stadium: String,
    val primaryHex: String,
    val secondaryHex: String,
    val crestSpec: ClubCrestSpec,
    val budgetM: Double,
    val clubOvr: Int,
    val formation: Formation = Formation.F433,
    val players: List<RealPlayerSeed>,
    val nameAr: String? = null,
)

object RealPlayerRosterBuilder {

    fun formatBudget(millions: Double): String {
        return if (millions < 1.0) {
            val thousands = (millions * 1000.0).roundToInt().coerceAtLeast(1)
            "€${thousands}K"
        } else {
            val rounded = (millions * 10.0).roundToInt() / 10.0
            "€${rounded}M"
        }
    }

    fun buildPlayer(
        id: String,
        seed: RealPlayerSeed,
        role: SquadRole = SquadRole.ROTATION,
        sellingClubId: String? = null,
        sellingClubName: String? = null,
    ): Player {
        val ovr = seed.overall.coerceIn(42, 92)
        val pace = when (seed.pos) {
            Position.GK -> (ovr - 22).coerceIn(32, 72)
            Position.DEF -> (ovr - 2).coerceIn(40, 93)
            Position.MID -> (ovr - 1).coerceIn(42, 93)
            Position.FWD -> (ovr + 4).coerceIn(45, 97)
        }
        val shooting = when (seed.pos) {
            Position.GK -> (ovr - 38).coerceIn(18, 52)
            Position.DEF -> (ovr - 13).coerceIn(30, 80)
            Position.MID -> (ovr - 2).coerceIn(38, 92)
            Position.FWD -> (ovr + 4).coerceIn(44, 96)
        }
        val passing = when (seed.pos) {
            Position.GK -> (ovr - 9).coerceIn(35, 86)
            Position.DEF -> (ovr - 4).coerceIn(38, 88)
            Position.MID -> (ovr + 4).coerceIn(44, 96)
            Position.FWD -> (ovr - 3).coerceIn(40, 90)
        }
        val defending = when (seed.pos) {
            Position.GK -> (ovr + 2).coerceIn(42, 94)
            Position.DEF -> (ovr + 4).coerceIn(44, 95)
            Position.MID -> (ovr - 5).coerceIn(34, 88)
            Position.FWD -> (ovr - 24).coerceIn(22, 64)
        }

        val ageFactor = when {
            seed.age <= 21 -> 1.25
            seed.age <= 26 -> 1.12
            seed.age <= 30 -> 0.90
            else -> 0.58
        }

        // Realistic FC-style valuation scaling from €0.02M (44 OVR) to €115M (91 OVR)
        val rawValueM = when {
            ovr >= 82 -> 24.0 + ((ovr - 81).toDouble().pow(1.75) * 2.1)
            ovr >= 72 -> 4.0 + ((ovr - 71).toDouble().pow(1.65) * 0.45)
            ovr >= 60 -> 0.45 + ((ovr - 59).toDouble().pow(1.55) * 0.08)
            else -> 0.02 + ((ovr - 42).coerceAtLeast(1) * 0.022)
        } * ageFactor

        val marketValueM = if (rawValueM < 1.0) {
            ((rawValueM * 100.0).roundToInt() / 100.0).coerceAtLeast(0.02)
        } else {
            ((rawValueM * 10.0).roundToInt() / 10.0).coerceAtMost(135.0)
        }

        val wageK = when {
            ovr >= 82 -> ((ovr - 75) * 18)
            ovr >= 70 -> ((ovr - 62) * 4)
            ovr >= 58 -> (ovr - 52).coerceAtLeast(2)
            else -> 1
        }

        val contractYears = 1 + (kotlin.math.abs(id.hashCode()) % 4)
        val potentialBonus = when {
            seed.age <= 20 -> 7
            seed.age <= 23 -> 4
            seed.age <= 26 -> 2
            else -> 0
        }
        val pot = (ovr + potentialBonus).coerceIn(ovr, 95)

        return Player(
            id = id,
            name = seed.name,
            age = seed.age,
            nationality = seed.nat,
            position = seed.pos,
            overall = ovr,
            pace = pace,
            shooting = shooting,
            passing = passing,
            defending = defending,
            fitness = 92,
            morale = 80,
            marketValueM = marketValueM,
            wageK = wageK,
            contractYearsLeft = contractYears,
            potential = pot,
            role = role,
            sellingClubId = sellingClubId,
            sellingClubName = sellingClubName,
        )
    }

    /**
     * Builds a complete 18-player matchday squad (2 GK, 6 DEF, 6 MID, 4 FWD: 11 starters + 7 bench)
     * from real player seeds. If fewer than 18 specific seeds are provided for lower-tier clubs,
     * remaining slots are filled deterministically from the country's real-world player pool scaled
     * to [RealClubSeed.clubOvr].
     */
    fun buildClub(
        seed: RealClubSeed,
        fallbackPool: List<RealPlayerSeed>,
    ): Club {
        val targetCounts = mapOf(
            Position.GK to 2,
            Position.DEF to 6,
            Position.MID to 6,
            Position.FWD to 4,
        )

        val chosen = seed.players.toMutableList()
        val usedNames = chosen.map { it.name }.toMutableSet()

        // deterministic rotation offset per club id so clubs don't share identical filler players
        val offset = kotlin.math.abs(seed.id.hashCode())

        targetCounts.forEach { (pos, needed) ->
            var currentCount = chosen.count { it.pos == pos }
            val poolForPos = fallbackPool.filter { it.pos == pos && it.name !in usedNames }
            var idx = 0
            while (currentCount < needed && poolForPos.isNotEmpty() && idx < poolForPos.size * 4) {
                val candidate = poolForPos[(offset + idx) % poolForPos.size]
                if (candidate.name !in usedNames) {
                    val variance = ((offset + idx) % 5) - 2
                    val scaledOvr = (seed.clubOvr + variance).coerceIn(43, 88)
                    chosen += candidate.copy(overall = scaledOvr)
                    usedNames += candidate.name
                    currentCount++
                }
                idx++
            }
        }

        val rawPlayers = chosen.take(18).mapIndexed { i, pSeed ->
            buildPlayer("${seed.id}_p${i + 1}", pSeed)
        }
        val (xiIds, benchIds) = SamplePlayers.autoPickLineup(rawPlayers, seed.formation)
        val xiSet = xiIds.toSet()
        val topThreeIds = rawPlayers.sortedByDescending { it.overall }.take(3).map { it.id }.toSet()

        val players = rawPlayers.map { player ->
            val assignedRole = when {
                player.id in topThreeIds && player.overall >= seed.clubOvr -> com.example.model.SquadRole.CRUCIAL
                player.id in xiSet -> com.example.model.SquadRole.IMPORTANT
                player.age <= 21 && player.overall < seed.clubOvr -> com.example.model.SquadRole.PROSPECT
                else -> com.example.model.SquadRole.ROTATION
            }
            player.copy(role = assignedRole)
        }

        val totalWagesK = players.sumOf { it.wageK }
        val minHeadroom = when (seed.tier) {
            1 -> 120
            2 -> 45
            3 -> 25
            4 -> 15
            else -> 8
        }
        val wageBudgetK = ((totalWagesK * 1.24).roundToInt()).coerceAtLeast(totalWagesK + minHeadroom)

        return Club(
            id = seed.id,
            name = seed.name,
            nameAr = seed.nameAr,
            shortName = seed.shortName,
            country = seed.country,
            divisionId = seed.divisionId,
            tier = seed.tier,
            city = seed.city,
            stadium = seed.stadium,
            primaryColorHex = seed.primaryHex,
            secondaryColorHex = seed.secondaryHex,
            badgeStyle = BadgeStyle.SHIELD,
            crestSpec = seed.crestSpec,
            budgetM = seed.budgetM,
            prestige = seed.clubOvr,
            reputation = seed.clubOvr,
            formation = seed.formation,
            squad = players,
            startingXiIds = xiIds,
            benchIds = benchIds,
            weeklyWageBudgetK = wageBudgetK,
        )
    }
}
