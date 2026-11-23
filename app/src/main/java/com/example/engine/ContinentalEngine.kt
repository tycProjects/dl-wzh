package com.example.engine

import com.example.data.SampleCompetitions
import com.example.data.world.WorldDatabase
import com.example.model.AclEliteState
import com.example.model.Club
import com.example.model.Confederation
import com.example.model.ContinentalTournamentId
import com.example.model.DivisionInfo
import com.example.model.LeagueStanding
import com.example.model.MatchFixture
import com.example.model.SwissTournamentState
import kotlin.random.Random

/**
 * Orchestrates authentic modern Continental Competitions:
 * - UEFA Champions League (36 teams, 8 Swiss rounds, Top 8 -> R16, 9th-24th -> 2-leg Play-off, R16 -> Final)
 * - UEFA Europa League (36 teams, 8 Swiss rounds, Top 8 -> R16, 9th-24th -> 2-leg Play-off, R16 -> Final)
 * - UEFA Conference League (36 teams, 6 Swiss rounds, Top 8 -> R16, 9th-24th -> 2-leg Play-off, R16 -> Final)
 * - AFC Champions League Elite (West 12 & East 12, 8 rounds each -> Regional R16 -> Centralized QF -> SF -> Final)
 */
object ContinentalEngine {

    data class ContinentalSeedBundle(
        val ucl: SwissTournamentState,
        val uel: SwissTournamentState,
        val uecl: SwissTournamentState,
        val acl: AclEliteState,
        val userTournament: ContinentalTournamentId?,
    )

    fun seedSeasonTournaments(
        clubs: List<Club>,
        divisions: List<DivisionInfo>,
        standings: List<LeagueStanding>?,
        userClubId: String,
        domesticCupWinnerId: String? = null,
        random: Random = Random.Default,
    ): ContinentalSeedBundle {
        val clubMap = clubs.associateBy { it.id }

        fun rankClubsForDivision(divId: String): List<Club> {
            return if (!standings.isNullOrEmpty() && standings.any { it.divisionId == divId && it.played > 0 }) {
                SampleCompetitions.sortedStandingsForDivision(divId, standings, clubs)
                    .mapNotNull { clubMap[it.clubId] }
            } else {
                clubs.filter { it.divisionId == divId }
                    .sortedWith(compareByDescending<Club> { it.prestige }.thenByDescending { it.averageOverall })
            }
        }

        // Partition 108 unique UEFA clubs across UCL (36), UEL (36), UECL (36)
        val uefaTier1Divs = divisions.filter {
            it.country.confederation == Confederation.UEFA && it.country.isPlayable && it.tier == 1
        }
        val uefaPoolClubs = clubs.filter { it.divisionId == "uefa_pool" }
            .sortedByDescending { it.prestige }

        val uclIds = mutableListOf<String>()
        val uelIds = mutableListOf<String>()
        val ueclIds = mutableListOf<String>()
        val assignedEuro = mutableSetOf<String>()

        // 1. Strict league qualification spots across all 8 UEFA playable leagues
        uefaTier1Divs.forEach { div ->
            val ranked = rankClubsForDivision(div.id)
            ranked.take(div.uclSpots).forEach { c ->
                if (assignedEuro.add(c.id)) uclIds += c.id
            }
            ranked.drop(div.uclSpots).take(div.uelSpots).forEach { c ->
                if (assignedEuro.add(c.id)) uelIds += c.id
            }
            ranked.drop(div.uclSpots + div.uelSpots).take(div.ueclSpots).forEach { c ->
                if (assignedEuro.add(c.id)) ueclIds += c.id
            }
        }

        // If user won domestic cup in UEFA and didn't make UCL, grant them a UEL spot
        if (domesticCupWinnerId != null && domesticCupWinnerId !in assignedEuro) {
            val cupClub = clubMap[domesticCupWinnerId]
            if (cupClub != null && cupClub.country.confederation == Confederation.UEFA && uelIds.size < 36) {
                if (assignedEuro.add(cupClub.id)) uelIds += cupClub.id
            }
        }

        // 2. Fill remaining UCL, UEL, UECL spots from UEFA nations pool & remaining Tier-1 UEFA clubs
        val remainingUefaPool = uefaPoolClubs.filter { it.id !in assignedEuro }
        for (club in remainingUefaPool) {
            if (uclIds.size < 36 && assignedEuro.add(club.id)) {
                uclIds += club.id
            } else if (uelIds.size < 36 && assignedEuro.add(club.id)) {
                uelIds += club.id
            } else if (ueclIds.size < 36 && assignedEuro.add(club.id)) {
                ueclIds += club.id
            }
        }

        val remainingLeagueT1 = uefaTier1Divs
            .flatMap { rankClubsForDivision(it.id) }
            .filter { it.id !in assignedEuro }
            .sortedByDescending { it.prestige }
        for (club in remainingLeagueT1) {
            if (uclIds.size < 36 && assignedEuro.add(club.id)) {
                uclIds += club.id
            } else if (uelIds.size < 36 && assignedEuro.add(club.id)) {
                uelIds += club.id
            } else if (ueclIds.size < 36 && assignedEuro.add(club.id)) {
                ueclIds += club.id
            }
        }

        val uclState = createSwissTournament(ContinentalTournamentId.UCL, uclIds.take(36), clubs, random)
        val uelState = createSwissTournament(ContinentalTournamentId.UEL, uelIds.take(36), clubs, random)
        val ueclState = createSwissTournament(ContinentalTournamentId.UECL, ueclIds.take(36), clubs, random)

        // Partition AFC Champions League Elite: 12 West + 12 East
        val afcWestDivs = listOf("ksa_t1", "uae_t1", "qat_t1", "irq_t1")
        val afcEastDivs = listOf("jpn_t1", "kor_t1")

        val westIds = mutableListOf<String>()
        val eastIds = mutableListOf<String>()
        val assignedWest = mutableSetOf<String>()
        val assignedEast = mutableSetOf<String>()

        afcWestDivs.forEach { divId ->
            val div = divisions.find { it.id == divId } ?: return@forEach
            rankClubsForDivision(divId).take(div.aclSpots).forEach { c ->
                if (assignedWest.add(c.id)) westIds += c.id
            }
        }
        clubs.filter { it.divisionId == "afc_west_pool" }
            .sortedByDescending { it.prestige }
            .forEach { c ->
                if (westIds.size < 12 && assignedWest.add(c.id)) westIds += c.id
            }

        afcEastDivs.forEach { divId ->
            val div = divisions.find { it.id == divId } ?: return@forEach
            rankClubsForDivision(divId).take(div.aclSpots).forEach { c ->
                if (assignedEast.add(c.id)) eastIds += c.id
            }
        }
        clubs.filter { it.divisionId == "afc_east_pool" }
            .sortedByDescending { it.prestige }
            .forEach { c ->
                if (eastIds.size < 12 && assignedEast.add(c.id)) eastIds += c.id
            }

        val aclState = createAclEliteTournament(westIds.take(12), eastIds.take(12), random)

        val userTourney = when {
            userClubId in uclState.participantIds -> ContinentalTournamentId.UCL
            userClubId in uelState.participantIds -> ContinentalTournamentId.UEL
            userClubId in ueclState.participantIds -> ContinentalTournamentId.UECL
            userClubId in aclState.westClubIds || userClubId in aclState.eastClubIds -> ContinentalTournamentId.ACL_ELITE
            else -> null
        }

        return ContinentalSeedBundle(
            ucl = uclState,
            uel = uelState,
            uecl = ueclState,
            acl = aclState,
            userTournament = userTourney,
        )
    }

    fun createSwissTournament(
        tournamentId: ContinentalTournamentId,
        participantIds: List<String>,
        clubs: List<Club>,
        random: Random = Random.Default,
    ): SwissTournamentState {
        val uniqueIds = participantIds.distinct()
        val rounds = tournamentId.totalLeagueRounds
        val fixtures = generateBalancedLeaguePhaseFixtures(
            tournamentId = tournamentId,
            clubIds = uniqueIds,
            rounds = rounds,
            region = null,
            random = random,
        )
        val standings = uniqueIds.map { LeagueStanding(clubId = it, divisionId = tournamentId.name) }
        return SwissTournamentState(
            tournamentId = tournamentId,
            participantIds = uniqueIds,
            standings = standings,
            swissFixtures = fixtures,
            currentPhase = "SWISS",
            swissRoundsPlayed = 0,
        )
    }

    fun createAclEliteTournament(
        westClubIds: List<String>,
        eastClubIds: List<String>,
        random: Random = Random.Default,
    ): AclEliteState {
        val west = westClubIds.distinct()
        val east = eastClubIds.distinct()
        val westFixtures = generateBalancedLeaguePhaseFixtures(
            tournamentId = ContinentalTournamentId.ACL_ELITE,
            clubIds = west,
            rounds = 8,
            region = "WEST",
            random = random,
        )
        val eastFixtures = generateBalancedLeaguePhaseFixtures(
            tournamentId = ContinentalTournamentId.ACL_ELITE,
            clubIds = east,
            rounds = 8,
            region = "EAST",
            random = random,
        )
        return AclEliteState(
            westClubIds = west,
            eastClubIds = east,
            westStandings = west.map { LeagueStanding(clubId = it, divisionId = "ACL_WEST") },
            eastStandings = east.map { LeagueStanding(clubId = it, divisionId = "ACL_EAST") },
            westFixtures = westFixtures,
            eastFixtures = eastFixtures,
            currentPhase = "LEAGUE",
            leagueRoundsPlayed = 0,
        )
    }

    /**
     * Generates [rounds] rounds via circle-method scheduling with balanced home/away tracking,
     * guaranteeing every club plays exactly [rounds] unique opponents (1 match per round).
     */
    private fun generateBalancedLeaguePhaseFixtures(
        tournamentId: ContinentalTournamentId,
        clubIds: List<String>,
        rounds: Int,
        region: String?,
        random: Random,
    ): List<MatchFixture> {
        val n = clubIds.size
        if (n < 2) return emptyList()

        val rotation = clubIds.shuffled(random).toMutableList()
        if (rotation.size % 2 == 1) {
            rotation += "__BYE__"
        }
        val m = rotation.size
        val homeCounts = clubIds.associateWith { 0 }.toMutableMap()
        val fixtures = mutableListOf<MatchFixture>()
        val effectiveRounds = rounds.coerceAtMost(m - 1)
        val stageTag = when (tournamentId) {
            ContinentalTournamentId.UCL -> "UCL League Phase"
            ContinentalTournamentId.UEL -> "UEL League Phase"
            ContinentalTournamentId.UECL -> "UECL League Phase"
            ContinentalTournamentId.ACL_ELITE -> "ACL ${region ?: ""} League"
        }

        for (r in 1..effectiveRounds) {
            var matchNum = 1
            for (i in 0 until m / 2) {
                val c1 = rotation[i]
                val c2 = rotation[m - 1 - i]
                if (c1 == "__BYE__" || c2 == "__BYE__") continue

                val h1 = homeCounts[c1] ?: 0
                val h2 = homeCounts[c2] ?: 0
                val firstIsHome = when {
                    h1 < h2 -> true
                    h2 < h1 -> false
                    else -> (i + r) % 2 == 0
                }
                val home = if (firstIsHome) c1 else c2
                val away = if (firstIsHome) c2 else c1
                homeCounts[home] = (homeCounts[home] ?: 0) + 1

                val regSuffix = if (region != null) "${region.lowercase()}_" else ""
                fixtures += MatchFixture(
                    id = "cont_${tournamentId.name.lowercase()}_${regSuffix}r${r}_m${matchNum++}",
                    round = r,
                    divisionId = tournamentId.name,
                    isCup = false,
                    isContinental = true,
                    continentalId = tournamentId,
                    region = region,
                    cupStageName = "$stageTag MD $r",
                    homeClubId = home,
                    awayClubId = away,
                )
            }
            val last = rotation.removeAt(rotation.lastIndex)
            rotation.add(1, last)
        }
        return fixtures
    }

    fun sortedStandings(standings: List<LeagueStanding>, clubs: List<Club>): List<LeagueStanding> {
        val clubMap = clubs.associateBy { it.id }
        return standings.sortedWith(
            compareByDescending<LeagueStanding> { it.points }
                .thenByDescending { it.goalDifference }
                .thenByDescending { it.goalsFor }
                .thenByDescending { it.won }
                .thenBy { clubMap[it.clubId]?.name ?: "" },
        )
    }

    fun applyFixturesToStandings(
        current: List<LeagueStanding>,
        playedFixtures: List<MatchFixture>,
    ): List<LeagueStanding> {
        val map = current.associateBy { it.clubId }.toMutableMap()
        playedFixtures.forEach { f ->
            val h = map[f.homeClubId] ?: LeagueStanding(f.homeClubId, f.divisionId)
            val a = map[f.awayClubId] ?: LeagueStanding(f.awayClubId, f.divisionId)
            val (hPts, aPts, hRes, aRes) = when {
                f.homeScore > f.awayScore -> listOf(3, 0, "W", "L")
                f.awayScore > f.homeScore -> listOf(0, 3, "L", "W")
                else -> listOf(1, 1, "D", "D")
            }
            map[f.homeClubId] = h.copy(
                played = h.played + 1,
                won = h.won + if (hRes == "W") 1 else 0,
                drawn = h.drawn + if (hRes == "D") 1 else 0,
                lost = h.lost + if (hRes == "L") 1 else 0,
                goalsFor = h.goalsFor + f.homeScore,
                goalsAgainst = h.goalsAgainst + f.awayScore,
                points = h.points + (hPts as Int),
                form = (h.form + (hRes as String)).takeLast(5),
            )
            map[f.awayClubId] = a.copy(
                played = a.played + 1,
                won = a.won + if (aRes == "W") 1 else 0,
                drawn = a.drawn + if (aRes == "D") 1 else 0,
                lost = a.lost + if (aRes == "L") 1 else 0,
                goalsFor = a.goalsFor + f.awayScore,
                goalsAgainst = a.goalsAgainst + f.homeScore,
                points = a.points + (aPts as Int),
                form = (a.form + (aRes as String)).takeLast(5),
            )
        }
        return map.values.toList()
    }

    fun advanceSwissPhaseIfNeeded(
        state: SwissTournamentState,
        clubs: List<Club>,
    ): SwissTournamentState {
        val prefix = state.tournamentId.shortCode
        return when (state.currentPhase) {
            "SWISS" -> {
                val totalRounds = state.tournamentId.totalLeagueRounds
                if (state.swissRoundsPlayed < totalRounds) return state
                val ranked = sortedStandings(state.standings, clubs)
                if (ranked.size < 24) return state

                // Ranks 9..16 (indices 8..15) vs Ranks 17..24 (indices 16..23): 9v24, 10v23...
                val l1 = (0 until 8).map { i ->
                    val seeded = ranked[8 + i].clubId
                    val unseeded = ranked[23 - i].clubId
                    MatchFixture(
                        id = "${state.tournamentId.name.lowercase()}_po1_${i + 1}",
                        round = 1,
                        divisionId = state.tournamentId.name,
                        isCup = false,
                        isContinental = true,
                        continentalId = state.tournamentId,
                        cupStageName = "$prefix Play-off Leg 1",
                        aggregateNote = "Leg 1 of 2 (Aggregate)",
                        homeClubId = unseeded,
                        awayClubId = seeded,
                    )
                }
                state.copy(playoffFirstLeg = l1, currentPhase = "PLAYOFF_L1")
            }

            "PLAYOFF_L1" -> {
                if (state.playoffFirstLeg.isEmpty() || state.playoffFirstLeg.any { !it.played }) return state
                val l2 = state.playoffFirstLeg.mapIndexed { i, leg1 ->
                    MatchFixture(
                        id = "${state.tournamentId.name.lowercase()}_po2_${i + 1}",
                        round = 2,
                        divisionId = state.tournamentId.name,
                        isCup = true,
                        isContinental = true,
                        continentalId = state.tournamentId,
                        cupStageName = "$prefix Play-off Leg 2",
                        aggregateNote = "Leg 1: ${leg1.awayScore}-${leg1.homeScore}",
                        homeClubId = leg1.awayClubId, // Seeded plays Leg 2 at home
                        awayClubId = leg1.homeClubId,
                    )
                }
                state.copy(playoffSecondLeg = l2, currentPhase = "PLAYOFF_L2")
            }

            "PLAYOFF_L2" -> {
                if (state.playoffSecondLeg.isEmpty() || state.playoffSecondLeg.any { !it.played }) return state
                val ranked = sortedStandings(state.standings, clubs)
                val top8 = ranked.take(8).map { it.clubId }
                val playoffWinners = state.playoffFirstLeg.zip(state.playoffSecondLeg).map { (l1, l2) ->
                    resolveTwoLegWinner(l1, l2)
                }
                if (top8.size < 8 || playoffWinners.size < 8) return state

                // Pair Top 8 seeds against the 8 Play-off winners
                val r16 = (0 until 8).map { i ->
                    MatchFixture(
                        id = "${state.tournamentId.name.lowercase()}_r16_${i + 1}",
                        round = 1,
                        divisionId = state.tournamentId.name,
                        isCup = true,
                        isContinental = true,
                        continentalId = state.tournamentId,
                        cupStageName = "$prefix Round of 16",
                        homeClubId = top8[i],
                        awayClubId = playoffWinners[7 - i],
                    )
                }
                state.copy(knockoutFixtures = r16, currentPhase = "R16")
            }

            "R16" -> advanceSwissKnockoutRound(state, completedRound = 1, nextRound = 2, nextPhase = "QF", stageTitle = "$prefix Quarter-Final")
            "QF" -> advanceSwissKnockoutRound(state, completedRound = 2, nextRound = 3, nextPhase = "SF", stageTitle = "$prefix Semi-Final")
            "SF" -> advanceSwissKnockoutRound(state, completedRound = 3, nextRound = 4, nextPhase = "FINAL", stageTitle = "$prefix Final")
            "FINAL" -> {
                val finalMatch = state.knockoutFixtures.firstOrNull { it.round == 4 && it.played } ?: return state
                state.copy(
                    currentPhase = "DONE",
                    championClubId = finalMatch.winnerClubId(),
                )
            }
            else -> state
        }
    }

    private fun advanceSwissKnockoutRound(
        state: SwissTournamentState,
        completedRound: Int,
        nextRound: Int,
        nextPhase: String,
        stageTitle: String,
    ): SwissTournamentState {
        val matches = state.knockoutFixtures.filter { it.round == completedRound }
        if (matches.isEmpty() || matches.any { !it.played }) return state
        val winners = matches.mapNotNull { it.winnerClubId() }
        if (winners.size < 2) return state

        val nextMatches = (0 until winners.size / 2).map { i ->
            MatchFixture(
                id = "${state.tournamentId.name.lowercase()}_r${nextRound}_${i + 1}",
                round = nextRound,
                divisionId = state.tournamentId.name,
                isCup = true,
                isContinental = true,
                continentalId = state.tournamentId,
                cupStageName = stageTitle,
                homeClubId = winners[i * 2],
                awayClubId = winners[i * 2 + 1],
            )
        }
        return state.copy(
            knockoutFixtures = state.knockoutFixtures + nextMatches,
            currentPhase = nextPhase,
        )
    }

    fun resolveTwoLegWinner(leg1: MatchFixture, leg2: MatchFixture): String {
        // leg1.home == leg2.away, leg1.away == leg2.home
        val leg2HomeAgg = leg2.homeScore + leg1.awayScore
        val leg2AwayAgg = leg2.awayScore + leg1.homeScore
        return when {
            leg2HomeAgg > leg2AwayAgg -> leg2.homeClubId
            leg2AwayAgg > leg2HomeAgg -> leg2.awayClubId
            (leg2.homePenalties ?: 0) > (leg2.awayPenalties ?: 0) -> leg2.homeClubId
            (leg2.awayPenalties ?: 0) > (leg2.homePenalties ?: 0) -> leg2.awayClubId
            else -> leg2.homeClubId
        }
    }

    fun advanceAclPhaseIfNeeded(
        state: AclEliteState,
        clubs: List<Club>,
    ): AclEliteState {
        return when (state.currentPhase) {
            "LEAGUE" -> {
                if (state.leagueRoundsPlayed < 8) return state
                val westTop8 = sortedStandings(state.westStandings, clubs).take(8).map { it.clubId }
                val eastTop8 = sortedStandings(state.eastStandings, clubs).take(8).map { it.clubId }
                if (westTop8.size < 8 || eastTop8.size < 8) return state

                val pairings = listOf(0 to 7, 3 to 4, 1 to 6, 2 to 5)
                val westR16 = pairings.mapIndexed { idx, (h, a) ->
                    MatchFixture(
                        id = "acl_r16_w_${idx + 1}",
                        round = 1,
                        divisionId = ContinentalTournamentId.ACL_ELITE.name,
                        isCup = true,
                        isContinental = true,
                        continentalId = ContinentalTournamentId.ACL_ELITE,
                        region = "WEST",
                        cupStageName = "ACL West R16",
                        homeClubId = westTop8[h],
                        awayClubId = westTop8[a],
                    )
                }
                val eastR16 = pairings.mapIndexed { idx, (h, a) ->
                    MatchFixture(
                        id = "acl_r16_e_${idx + 1}",
                        round = 1,
                        divisionId = ContinentalTournamentId.ACL_ELITE.name,
                        isCup = true,
                        isContinental = true,
                        continentalId = ContinentalTournamentId.ACL_ELITE,
                        region = "EAST",
                        cupStageName = "ACL East R16",
                        homeClubId = eastTop8[h],
                        awayClubId = eastTop8[a],
                    )
                }
                state.copy(knockoutFixtures = westR16 + eastR16, currentPhase = "R16")
            }

            "R16" -> {
                val r16 = state.knockoutFixtures.filter { it.round == 1 }
                if (r16.isEmpty() || r16.any { !it.played }) return state
                val westWinners = r16.filter { it.region == "WEST" }.mapNotNull { it.winnerClubId() }
                val eastWinners = r16.filter { it.region == "EAST" }.mapNotNull { it.winnerClubId() }
                if (westWinners.size < 4 || eastWinners.size < 4) return state

                // Centralized Cross-Region Quarter-Finals (West vs East!)
                val qf = (0 until 4).map { i ->
                    MatchFixture(
                        id = "acl_qf_${i + 1}",
                        round = 2,
                        divisionId = ContinentalTournamentId.ACL_ELITE.name,
                        isCup = true,
                        isContinental = true,
                        continentalId = ContinentalTournamentId.ACL_ELITE,
                        region = "CROSS",
                        cupStageName = "ACL Centralized QF",
                        homeClubId = westWinners[i],
                        awayClubId = eastWinners[3 - i],
                    )
                }
                state.copy(knockoutFixtures = state.knockoutFixtures + qf, currentPhase = "QF")
            }

            "QF" -> {
                val qf = state.knockoutFixtures.filter { it.round == 2 }
                if (qf.isEmpty() || qf.any { !it.played }) return state
                val winners = qf.mapNotNull { it.winnerClubId() }
                if (winners.size < 4) return state
                val sf = (0 until 2).map { i ->
                    MatchFixture(
                        id = "acl_sf_${i + 1}",
                        round = 3,
                        divisionId = ContinentalTournamentId.ACL_ELITE.name,
                        isCup = true,
                        isContinental = true,
                        continentalId = ContinentalTournamentId.ACL_ELITE,
                        region = "CROSS",
                        cupStageName = "ACL Semi-Final",
                        homeClubId = winners[i * 2],
                        awayClubId = winners[i * 2 + 1],
                    )
                }
                state.copy(knockoutFixtures = state.knockoutFixtures + sf, currentPhase = "SF")
            }

            "SF" -> {
                val sf = state.knockoutFixtures.filter { it.round == 3 }
                if (sf.isEmpty() || sf.any { !it.played }) return state
                val winners = sf.mapNotNull { it.winnerClubId() }
                if (winners.size < 2) return state
                val finalMatch = MatchFixture(
                    id = "acl_final_1",
                    round = 4,
                    divisionId = ContinentalTournamentId.ACL_ELITE.name,
                    isCup = true,
                    isContinental = true,
                    continentalId = ContinentalTournamentId.ACL_ELITE,
                    region = "CROSS",
                    cupStageName = "ACL Elite Final",
                    homeClubId = winners[0],
                    awayClubId = winners[1],
                )
                state.copy(knockoutFixtures = state.knockoutFixtures + finalMatch, currentPhase = "FINAL")
            }

            "FINAL" -> {
                val finalMatch = state.knockoutFixtures.firstOrNull { it.round == 4 && it.played } ?: return state
                state.copy(currentPhase = "DONE", championClubId = finalMatch.winnerClubId())
            }

            else -> state
        }
    }

    /**
     * Returns the currently unplayed fixtures in the active phase of a Swiss tournament.
     */
    fun currentSwissRoundFixtures(state: SwissTournamentState): List<MatchFixture> = when (state.currentPhase) {
        "SWISS" -> {
            val nextRound = state.swissRoundsPlayed + 1
            state.swissFixtures.filter { it.round == nextRound && !it.played }
        }
        "PLAYOFF_L1" -> state.playoffFirstLeg.filter { !it.played }
        "PLAYOFF_L2" -> state.playoffSecondLeg.filter { !it.played }
        "R16" -> state.knockoutFixtures.filter { it.round == 1 && !it.played }
        "QF" -> state.knockoutFixtures.filter { it.round == 2 && !it.played }
        "SF" -> state.knockoutFixtures.filter { it.round == 3 && !it.played }
        "FINAL" -> state.knockoutFixtures.filter { it.round == 4 && !it.played }
        else -> emptyList()
    }

    fun currentAclRoundFixtures(state: AclEliteState): List<MatchFixture> = when (state.currentPhase) {
        "LEAGUE" -> {
            val nextRound = state.leagueRoundsPlayed + 1
            (state.westFixtures + state.eastFixtures).filter { it.round == nextRound && !it.played }
        }
        "R16" -> state.knockoutFixtures.filter { it.round == 1 && !it.played }
        "QF" -> state.knockoutFixtures.filter { it.round == 2 && !it.played }
        "SF" -> state.knockoutFixtures.filter { it.round == 3 && !it.played }
        "FINAL" -> state.knockoutFixtures.filter { it.round == 4 && !it.played }
        else -> emptyList()
    }

    fun applyCompletedSwissFixtures(
        state: SwissTournamentState,
        completedFixtures: List<MatchFixture>,
        clubs: List<Club>,
    ): SwissTournamentState {
        if (completedFixtures.isEmpty()) return state
        val byId = completedFixtures.associateBy { it.id }

        val updated = when (state.currentPhase) {
            "SWISS" -> {
                val merged = state.swissFixtures.map { byId[it.id] ?: it }
                val newStandings = applyFixturesToStandings(state.standings, completedFixtures)
                state.copy(
                    swissFixtures = merged,
                    standings = newStandings,
                    swissRoundsPlayed = state.swissRoundsPlayed + 1,
                )
            }
            "PLAYOFF_L1" -> state.copy(playoffFirstLeg = state.playoffFirstLeg.map { byId[it.id] ?: it })
            "PLAYOFF_L2" -> state.copy(playoffSecondLeg = state.playoffSecondLeg.map { byId[it.id] ?: it })
            else -> state.copy(knockoutFixtures = state.knockoutFixtures.map { byId[it.id] ?: it })
        }
        return advanceSwissPhaseIfNeeded(updated, clubs)
    }

    fun applyCompletedAclFixtures(
        state: AclEliteState,
        completedFixtures: List<MatchFixture>,
        clubs: List<Club>,
    ): AclEliteState {
        if (completedFixtures.isEmpty()) return state
        val byId = completedFixtures.associateBy { it.id }

        val updated = when (state.currentPhase) {
            "LEAGUE" -> {
                val westDone = completedFixtures.filter { it.region == "WEST" }
                val eastDone = completedFixtures.filter { it.region == "EAST" }
                state.copy(
                    westFixtures = state.westFixtures.map { byId[it.id] ?: it },
                    eastFixtures = state.eastFixtures.map { byId[it.id] ?: it },
                    westStandings = applyFixturesToStandings(state.westStandings, westDone),
                    eastStandings = applyFixturesToStandings(state.eastStandings, eastDone),
                    leagueRoundsPlayed = state.leagueRoundsPlayed + 1,
                )
            }
            else -> state.copy(knockoutFixtures = state.knockoutFixtures.map { byId[it.id] ?: it })
        }
        return advanceAclPhaseIfNeeded(updated, clubs)
    }
}
