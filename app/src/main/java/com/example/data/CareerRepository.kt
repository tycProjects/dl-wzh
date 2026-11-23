package com.example.data

import android.content.Context
import com.example.data.world.RealPlayerRosterBuilder
import com.example.data.world.WorldDatabase
import com.example.engine.CareerEventsEngine
import com.example.engine.ContinentalEngine
import com.example.engine.LiveMatchSession
import com.example.engine.MatchEngine
import com.example.engine.PressConferenceGenerator
import com.example.model.AclEliteState
import com.example.model.CareerSaveState
import com.example.model.Club
import com.example.model.ContinentalTournamentId
import com.example.model.CountryCode
import com.example.model.CrestShape
import com.example.model.Formation
import com.example.model.InboxCategory
import com.example.model.InboxMessage
import com.example.model.LeagueStanding
import com.example.model.MatchFixture
import com.example.model.OfferStatus
import com.example.model.SeasonSummaryReport
import com.example.model.SwissTournamentState
import com.example.model.TrainingFocus
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Coordinates 18-country multi-tier career persistence, real-world club selection,
 * domestic cups, interleaved UCL/UEL/UECL (36-team Swiss) & ACL Elite (East/West),
 * promotion/playoff/relegation transitions, Youth Academy, Inbox, Contracts, and Press Conferences.
 */
class CareerRepository(context: Context) {

    private val saveFile = File(context.filesDir, "touchline_career_v3.json")
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(CareerSaveState::class.java)

    private val _state = MutableStateFlow(loadOrDefault())
    val state: StateFlow<CareerSaveState> = _state.asStateFlow()

    private fun loadOrDefault(): CareerSaveState {
        if (saveFile.exists()) {
            try {
                val json = saveFile.readText()
                val loaded = adapter.fromJson(json)
                if (loaded != null &&
                    loaded.careerStarted &&
                    loaded.clubs.isNotEmpty() &&
                    loaded.divisions.isNotEmpty() &&
                    loaded.uclState != null
                ) {
                    return loaded
                }
            } catch (_: Exception) {
                // Fall back to fresh real-world catalog
            }
        }
        return createInitialLobbyState()
    }

    private fun persist(newState: CareerSaveState) {
        _state.value = newState
        try {
            saveFile.writeText(adapter.toJson(newState))
        } catch (_: Exception) {
            // Non-fatal in-memory operation continues
        }
    }

    private fun createInitialLobbyState(): CareerSaveState {
        val divisions = WorldDatabase.allDivisions
        val clubs = WorldDatabase.createAllClubs()
        return CareerSaveState(
            careerStarted = false,
            divisions = divisions,
            clubs = clubs,
            transferMarket = SamplePlayers.marketPool(),
            standings = SampleCompetitions.createInitialStandings(clubs),
        )
    }

    fun startNewCareer(
        selectedClubId: String,
        customClubName: String,
        customShortName: String,
        primaryHex: String,
        secondaryHex: String,
        crestShape: CrestShape,
        managerName: String,
    ) {
        val divisions = WorldDatabase.allDivisions
        val clubs = WorldDatabase.createAllClubs().map { club ->
            if (club.id == selectedClubId) {
                val customizedCrest = crestShape != club.crestSpec.shape ||
                    !primaryHex.equals(club.primaryColorHex, ignoreCase = true) ||
                    !secondaryHex.equals(club.secondaryColorHex, ignoreCase = true)
                club.copy(
                    name = customClubName.trim().ifBlank { club.name },
                    shortName = customShortName.trim().uppercase().take(3).ifBlank { club.shortName },
                    primaryColorHex = primaryHex,
                    secondaryColorHex = secondaryHex,
                    crestSpec = club.crestSpec.copy(
                        shape = crestShape,
                        customSvg = if (customizedCrest) null else club.crestSpec.customSvg,
                    ),
                )
            } else {
                club
            }
        }

        val userClub = clubs.first { it.id == selectedClubId }
        val userDivClubs = clubs.filter { it.divisionId == userClub.divisionId }
        val totalRounds = ((userDivClubs.size - 1) * 2).coerceAtLeast(10)

        val leagueFixtures = SampleCompetitions.generateAllLeagueFixtures(divisions, clubs)
        val cupFixtures = SampleCompetitions.generateDomesticCupRoundOf16(
            country = userClub.country,
            userClubId = userClub.id,
            clubs = clubs,
        )
        val startCupStage = cupFixtures.minOfOrNull { it.round } ?: 1

        val contBundle = ContinentalEngine.seedSeasonTournaments(
            clubs = clubs,
            divisions = divisions,
            standings = null,
            userClubId = userClub.id,
        )
        val standings = SampleCompetitions.createInitialStandings(clubs)
        val userDivName = divisions.find { it.id == userClub.divisionId }?.name ?: "League"
        val cleanManager = managerName.trim().ifBlank { "Career Manager" }
        val contName = contBundle.userTournament?.displayName ?: userClub.country.continentalName

        val initialYouth = CareerEventsEngine.generateYouthProspects(userClub, count = 3, seedOffset = 1)
        val welcomeMsgs = buildList {
            add(
                InboxMessage(
                    id = "welcome_board_s1",
                    category = InboxCategory.BOARD,
                    title = "Board Mandate: ${userClub.name}",
                    body = "Welcome $cleanManager! You take charge in $userDivName with a ${RealPlayerRosterBuilder.formatBudget(userClub.budgetM)} budget and €${userClub.totalWeeklyWageK}k/wk payroll.",
                    matchdayLabel = "Pre-Season",
                ),
            )
            if (contBundle.userTournament != null) {
                add(
                    InboxMessage(
                        id = "welcome_cont_s1",
                        category = InboxCategory.CONTINENTAL,
                        title = "${contBundle.userTournament.shortCode} Qualification Confirmed",
                        body = "${userClub.name} enter the ${contBundle.userTournament.displayName} league phase! Continental fixtures run interleaved alongside domestic matchdays.",
                        matchdayLabel = "Draw",
                    ),
                )
            }
        }

        persist(
            CareerSaveState(
                careerStarted = true,
                seasonNumber = 1,
                currentMatchday = 1,
                totalLeagueRounds = totalRounds,
                currentCupStage = startCupStage,
                currentContinentalStage = 1,
                userClubId = selectedClubId,
                managerName = cleanManager,
                boardConfidence = 78,
                divisions = divisions,
                clubs = clubs,
                transferMarket = SamplePlayers.marketPool(),
                youthAcademy = initialYouth,
                inboxMessages = welcomeMsgs,
                leagueFixtures = leagueFixtures,
                cupFixtures = cupFixtures,
                continentalFixtures = emptyList(),
                continentalCompetitionName = contName,
                uclState = contBundle.ucl,
                uelState = contBundle.uel,
                ueclState = contBundle.uecl,
                aclState = contBundle.acl,
                standings = standings,
                trainingUsedThisWeek = false,
                recentNews = listOf(
                    "${userClub.name} appoint $cleanManager in $userDivName (${RealPlayerRosterBuilder.formatBudget(userClub.budgetM)}).",
                ),
            ),
        )
    }

    fun resetToClubSelection() {
        persist(createInitialLobbyState())
    }

    /**
     * Determines what stage is active next for the user:
     * 1. Domestic Cup milestone
     * 2. Interleaved Continental League/Swiss or Knockout fixture involving the user's club
     * 3. Domestic League Matchday (1..totalLeagueRounds)
     * 4. End-of-season Continental Knockouts & Promotion Play-offs
     */
    fun nextMatchForUser(current: CareerSaveState = _state.value): MatchFixture? {
        if (!current.careerStarted || current.seasonSummary != null) return null
        val userId = current.userClubId

        val pendingCupStage = pendingDomesticCupStage(current)
        if (pendingCupStage != null) {
            val userCup = current.cupFixtures.firstOrNull {
                it.round == pendingCupStage && !it.played && (it.homeClubId == userId || it.awayClubId == userId)
            }
            if (userCup != null) return userCup
        }

        val interleavedCont = nextUserContinentalFixture(current)
        if (interleavedCont != null) return interleavedCont

        if (current.currentMatchday <= current.totalLeagueRounds) {
            val userClub = current.clubs.find { it.id == userId } ?: return null
            return current.leagueFixtures.firstOrNull {
                it.divisionId == userClub.divisionId &&
                    it.round == current.currentMatchday &&
                    !it.played &&
                    (it.homeClubId == userId || it.awayClubId == userId)
            }
        }

        // End-of-season continental knockouts if league & cup done
        val postSeasonCont = nextUserPostSeasonContinentalFixture(current)
        if (postSeasonCont != null) return postSeasonCont

        val userPlayoff = current.playoffFixtures.firstOrNull {
            !it.played && (it.homeClubId == userId || it.awayClubId == userId)
        }
        if (userPlayoff != null) return userPlayoff

        return null
    }

    private fun nextUserContinentalFixture(current: CareerSaveState): MatchFixture? {
        val userId = current.userClubId
        val md = current.currentMatchday
        val totalMd = current.totalLeagueRounds.coerceAtLeast(1)

        fun SwissTournamentState?.userMatchIfDue(): MatchFixture? {
            if (this == null || currentPhase != "SWISS") return null
            if (userId !in participantIds) return null
            val targetPlayed = if (md > totalMd) {
                tournamentId.totalLeagueRounds
            } else {
                ((md - 1) * tournamentId.totalLeagueRounds) / totalMd
            }
            if (swissRoundsPlayed >= targetPlayed) return null
            return ContinentalEngine.currentSwissRoundFixtures(this)
                .firstOrNull { it.homeClubId == userId || it.awayClubId == userId }
        }

        current.uclState.userMatchIfDue()?.let { return it }
        current.uelState.userMatchIfDue()?.let { return it }
        current.ueclState.userMatchIfDue()?.let { return it }

        val acl = current.aclState
        if (acl != null && acl.currentPhase == "LEAGUE" && (userId in acl.westClubIds || userId in acl.eastClubIds)) {
            val targetPlayed = if (md > totalMd) 8 else ((md - 1) * 8) / totalMd
            if (acl.leagueRoundsPlayed < targetPlayed) {
                return ContinentalEngine.currentAclRoundFixtures(acl)
                    .firstOrNull { it.homeClubId == userId || it.awayClubId == userId }
            }
        }
        return null
    }

    private fun nextUserPostSeasonContinentalFixture(current: CareerSaveState): MatchFixture? {
        if (current.currentMatchday <= current.totalLeagueRounds) return null
        val userId = current.userClubId

        listOfNotNull(current.uclState, current.uelState, current.ueclState).forEach { swiss ->
            if (swiss.currentPhase != "DONE") {
                val match = ContinentalEngine.currentSwissRoundFixtures(swiss)
                    .firstOrNull { it.homeClubId == userId || it.awayClubId == userId }
                if (match != null) return match
            }
        }
        val acl = current.aclState
        if (acl != null && acl.currentPhase != "DONE") {
            val match = ContinentalEngine.currentAclRoundFixtures(acl)
                .firstOrNull { it.homeClubId == userId || it.awayClubId == userId }
            if (match != null) return match
        }
        return null
    }

    private fun pendingDomesticCupStage(current: CareerSaveState): Int? = when {
        current.currentMatchday > 2 && current.currentCupStage == 1 -> 1
        current.currentMatchday > 5 && current.currentCupStage == 2 -> 2
        current.currentMatchday > 8 && current.currentCupStage == 3 -> 3
        current.currentMatchday > current.totalLeagueRounds && current.currentCupStage == 4 -> 4
        else -> null
    }

    fun pendingNonUserStageLabel(current: CareerSaveState = _state.value): String? {
        if (!current.careerStarted || current.seasonSummary != null) return null
        if (nextMatchForUser(current) != null) return null

        val userClub = current.clubs.find { it.id == current.userClubId } ?: return null
        val cupStage = pendingDomesticCupStage(current)
        if (cupStage != null && current.cupFixtures.any { it.round == cupStage && !it.played }) {
            return "Simulate ${userClub.country.domesticCupName} Round"
        }

        // Check if user's Swiss/ACL round finished or user was eliminated in continental knockouts
        if (hasPendingUserTournamentRoundWithoutUser(current)) {
            return "Advance ${current.continentalCompetitionName}"
        }

        if (current.currentMatchday > current.totalLeagueRounds) {
            if (current.playoffFixtures.any { !it.played }) {
                return "Simulate Promotion Play-offs"
            }
            return "Finalize Season & Crowns"
        }
        return null
    }

    private fun hasPendingUserTournamentRoundWithoutUser(current: CareerSaveState): Boolean {
        val userId = current.userClubId
        val md = current.currentMatchday
        val totalMd = current.totalLeagueRounds.coerceAtLeast(1)

        listOfNotNull(current.uclState, current.uelState, current.ueclState).forEach { swiss ->
            if (userId in swiss.participantIds && swiss.currentPhase != "DONE") {
                val target = if (md > totalMd) swiss.tournamentId.totalLeagueRounds else ((md - 1) * swiss.tournamentId.totalLeagueRounds) / totalMd
                if (swiss.currentPhase == "SWISS" && swiss.swissRoundsPlayed < target) return true
                if (md > totalMd && swiss.currentPhase != "DONE") return true
            }
        }
        val acl = current.aclState
        if (acl != null && (userId in acl.westClubIds || userId in acl.eastClubIds) && acl.currentPhase != "DONE") {
            val target = if (md > totalMd) 8 else ((md - 1) * 8) / totalMd
            if (acl.currentPhase == "LEAGUE" && acl.leagueRoundsPlayed < target) return true
            if (md > totalMd && acl.currentPhase != "DONE") return true
        }
        return false
    }

    fun advanceNonUserStage() {
        val current = _state.value
        val userClub = current.clubs.find { it.id == current.userClubId } ?: return

        val cupStage = pendingDomesticCupStage(current)
        if (cupStage != null && current.cupFixtures.any { it.round == cupStage && !it.played }) {
            simulateCupStage(stage = cupStage, userClub = userClub)
            return
        }

        if (hasPendingUserTournamentRoundWithoutUser(current)) {
            val nextState = advanceContinentalMilestones(current, simAllRemainingIfPostSeason = current.currentMatchday > current.totalLeagueRounds)
            val finalState = maybeSpawnPlayoffs(nextState)
            persist(finalState)
            return
        }

        if (current.currentMatchday > current.totalLeagueRounds) {
            if (current.playoffFixtures.any { !it.played }) {
                simulateRemainingPlayoffsAndCheckFinal(userClub)
            } else {
                finalizeSeasonAndBuildSummary()
            }
        }
    }

    private fun simulateCupStage(stage: Int, userClub: Club) {
        val current = _state.value
        val stageMatches = current.cupFixtures.filter { it.round == stage && !it.played }
        if (stageMatches.isEmpty()) return

        var updatedClubs = current.clubs
        val finished = stageMatches.map { f ->
            val h = updatedClubs.first { it.id == f.homeClubId }
            val a = updatedClubs.first { it.id == f.awayClubId }
            val sim = MatchEngine.simulateAiMatch(f, h, a)
            updatedClubs = applySessionStatsToClubs(updatedClubs, sim, updateFitness = false)
            f.copy(
                homeScore = sim.homeScore,
                awayScore = sim.awayScore,
                homePenalties = sim.homePenalties,
                awayPenalties = sim.awayPenalties,
                played = true,
                homeStats = sim.homeStats,
                awayStats = sim.awayStats,
                events = sim.events,
                manOfTheMatch = sim.manOfTheMatch,
            )
        }

        val merged = current.cupFixtures.map { existing -> finished.find { it.id == existing.id } ?: existing }.toMutableList()
        var champId = current.cupChampionClubId
        if (stage < 4) {
            merged += SampleCompetitions.generateNextKnockoutRound(
                completedStage = stage,
                existingFixtures = merged,
                isContinental = false,
                isSaudi = userClub.country == CountryCode.KSA,
            )
        } else {
            champId = merged.firstOrNull { it.round == 4 && it.played }?.winnerClubId()
        }

        val compName = userClub.country.domesticCupName
        val news = if (champId != null) {
            val winnerName = updatedClubs.find { it.id == champId }?.name ?: "Champion"
            listOf("$winnerName won the $compName!") + current.recentNews.take(4)
        } else {
            listOf("$compName round completed.") + current.recentNews.take(4)
        }

        var nextState = current.copy(
            clubs = updatedClubs,
            cupFixtures = merged,
            currentCupStage = stage + 1,
            cupChampionClubId = champId,
            recentNews = news,
        )
        nextState = maybeSpawnPlayoffs(nextState)
        persist(nextState)
    }

    private fun simulateRemainingPlayoffsAndCheckFinal(userClub: Club) {
        val current = _state.value
        val unplayed = current.playoffFixtures.filter { !it.played }
        if (unplayed.isEmpty()) {
            finalizeSeasonAndBuildSummary()
            return
        }

        var updatedClubs = current.clubs
        val finished = unplayed.map { f ->
            val h = updatedClubs.first { it.id == f.homeClubId }
            val a = updatedClubs.first { it.id == f.awayClubId }
            val sim = MatchEngine.simulateAiMatch(f, h, a)
            updatedClubs = applySessionStatsToClubs(updatedClubs, sim, updateFitness = false)
            f.copy(
                homeScore = sim.homeScore,
                awayScore = sim.awayScore,
                homePenalties = sim.homePenalties,
                awayPenalties = sim.awayPenalties,
                played = true,
                homeStats = sim.homeStats,
                awayStats = sim.awayStats,
                events = sim.events,
                manOfTheMatch = sim.manOfTheMatch,
            )
        }

        val merged = current.playoffFixtures.map { e -> finished.find { it.id == e.id } ?: e }.toMutableList()
        val semis = merged.filter { it.round == 1 && it.played }
        val finalExists = merged.any { it.round == 2 }
        if (semis.size == 2 && !finalExists) {
            val winners = semis.mapNotNull { it.winnerClubId() }
            if (winners.size == 2) {
                merged += MatchFixture(
                    id = "po_${userClub.divisionId}_final",
                    round = 2,
                    divisionId = userClub.divisionId,
                    isCup = true,
                    isPlayoff = true,
                    cupStageName = "Promotion Play-off Final",
                    homeClubId = winners[0],
                    awayClubId = winners[1],
                )
            }
        }

        val nextState = current.copy(clubs = updatedClubs, playoffFixtures = merged)
        persist(nextState)

        if (nextState.playoffFixtures.all { it.played }) {
            finalizeSeasonAndBuildSummary()
        }
    }

    private fun maybeSpawnPlayoffs(state: CareerSaveState): CareerSaveState {
        if (state.currentMatchday <= state.totalLeagueRounds) return state
        if (state.currentCupStage <= 4) return state
        if (state.playoffFixtures.isNotEmpty()) return state

        val userClub = state.clubs.find { it.id == state.userClubId } ?: return state
        val userDiv = state.divisions.find { it.id == userClub.divisionId } ?: return state
        if (userDiv.playoffSpots <= 0) return state

        val sorted = SampleCompetitions.sortedStandingsForDivision(userDiv.id, state.standings, state.clubs)
        val po = SampleCompetitions.generatePlayoffSemisOrFinal(userDiv, sorted)
        return state.copy(playoffFixtures = po)
    }

    fun updateFormation(formation: Formation) {
        val current = _state.value
        val updatedClubs = current.clubs.map { club ->
            if (club.id == current.userClubId) {
                val (xi, bench) = SamplePlayers.autoPickLineup(club.squad, formation)
                club.copy(formation = formation, startingXiIds = xi, benchIds = bench)
            } else {
                club
            }
        }
        persist(current.copy(clubs = updatedClubs))
    }

    fun swapSquadPlayers(firstPlayerId: String, secondPlayerId: String) {
        if (firstPlayerId == secondPlayerId) return
        val current = _state.value
        val updatedClubs = current.clubs.map { club ->
            if (club.id != current.userClubId) return@map club

            val xi = club.startingXiIds.toMutableList()
            val bench = club.benchIds.toMutableList()
            val xiA = xi.indexOf(firstPlayerId)
            val xiB = xi.indexOf(secondPlayerId)
            val bA = bench.indexOf(firstPlayerId)
            val bB = bench.indexOf(secondPlayerId)

            when {
                xiA != -1 && xiB != -1 -> {
                    xi[xiA] = secondPlayerId; xi[xiB] = firstPlayerId
                }
                xiA != -1 && bB != -1 -> {
                    xi[xiA] = secondPlayerId; bench[bB] = firstPlayerId
                }
                bA != -1 && xiB != -1 -> {
                    bench[bA] = secondPlayerId; xi[xiB] = firstPlayerId
                }
                bA != -1 && bB != -1 -> {
                    bench[bA] = secondPlayerId; bench[bB] = firstPlayerId
                }
            }
            club.copy(startingXiIds = xi, benchIds = bench)
        }
        persist(current.copy(clubs = updatedClubs))
    }

    fun applyTraining(focus: TrainingFocus): String {
        val current = _state.value
        if (current.trainingUsedThisWeek) return "Training already completed before this match."

        val updatedClubs = current.clubs.map { club ->
            if (club.id != current.userClubId) return@map club
            val trainedSquad = club.squad.map { p ->
                val bonusOvr = when {
                    focus == TrainingFocus.ATTACKING && (p.position.name == "FWD" || p.position.name == "MID") && p.age <= 24 && p.overall < p.potential -> 1
                    focus == TrainingFocus.DEFENDING && (p.position.name == "DEF" || p.position.name == "GK") && p.age <= 24 && p.overall < p.potential -> 1
                    else -> 0
                }
                p.copy(
                    fitness = (p.fitness + focus.fitnessDelta).coerceIn(45, 100),
                    morale = (p.morale + focus.moraleDelta).coerceIn(40, 100),
                    overall = (p.overall + bonusOvr).coerceAtMost(94),
                )
            }
            club.copy(squad = trainedSquad)
        }

        val msg = "${focus.title} session complete (${focus.statNote})."
        persist(
            current.copy(
                clubs = updatedClubs,
                trainingUsedThisWeek = true,
                lastTrainingNote = msg,
                recentNews = listOf(msg) + current.recentNews.take(4),
            ),
        )
        return msg
    }

    fun buyPlayer(playerId: String): Result<String> {
        val current = _state.value
        val userClub = current.clubs.firstOrNull { it.id == current.userClubId }
            ?: return Result.failure(IllegalStateException("No active club"))
        val target = current.transferMarket.firstOrNull { it.id == playerId }
            ?: return Result.failure(IllegalArgumentException("Player no longer available"))

        if (userClub.budgetM < target.marketValueM) {
            val needStr = RealPlayerRosterBuilder.formatBudget(target.marketValueM)
            return Result.failure(IllegalStateException("Insufficient budget (need $needStr)"))
        }
        if (userClub.squad.size >= 24) {
            return Result.failure(IllegalStateException("Squad limit reached (max 24 players)"))
        }

        val newBudget = ((userClub.budgetM - target.marketValueM) * 100.0).roundToInt() / 100.0
        val updatedSquad = userClub.squad + target.copy(fitness = 95, morale = 88, contractYearsLeft = 4)
        val updatedBench = userClub.benchIds + target.id

        val updatedClubs = current.clubs.map { club ->
            if (club.id == userClub.id) {
                club.copy(budgetM = newBudget, squad = updatedSquad, benchIds = updatedBench)
            } else {
                club
            }
        }
        val remainingMarket = current.transferMarket.filterNot { it.id == playerId }
        val feeStr = RealPlayerRosterBuilder.formatBudget(target.marketValueM)
        val news = "TRANSFER: ${userClub.name} signed ${target.name} (${target.position.label}, ${target.overall} OVR) for $feeStr."

        persist(
            current.copy(
                clubs = updatedClubs,
                transferMarket = remainingMarket,
                recentNews = listOf(news) + current.recentNews.take(4),
            ),
        )
        return Result.success(news)
    }

    fun sellPlayer(playerId: String): Result<String> {
        val current = _state.value
        val userClub = current.clubs.firstOrNull { it.id == current.userClubId }
            ?: return Result.failure(IllegalStateException("No active club"))

        if (userClub.squad.size <= 13) {
            return Result.failure(IllegalStateException("Minimum squad size is 13 players."))
        }
        val player = userClub.squad.firstOrNull { it.id == playerId }
            ?: return Result.failure(IllegalArgumentException("Player not found in squad"))

        val salePrice = ((player.marketValueM * 0.9) * 100.0).roundToInt() / 100.0
        val newBudget = ((userClub.budgetM + salePrice) * 100.0).roundToInt() / 100.0
        val remainingSquad = userClub.squad.filterNot { it.id == playerId }

        val (xi, bench) = if (playerId in userClub.startingXiIds) {
            SamplePlayers.autoPickLineup(remainingSquad, userClub.formation)
        } else {
            userClub.startingXiIds to userClub.benchIds.filterNot { it == playerId }
        }

        val updatedClubs = current.clubs.map { club ->
            if (club.id == userClub.id) {
                club.copy(
                    budgetM = newBudget,
                    squad = remainingSquad,
                    startingXiIds = xi,
                    benchIds = bench,
                )
            } else {
                club
            }
        }
        val updatedMarket = listOf(player.copy(goals = 0, assists = 0, appearances = 0)) + current.transferMarket
        val feeStr = RealPlayerRosterBuilder.formatBudget(salePrice)
        val news = "SOLD: ${player.name} departed ${userClub.name} for $feeStr."

        persist(
            current.copy(
                clubs = updatedClubs,
                transferMarket = updatedMarket,
                recentNews = listOf(news) + current.recentNews.take(4),
            ),
        )
        return Result.success(news)
    }

    // --- Inbox, Youth Academy & Contracts ---

    fun markInboxRead(messageId: String) {
        val current = _state.value
        val updated = current.inboxMessages.map { if (it.id == messageId) it.copy(isRead = true) else it }
        persist(current.copy(inboxMessages = updated))
    }

    fun respondToTransferOffer(messageId: String, accept: Boolean): Result<String> {
        val current = _state.value
        val msg = current.inboxMessages.firstOrNull { it.id == messageId }
            ?: return Result.failure(IllegalArgumentException("Message not found"))
        val offer = msg.offer ?: return Result.failure(IllegalArgumentException("No transfer offer attached"))
        if (offer.status != OfferStatus.PENDING) {
            return Result.failure(IllegalStateException("Offer already resolved"))
        }

        val userClub = current.clubs.firstOrNull { it.id == current.userClubId }
            ?: return Result.failure(IllegalStateException("No active club"))

        if (!accept) {
            val updatedMsgs = current.inboxMessages.map { m ->
                if (m.id == messageId) m.copy(isRead = true, offer = offer.copy(status = OfferStatus.REJECTED)) else m
            }
            persist(current.copy(inboxMessages = updatedMsgs))
            return Result.success("Rejected ${offer.buyingClubName}'s bid for ${offer.playerName}.")
        }

        if (userClub.squad.size <= 13) {
            return Result.failure(IllegalStateException("Minimum squad size is 13 players."))
        }
        val player = userClub.squad.firstOrNull { it.id == offer.playerId }
            ?: return Result.failure(IllegalStateException("Player is no longer in squad."))

        val newBudget = ((userClub.budgetM + offer.offerAmountM) * 100.0).roundToInt() / 100.0
        val remainingSquad = userClub.squad.filterNot { it.id == player.id }
        val (xi, bench) = if (player.id in userClub.startingXiIds) {
            SamplePlayers.autoPickLineup(remainingSquad, userClub.formation)
        } else {
            userClub.startingXiIds to userClub.benchIds.filterNot { it == player.id }
        }

        val updatedClubs = current.clubs.map { club ->
            if (club.id == userClub.id) {
                club.copy(budgetM = newBudget, squad = remainingSquad, startingXiIds = xi, benchIds = bench)
            } else {
                club
            }
        }
        val feeStr = RealPlayerRosterBuilder.formatBudget(offer.offerAmountM)
        val news = "SOLD: ${offer.playerName} joined ${offer.buyingClubName} for $feeStr."
        val updatedMsgs = current.inboxMessages.map { m ->
            if (m.id == messageId) m.copy(isRead = true, offer = offer.copy(status = OfferStatus.ACCEPTED)) else m
        }
        persist(
            current.copy(
                clubs = updatedClubs,
                inboxMessages = updatedMsgs,
                recentNews = (listOf(news) + current.recentNews).take(5),
            ),
        )
        return Result.success(news)
    }

    fun scoutYouthProspects(): String {
        val current = _state.value
        val userClub = current.clubs.firstOrNull { it.id == current.userClubId } ?: return "No club active"
        val newBatch = CareerEventsEngine.generateYouthProspects(
            club = userClub,
            count = 3,
            seedOffset = current.currentMatchday + current.youthAcademy.size,
        )
        val combined = (newBatch + current.youthAcademy).take(8)
        persist(current.copy(youthAcademy = combined))
        return "Scouted ${newBatch.size} new U18 academy prospects!"
    }

    fun promoteYouthProspect(prospectId: String): Result<String> {
        val current = _state.value
        val userClub = current.clubs.firstOrNull { it.id == current.userClubId }
            ?: return Result.failure(IllegalStateException("No active club"))
        if (userClub.squad.size >= 24) {
            return Result.failure(IllegalStateException("Senior squad full (max 24 players)"))
        }
        val prospect = current.youthAcademy.firstOrNull { it.id == prospectId }
            ?: return Result.failure(IllegalArgumentException("Prospect not found"))

        val graduate = CareerEventsEngine.prospectToPlayer(prospect)
        val updatedClubs = current.clubs.map { club ->
            if (club.id == userClub.id) {
                club.copy(squad = club.squad + graduate, benchIds = club.benchIds + graduate.id)
            } else {
                club
            }
        }
        val msg = "ACADEMY GRADUATE: ${graduate.name} (${graduate.position.label}, ${graduate.overall} OVR) promoted to senior squad!"
        persist(
            current.copy(
                clubs = updatedClubs,
                youthAcademy = current.youthAcademy.filterNot { it.id == prospectId },
                recentNews = (listOf(msg) + current.recentNews).take(5),
            ),
        )
        return Result.success(msg)
    }

    fun releaseYouthProspect(prospectId: String) {
        val current = _state.value
        persist(current.copy(youthAcademy = current.youthAcademy.filterNot { it.id == prospectId }))
    }

    fun renewPlayerContract(playerId: String, offerWageK: Int, extraYears: Int): Result<String> {
        val current = _state.value
        val userClub = current.clubs.firstOrNull { it.id == current.userClubId }
            ?: return Result.failure(IllegalStateException("No active club"))
        val target = userClub.squad.firstOrNull { it.id == playerId }
            ?: return Result.failure(IllegalArgumentException("Player not found"))

        val minAcceptable = (target.wageK * 0.95).roundToInt().coerceAtLeast(1)
        if (offerWageK < minAcceptable) {
            return Result.failure(IllegalStateException("${target.name} declined €${offerWageK}k/wk (wants at least €${minAcceptable}k/wk)."))
        }

        val updatedClubs = current.clubs.map { club ->
            if (club.id != userClub.id) return@map club
            club.copy(
                squad = club.squad.map { p ->
                    if (p.id == playerId) {
                        p.copy(
                            wageK = offerWageK,
                            contractYearsLeft = (p.contractYearsLeft + extraYears).coerceIn(1, 5),
                            morale = (p.morale + 5).coerceAtMost(100),
                        )
                    } else {
                        p
                    }
                },
            )
        }
        val msg = "CONTRACT RENEWED: ${target.name} signed for $extraYears more year(s) at €${offerWageK}k/wk."
        persist(
            current.copy(
                clubs = updatedClubs,
                recentNews = (listOf(msg) + current.recentNews).take(5),
            ),
        )
        return Result.success(msg)
    }

    // --- Match Commit & Interleaved Continental Progression ---

    fun commitMatchSession(session: LiveMatchSession) {
        val current = _state.value
        val userClub = current.clubs.first { it.id == current.userClubId }
        val userFixture = session.fixture.copy(
            homeScore = session.homeScore,
            awayScore = session.awayScore,
            homePenalties = session.homePenalties,
            awayPenalties = session.awayPenalties,
            played = true,
            homeStats = session.homeStats,
            awayStats = session.awayStats,
            events = session.events,
            manOfTheMatch = session.manOfTheMatch,
        )

        var updatedClubs = applySessionStatsToClubs(current.clubs, session, updateFitness = true)
        updatedClubs = progressInjuriesAndSuspensions(updatedClubs, userClub.id, session)
        updatedClubs = recoverSquadFitness(updatedClubs)

        var nextState = when {
            userFixture.isPlayoff -> commitPlayoffMatch(current, userFixture, updatedClubs, session)
            userFixture.continentalId != null -> commitContinentalFixture(current, userFixture, updatedClubs, session)
            userFixture.isCup -> commitDomesticCupMatch(current, userFixture, updatedClubs, session, userClub)
            else -> commitLeagueMatchday(current, userFixture, updatedClubs, session, userClub)
        }

        // Advance background interleaved continental rounds to match currentMatchday
        nextState = advanceContinentalMilestones(nextState, simAllRemainingIfPostSeason = false)
        nextState = maybeSpawnPlayoffs(nextState)
        val presser = PressConferenceGenerator.createIfKeyMatch(session, nextState)
        persist(nextState.copy(pendingPressConference = presser))

        // Auto-finalize if league + cup + playoffs are all finished and no user action remains
        if (nextState.currentMatchday > nextState.totalLeagueRounds &&
            nextState.currentCupStage > 4 &&
            nextState.playoffFixtures.all { it.played } &&
            nextUserPostSeasonContinentalFixture(nextState) == null &&
            !hasPendingUserTournamentRoundWithoutUser(nextState) &&
            nextState.seasonSummary == null
        ) {
            finalizeSeasonAndBuildSummary()
        }
    }

    private fun commitContinentalFixture(
        current: CareerSaveState,
        userFixture: MatchFixture,
        initialClubs: List<Club>,
        session: LiveMatchSession,
    ): CareerSaveState {
        var updatedClubs = initialClubs
        val cId = userFixture.continentalId!!

        fun simRoundList(list: List<MatchFixture>): List<MatchFixture> = list.map { f ->
            if (f.id == userFixture.id) {
                userFixture
            } else if (!f.played) {
                val h = updatedClubs.first { it.id == f.homeClubId }
                val a = updatedClubs.first { it.id == f.awayClubId }
                val sim = MatchEngine.simulateAiMatch(f, h, a)
                updatedClubs = applySessionStatsToClubs(updatedClubs, sim, updateFitness = false)
                f.copy(
                    homeScore = sim.homeScore,
                    awayScore = sim.awayScore,
                    homePenalties = sim.homePenalties,
                    awayPenalties = sim.awayPenalties,
                    played = true,
                    homeStats = sim.homeStats,
                    awayStats = sim.awayStats,
                    events = sim.events,
                    manOfTheMatch = sim.manOfTheMatch,
                )
            } else {
                f
            }
        }

        var nextUcl = current.uclState
        var nextUel = current.uelState
        var nextUecl = current.ueclState
        var nextAcl = current.aclState

        when (cId) {
            ContinentalTournamentId.UCL -> nextUcl?.let { swiss ->
                val done = simRoundList(ContinentalEngine.currentSwissRoundFixtures(swiss))
                nextUcl = resolveSwissPlayoffTiesIfNeeded(ContinentalEngine.applyCompletedSwissFixtures(swiss, done, updatedClubs))
            }
            ContinentalTournamentId.UEL -> nextUel?.let { swiss ->
                val done = simRoundList(ContinentalEngine.currentSwissRoundFixtures(swiss))
                nextUel = resolveSwissPlayoffTiesIfNeeded(ContinentalEngine.applyCompletedSwissFixtures(swiss, done, updatedClubs))
            }
            ContinentalTournamentId.UECL -> nextUecl?.let { swiss ->
                val done = simRoundList(ContinentalEngine.currentSwissRoundFixtures(swiss))
                nextUecl = resolveSwissPlayoffTiesIfNeeded(ContinentalEngine.applyCompletedSwissFixtures(swiss, done, updatedClubs))
            }
            ContinentalTournamentId.ACL_ELITE -> nextAcl?.let { acl ->
                val done = simRoundList(ContinentalEngine.currentAclRoundFixtures(acl))
                nextAcl = ContinentalEngine.applyCompletedAclFixtures(acl, done, updatedClubs)
            }
        }

        val scoreLine = "${cId.shortCode}: ${session.homeClub.shortName} ${session.homeScore}-${session.awayScore} ${session.awayClub.shortName}"
        return current.copy(
            clubs = updatedClubs,
            uclState = nextUcl,
            uelState = nextUel,
            ueclState = nextUecl,
            aclState = nextAcl,
            trainingUsedThisWeek = false,
            lastTrainingNote = null,
            recentNews = (listOf(scoreLine) + current.recentNews).take(5),
        )
    }

    private fun resolveSwissPlayoffTiesIfNeeded(state: SwissTournamentState): SwissTournamentState {
        if (state.playoffSecondLeg.isEmpty() || state.playoffSecondLeg.any { !it.played }) return state
        val updatedL2 = state.playoffFirstLeg.zip(state.playoffSecondLeg).map { (l1, l2) ->
            val hAgg = l2.homeScore + l1.awayScore
            val aAgg = l2.awayScore + l1.homeScore
            if (hAgg == aAgg && (l2.homePenalties == null || l2.awayPenalties == null)) {
                val hp = 4 + Random.nextInt(2)
                var ap = 4 + Random.nextInt(2)
                if (hp == ap) ap++
                l2.copy(homePenalties = hp, awayPenalties = ap, aggregateNote = "Agg $hAgg-$aAgg ($hp-$ap pens)")
            } else {
                l2.copy(aggregateNote = "Agg $hAgg-$aAgg")
            }
        }
        return state.copy(playoffSecondLeg = updatedL2)
    }

    /**
     * Simulates non-user continental fixtures that have caught up to [CareerSaveState.currentMatchday]
     * (or completes all remaining continental stages if [simAllRemainingIfPostSeason] is true).
     */
    private fun advanceContinentalMilestones(
        state: CareerSaveState,
        simAllRemainingIfPostSeason: Boolean,
    ): CareerSaveState {
        val userId = state.userClubId
        val clubMap = state.clubs.associateBy { it.id }
        val md = state.currentMatchday
        val totalMd = state.totalLeagueRounds.coerceAtLeast(1)

        fun quickSimFixtures(list: List<MatchFixture>): List<MatchFixture> = list.map { f ->
            if (f.played) return@map f
            val h = clubMap[f.homeClubId]
            val a = clubMap[f.awayClubId]
            val (hg, ag) = quickScoreline(h?.averageOverall ?: 72, a?.averageOverall ?: 72)
            if (f.isCup && hg == ag) {
                val hp = 4 + Random.nextInt(2)
                var ap = 4 + Random.nextInt(2)
                if (hp == ap) ap++
                f.copy(homeScore = hg, awayScore = ag, homePenalties = hp, awayPenalties = ap, played = true)
            } else {
                f.copy(homeScore = hg, awayScore = ag, played = true)
            }
        }

        fun advanceSwiss(swiss: SwissTournamentState): SwissTournamentState {
            if (swiss.currentPhase == "DONE") return swiss
            var cur: SwissTournamentState = swiss
            val userIn = userId in cur.participantIds

            // Loop while Swiss/Knockout round should advance without blocking on user
            var guard = 0
            while (cur.currentPhase != "DONE" && guard++ < 18) {
                val roundFixtures = ContinentalEngine.currentSwissRoundFixtures(cur)
                if (roundFixtures.isEmpty()) break
                val userPlaysNow = userIn && roundFixtures.any { it.homeClubId == userId || it.awayClubId == userId }
                if (userPlaysNow) break

                if (cur.currentPhase == "SWISS") {
                    val target = if (md > totalMd) {
                        cur.tournamentId.totalLeagueRounds
                    } else {
                        ((md - 1) * cur.tournamentId.totalLeagueRounds) / totalMd
                    }
                    if (!simAllRemainingIfPostSeason && cur.swissRoundsPlayed >= target) break
                } else {
                    if (!simAllRemainingIfPostSeason) break
                }

                val done = quickSimFixtures(roundFixtures)
                cur = resolveSwissPlayoffTiesIfNeeded(ContinentalEngine.applyCompletedSwissFixtures(cur, done, state.clubs))
            }
            return cur
        }

        fun advanceAcl(acl: AclEliteState): AclEliteState {
            if (acl.currentPhase == "DONE") return acl
            var cur: AclEliteState = acl
            val userIn = userId in cur.westClubIds || userId in cur.eastClubIds

            var guard = 0
            while (cur.currentPhase != "DONE" && guard++ < 16) {
                val roundFixtures = ContinentalEngine.currentAclRoundFixtures(cur)
                if (roundFixtures.isEmpty()) break
                val userPlaysNow = userIn && roundFixtures.any { it.homeClubId == userId || it.awayClubId == userId }
                if (userPlaysNow) break

                if (cur.currentPhase == "LEAGUE") {
                    val target = if (md > totalMd) 8 else ((md - 1) * 8) / totalMd
                    if (!simAllRemainingIfPostSeason && cur.leagueRoundsPlayed >= target) break
                } else {
                    if (!simAllRemainingIfPostSeason) break
                }

                val done = quickSimFixtures(roundFixtures)
                cur = ContinentalEngine.applyCompletedAclFixtures(cur, done, state.clubs)
            }
            return cur
        }

        return state.copy(
            uclState = state.uclState?.let(::advanceSwiss),
            uelState = state.uelState?.let(::advanceSwiss),
            ueclState = state.ueclState?.let(::advanceSwiss),
            aclState = state.aclState?.let(::advanceAcl),
        )
    }

    private fun commitPlayoffMatch(
        current: CareerSaveState,
        userFixture: MatchFixture,
        initialClubs: List<Club>,
        session: LiveMatchSession,
    ): CareerSaveState {
        var updatedClubs = initialClubs
        val stage = userFixture.round
        val others = current.playoffFixtures.filter { it.round == stage && !it.played && it.id != userFixture.id }
        val simOthers = others.map { f ->
            val h = updatedClubs.first { it.id == f.homeClubId }
            val a = updatedClubs.first { it.id == f.awayClubId }
            val sim = MatchEngine.simulateAiMatch(f, h, a)
            updatedClubs = applySessionStatsToClubs(updatedClubs, sim, updateFitness = false)
            f.copy(
                homeScore = sim.homeScore,
                awayScore = sim.awayScore,
                homePenalties = sim.homePenalties,
                awayPenalties = sim.awayPenalties,
                played = true,
                homeStats = sim.homeStats,
                awayStats = sim.awayStats,
                events = sim.events,
                manOfTheMatch = sim.manOfTheMatch,
            )
        }

        val doneMap = (listOf(userFixture) + simOthers).associateBy { it.id }
        val merged = current.playoffFixtures.map { doneMap[it.id] ?: it }.toMutableList()
        if (stage == 1) {
            val winners = merged.filter { it.round == 1 && it.played }.mapNotNull { it.winnerClubId() }
            if (winners.size == 2) {
                merged += MatchFixture(
                    id = "po_${userFixture.divisionId}_final",
                    round = 2,
                    divisionId = userFixture.divisionId,
                    isCup = true,
                    isPlayoff = true,
                    cupStageName = "Promotion Play-off Final",
                    homeClubId = winners[0],
                    awayClubId = winners[1],
                )
            }
        }
        val headline = "PLAY-OFF: ${session.homeClub.shortName} ${session.homeScore}-${session.awayScore} ${session.awayClub.shortName}"
        return current.copy(
            clubs = updatedClubs,
            playoffFixtures = merged,
            trainingUsedThisWeek = false,
            lastTrainingNote = null,
            recentNews = (listOf(headline) + current.recentNews).take(5),
        )
    }

    private fun commitDomesticCupMatch(
        current: CareerSaveState,
        userFixture: MatchFixture,
        initialClubs: List<Club>,
        session: LiveMatchSession,
        userClub: Club,
    ): CareerSaveState {
        var updatedClubs = initialClubs
        val stage = userFixture.round
        val others = current.cupFixtures.filter { it.round == stage && !it.played && it.id != userFixture.id }
        val simOthers = others.map { f ->
            val h = updatedClubs.first { it.id == f.homeClubId }
            val a = updatedClubs.first { it.id == f.awayClubId }
            val sim = MatchEngine.simulateAiMatch(f, h, a)
            updatedClubs = applySessionStatsToClubs(updatedClubs, sim, updateFitness = false)
            f.copy(
                homeScore = sim.homeScore,
                awayScore = sim.awayScore,
                homePenalties = sim.homePenalties,
                awayPenalties = sim.awayPenalties,
                played = true,
                homeStats = sim.homeStats,
                awayStats = sim.awayStats,
                events = sim.events,
                manOfTheMatch = sim.manOfTheMatch,
            )
        }

        val doneMap = (listOf(userFixture) + simOthers).associateBy { it.id }
        val merged = current.cupFixtures.map { doneMap[it.id] ?: it }.toMutableList()
        var champId = current.cupChampionClubId

        if (stage < 4) {
            merged += SampleCompetitions.generateNextKnockoutRound(
                completedStage = stage,
                existingFixtures = merged,
                isContinental = false,
                isSaudi = userClub.country == CountryCode.KSA,
            )
        } else {
            champId = merged.firstOrNull { it.round == 4 && it.played }?.winnerClubId()
        }

        val compLabel = userClub.country.domesticCupName
        val summary = "${session.homeClub.shortName} ${session.homeScore}-${session.awayScore} ${session.awayClub.shortName} (${userFixture.cupStageName})"
        val extraNews = if (champId != null) {
            val winnerName = updatedClubs.find { it.id == champId }?.name ?: "Champion"
            listOf("$winnerName won the $compLabel!", summary)
        } else {
            listOf(summary)
        }

        return current.copy(
            clubs = updatedClubs,
            cupFixtures = merged,
            currentCupStage = stage + 1,
            cupChampionClubId = champId,
            trainingUsedThisWeek = false,
            lastTrainingNote = null,
            recentNews = (extraNews + current.recentNews).take(5),
        )
    }

    private fun commitLeagueMatchday(
        current: CareerSaveState,
        userFixture: MatchFixture,
        initialClubs: List<Club>,
        session: LiveMatchSession,
        userClub: Club,
    ): CareerSaveState {
        var updatedClubs = initialClubs
        val round = userFixture.round

        val sameDivOthers = current.leagueFixtures.filter {
            it.divisionId == userClub.divisionId && it.round == round && !it.played && it.id != userFixture.id
        }
        val simulatedSameDiv = sameDivOthers.map { f ->
            val h = updatedClubs.first { it.id == f.homeClubId }
            val a = updatedClubs.first { it.id == f.awayClubId }
            val sim = MatchEngine.simulateAiMatch(f, h, a)
            updatedClubs = applySessionStatsToClubs(updatedClubs, sim, updateFitness = false)
            f.copy(
                homeScore = sim.homeScore,
                awayScore = sim.awayScore,
                played = true,
                homeStats = sim.homeStats,
                awayStats = sim.awayStats,
                events = sim.events,
                manOfTheMatch = sim.manOfTheMatch,
            )
        }

        val clubMap = updatedClubs.associateBy { it.id }
        val otherDivFixtures = current.leagueFixtures.filter {
            it.divisionId != userClub.divisionId && it.round == round && !it.played
        }
        val simulatedOtherDivs = otherDivFixtures.map { f ->
            val h = clubMap[f.homeClubId]
            val a = clubMap[f.awayClubId]
            val (hg, ag) = quickScoreline(h?.averageOverall ?: 65, a?.averageOverall ?: 65)
            f.copy(homeScore = hg, awayScore = ag, played = true)
        }

        val allRoundResults = listOf(userFixture) + simulatedSameDiv + simulatedOtherDivs
        val roundById = allRoundResults.associateBy { it.id }
        val mergedLeague = current.leagueFixtures.map { roundById[it.id] ?: it }
        val updatedStandings = updateStandings(current.standings, allRoundResults)

        // Balanced weekly wages/revenue & board confidence + Inbox events
        val sortedDiv = SampleCompetitions.sortedStandingsForDivision(userClub.divisionId, updatedStandings, updatedClubs)
        val userDiv = current.divisions.find { it.id == userClub.divisionId }
        val latestUserClub = updatedClubs.first { it.id == userClub.id }
        val finTick = CareerEventsEngine.evaluateWeeklyFinancesAndBoard(
            club = latestUserClub,
            sortedStandings = sortedDiv,
            division = userDiv,
            currentConfidence = current.boardConfidence,
            matchday = round,
        )
        updatedClubs = updatedClubs.map { if (it.id == latestUserClub.id) it.copy(budgetM = finTick.updatedBudgetM) else it }

        val newMsgs = mutableListOf<InboxMessage>()
        finTick.boardWarningMessage?.let { newMsgs += it }
        CareerEventsEngine.maybeGenerateTransferOffer(latestUserClub, updatedClubs, round)?.let { newMsgs += it }
        if (session.playerInjuries.keys.any { id -> latestUserClub.squad.any { it.id == id } }) {
            val hurtNames = latestUserClub.squad.filter { it.id in session.playerInjuries }.joinToString { it.name }
            newMsgs += InboxMessage(
                id = "med_md${round}_${Random.nextInt(999)}",
                category = InboxCategory.MEDICAL,
                title = "Medical Room Report",
                body = "$hurtNames sustained a match knock and will sit out upcoming training & selection.",
                matchdayLabel = "MD $round",
            )
        }

        val nextMd = current.currentMatchday + 1
        var leagueChamp = current.leagueChampionClubId
        val news = mutableListOf(
            "MD $round: ${session.homeClub.shortName} ${session.homeScore}-${session.awayScore} ${session.awayClub.shortName}",
        )
        if (nextMd > current.totalLeagueRounds) {
            leagueChamp = sortedDiv.firstOrNull()?.clubId
            val champName = updatedClubs.find { it.id == leagueChamp }?.name
            val divName = userDiv?.name ?: "League"
            if (champName != null) {
                news.add(0, "$champName crowned $divName Champions!")
            }
        }

        return current.copy(
            clubs = updatedClubs,
            leagueFixtures = mergedLeague,
            standings = updatedStandings,
            currentMatchday = nextMd,
            boardConfidence = finTick.newBoardConfidence,
            inboxMessages = (newMsgs + current.inboxMessages).take(24),
            leagueChampionClubId = leagueChamp,
            trainingUsedThisWeek = false,
            lastTrainingNote = null,
            recentNews = (news + current.recentNews).take(5),
        )
    }

    private fun progressInjuriesAndSuspensions(
        clubs: List<Club>,
        userClubId: String,
        session: LiveMatchSession,
    ): List<Club> {
        return clubs.map { club ->
            if (club.id != userClubId) return@map club
            val updatedSquad = club.squad.map { p ->
                val newInjury = session.playerInjuries[p.id] ?: (p.injuredMatches - 1).coerceAtLeast(0)
                val newSusp = if (p.id in session.redCardedIds) 1 else (p.suspendedMatches - 1).coerceAtLeast(0)
                p.copy(injuredMatches = newInjury, suspendedMatches = newSusp)
            }
            val needsAutoSwap = updatedSquad.any { it.id in club.startingXiIds && (it.injuredMatches > 0 || it.suspendedMatches > 0) }
            if (needsAutoSwap) {
                val (xi, bench) = SamplePlayers.autoPickLineup(updatedSquad, club.formation)
                club.copy(squad = updatedSquad, startingXiIds = xi, benchIds = bench)
            } else {
                club.copy(squad = updatedSquad)
            }
        }
    }

    private fun quickScoreline(homeOvr: Int, awayOvr: Int): Pair<Int, Int> {
        val diff = (homeOvr - awayOvr) + 2
        val homeLambda = (1.35 + diff * 0.055).coerceIn(0.45, 3.1)
        val awayLambda = (1.15 - diff * 0.050).coerceIn(0.35, 2.8)
        return samplePoissonGoals(homeLambda) to samplePoissonGoals(awayLambda)
    }

    private fun samplePoissonGoals(lambda: Double): Int {
        val l = kotlin.math.exp(-lambda)
        var k = 0
        var p = 1.0
        do {
            k++
            p *= Random.nextDouble()
        } while (p > l && k < 7)
        return (k - 1).coerceAtLeast(0)
    }

    private fun finalizeSeasonAndBuildSummary() {
        val current = advanceContinentalMilestones(_state.value, simAllRemainingIfPostSeason = true)
        val userClub = current.clubs.find { it.id == current.userClubId } ?: return
        val userDiv = current.divisions.find { it.id == userClub.divisionId } ?: return

        var finalLeagueFixtures = current.leagueFixtures
        var finalStandings = current.standings
        val clubMap = current.clubs.associateBy { it.id }

        val remainingUnplayed = finalLeagueFixtures.filter { !it.played }
        if (remainingUnplayed.isNotEmpty()) {
            val simmed = remainingUnplayed.map { f ->
                val h = clubMap[f.homeClubId]
                val a = clubMap[f.awayClubId]
                val (hg, ag) = quickScoreline(h?.averageOverall ?: 65, a?.averageOverall ?: 65)
                f.copy(homeScore = hg, awayScore = ag, played = true)
            }
            val mapSim = simmed.associateBy { it.id }
            finalLeagueFixtures = finalLeagueFixtures.map { mapSim[it.id] ?: it }
            finalStandings = updateStandings(finalStandings, simmed)
        }

        val userSorted = SampleCompetitions.sortedStandingsForDivision(userDiv.id, finalStandings, current.clubs)
        val userPos = (userSorted.indexOfFirst { it.clubId == userClub.id } + 1).coerceAtLeast(1)

        val promotedIds = mutableListOf<String>()
        val relegatedIds = mutableListOf<String>()

        if (userDiv.tier > 1) {
            promotedIds += userSorted.take(userDiv.autoPromoteSpots).map { it.clubId }
            val poFinalWinner = current.playoffFixtures.firstOrNull { it.round == 2 && it.played }?.winnerClubId()
            if (poFinalWinner != null && poFinalWinner !in promotedIds) {
                promotedIds += poFinalWinner
            }
        }
        if (userDiv.relegateSpots > 0) {
            relegatedIds += userSorted.takeLast(userDiv.relegateSpots).map { it.clubId }
        }

        val statusText = when {
            userClub.id in promotedIds -> "PROMOTED to Tier ${userDiv.tier - 1}!"
            userClub.id in relegatedIds -> "RELEGATED to Tier ${userDiv.tier + 1}."
            userPos == 1 && userDiv.tier == 1 -> "CHAMPIONS of ${userDiv.name}!"
            userDiv.uclSpots > 0 && userPos <= userDiv.uclSpots -> "Qualified for UEFA Champions League!"
            userDiv.uelSpots > 0 && userPos <= userDiv.uclSpots + userDiv.uelSpots -> "Qualified for UEFA Europa League!"
            userDiv.ueclSpots > 0 && userPos <= userDiv.uclSpots + userDiv.uelSpots + userDiv.ueclSpots -> "Qualified for UEFA Conference League!"
            userDiv.aclSpots > 0 && userPos <= userDiv.aclSpots -> "Qualified for AFC Champions League Elite!"
            else -> "Finished #$userPos in ${userDiv.name}."
        }

        val leagueChampName = userSorted.firstOrNull()?.clubId?.let { clubMap[it]?.name } ?: userClub.name
        val domWinnerName = current.cupChampionClubId?.let { clubMap[it]?.name }
        val uclChamp = current.uclState?.championClubId?.let { clubMap[it]?.name }
        val uelChamp = current.uelState?.championClubId?.let { clubMap[it]?.name }
        val ueclChamp = current.ueclState?.championClubId?.let { clubMap[it]?.name }
        val aclChamp = current.aclState?.championClubId?.let { clubMap[it]?.name }
        val primaryContChamp = if (userClub.country.confederation.name == "AFC") aclChamp else uclChamp

        val summary = SeasonSummaryReport(
            seasonNumber = current.seasonNumber,
            userDivisionName = userDiv.name,
            userFinishPosition = userPos,
            leagueChampionName = leagueChampName,
            domesticCupWinnerName = domWinnerName,
            continentalWinnerName = primaryContChamp,
            uclWinnerName = uclChamp,
            uelWinnerName = uelChamp,
            ueclWinnerName = ueclChamp,
            aclWinnerName = aclChamp,
            promotionStatusText = statusText,
            promotedClubs = promotedIds.mapNotNull { clubMap[it]?.name },
            relegatedClubs = relegatedIds.mapNotNull { clubMap[it]?.name },
        )

        persist(
            current.copy(
                leagueFixtures = finalLeagueFixtures,
                standings = finalStandings,
                leagueChampionClubId = userSorted.firstOrNull()?.clubId,
                continentalChampionClubId = primaryContChamp?.let { name -> clubMap.values.find { it.name == name }?.id },
                seasonSummary = summary,
            ),
        )
    }

    fun startNextSeason() {
        val current = _state.value
        val nextSeason = current.seasonNumber + 1
        val clubDivisionSwap = mutableMapOf<String, Pair<String, Int>>()

        WorldDatabase.playableCountries.forEach { country ->
            val countryDivs = current.divisions.filter { it.country == country }.sortedBy { it.tier }
            for (i in 0 until countryDivs.lastIndex) {
                val upper = countryDivs[i]
                val lower = countryDivs[i + 1]

                val upperSorted = SampleCompetitions.sortedStandingsForDivision(upper.id, current.standings, current.clubs)
                val lowerSorted = SampleCompetitions.sortedStandingsForDivision(lower.id, current.standings, current.clubs)

                val goingDown = upperSorted.takeLast(upper.relegateSpots.coerceAtLeast(1)).map { it.clubId }
                val goingUp = mutableListOf<String>()

                if (lower.id == current.clubs.find { it.id == current.userClubId }?.divisionId && current.playoffFixtures.isNotEmpty()) {
                    goingUp += lowerSorted.take(lower.autoPromoteSpots).map { it.clubId }
                    val poWinner = current.playoffFixtures.firstOrNull { it.round == 2 && it.played }?.winnerClubId()
                    if (poWinner != null && poWinner !in goingUp) goingUp += poWinner
                } else {
                    val totalUp = (lower.autoPromoteSpots + if (lower.playoffSpots > 0) 1 else 0).coerceAtLeast(1)
                    goingUp += lowerSorted.take(totalUp).map { it.clubId }
                }

                val swapCount = minOf(goingDown.size, goingUp.size)
                goingDown.take(swapCount).forEach { id -> clubDivisionSwap[id] = lower.id to lower.tier }
                goingUp.take(swapCount).forEach { id -> clubDivisionSwap[id] = upper.id to upper.tier }
            }
        }

        val refreshedClubs = current.clubs.map { club ->
            val swap = clubDivisionSwap[club.id]
            val newDivId = swap?.first ?: club.divisionId
            val newTier = swap?.second ?: club.tier
            val promotedBonus = if (swap != null && swap.second < club.tier) {
                if (swap.second == 1) 18.0 else if (swap.second <= 3) 4.0 else 0.5
            } else {
                0.0
            }
            val tierIncome = when (newTier) {
                1 -> 20.0
                2 -> 5.0
                3 -> 1.5
                4 -> 0.8
                5, 6 -> 0.25
                else -> 0.05
            }
            val refreshedSquad = club.squad.map { p ->
                val growth = if (p.age <= 23 && p.overall < p.potential) 1 else if (p.age >= 34) -1 else 0
                p.copy(
                    age = p.age + 1,
                    overall = (p.overall + growth).coerceIn(40, 94),
                    fitness = 95,
                    morale = 84,
                    goals = 0,
                    assists = 0,
                    yellowCards = 0,
                    redCards = 0,
                    appearances = 0,
                    injuredMatches = 0,
                    suspendedMatches = 0,
                    contractYearsLeft = (p.contractYearsLeft - 1).coerceAtLeast(1),
                )
            }
            club.copy(
                divisionId = newDivId,
                tier = newTier,
                budgetM = ((club.budgetM + tierIncome + promotedBonus) * 100.0).roundToInt() / 100.0,
                squad = refreshedSquad,
            )
        }

        val userClub = refreshedClubs.first { it.id == current.userClubId }
        val userDivClubs = refreshedClubs.filter { it.divisionId == userClub.divisionId }
        val totalRounds = ((userDivClubs.size - 1) * 2).coerceAtLeast(10)

        val leagueFixtures = SampleCompetitions.generateAllLeagueFixtures(current.divisions, refreshedClubs)
        val cupFixtures = SampleCompetitions.generateDomesticCupRoundOf16(userClub.country, userClub.id, refreshedClubs)
        val startCupStage = cupFixtures.minOfOrNull { it.round } ?: 1

        val contBundle = ContinentalEngine.seedSeasonTournaments(
            clubs = refreshedClubs,
            divisions = current.divisions,
            standings = current.standings,
            userClubId = userClub.id,
            domesticCupWinnerId = current.cupChampionClubId,
        )
        val standings = SampleCompetitions.createInitialStandings(refreshedClubs)
        val newDivName = current.divisions.find { it.id == userClub.divisionId }?.name ?: "League"
        val contName = contBundle.userTournament?.displayName ?: userClub.country.continentalName

        val expiringCount = userClub.squad.count { it.contractYearsLeft <= 1 }
        val seasonMsgs = buildList {
            if (contBundle.userTournament != null) {
                add(
                    InboxMessage(
                        id = "s${nextSeason}_cont",
                        category = InboxCategory.CONTINENTAL,
                        title = "Qualified: ${contBundle.userTournament.displayName}",
                        body = "Based on last season's standings, ${userClub.name} enter the ${contBundle.userTournament.displayName}!",
                        matchdayLabel = "S$nextSeason",
                    ),
                )
            }
            if (expiringCount > 0) {
                add(
                    InboxMessage(
                        id = "s${nextSeason}_contracts",
                        category = InboxCategory.BOARD,
                        title = "$expiringCount Expiring Contracts",
                        body = "$expiringCount player(s) have 1 year remaining. Visit Squad -> Contracts to negotiate renewals.",
                        matchdayLabel = "S$nextSeason",
                    ),
                )
            }
        }

        persist(
            current.copy(
                seasonNumber = nextSeason,
                currentMatchday = 1,
                totalLeagueRounds = totalRounds,
                currentCupStage = startCupStage,
                currentContinentalStage = 1,
                clubs = refreshedClubs,
                youthAcademy = (CareerEventsEngine.generateYouthProspects(userClub, 2, nextSeason) + current.youthAcademy).take(8),
                inboxMessages = (seasonMsgs + current.inboxMessages).take(24),
                leagueFixtures = leagueFixtures,
                cupFixtures = cupFixtures,
                continentalFixtures = emptyList(),
                playoffFixtures = emptyList(),
                continentalCompetitionName = contName,
                uclState = contBundle.ucl,
                uelState = contBundle.uel,
                ueclState = contBundle.uecl,
                aclState = contBundle.acl,
                standings = standings,
                trainingUsedThisWeek = false,
                lastTrainingNote = null,
                cupChampionClubId = null,
                continentalChampionClubId = null,
                leagueChampionClubId = null,
                seasonSummary = null,
                recentNews = listOf("Season $nextSeason kicks off! ${userClub.name} compete in $newDivName.") + current.recentNews.take(3),
            ),
        )
    }

    fun answerPressQuestion(optionId: String) {
        val current = _state.value
        val presser = current.pendingPressConference ?: return
        if (presser.isComplete) return

        val question = presser.questions.getOrNull(presser.currentQuestionIndex) ?: return
        val chosen = question.options.find { it.id == optionId } ?: return

        val nextIdx = presser.currentQuestionIndex + 1
        val finished = nextIdx >= presser.questions.size
        val updatedPresser = presser.copy(
            currentQuestionIndex = if (finished) presser.currentQuestionIndex else nextIdx,
            totalMoraleDelta = presser.totalMoraleDelta + chosen.moraleDelta,
            totalReputationDelta = presser.totalReputationDelta + chosen.reputationDelta,
            lastHeadlineSnippet = chosen.headlineSnippet,
            isComplete = finished,
        )
        persist(current.copy(pendingPressConference = updatedPresser))
    }

    fun completePressConference(): String? {
        val current = _state.value
        val presser = current.pendingPressConference ?: return null

        val moraleShift = presser.totalMoraleDelta
        val repShift = presser.totalReputationDelta

        val updatedClubs = current.clubs.map { club ->
            if (club.id != current.userClubId) return@map club
            club.copy(
                reputation = (club.reputation + repShift).coerceIn(10, 99),
                squad = club.squad.map { p ->
                    p.copy(morale = (p.morale + moraleShift).coerceIn(35, 100))
                },
            )
        }

        val headline = presser.lastHeadlineSnippet ?: "Manager wraps up post-match press conference."
        val wireEntry = "PRESS: $headline"
        persist(
            current.copy(
                clubs = updatedClubs,
                pendingPressConference = null,
                recentNews = (listOf(wireEntry) + current.recentNews).take(5),
            ),
        )
        val morSign = if (moraleShift >= 0) "+" else ""
        val repSign = if (repShift >= 0) "+" else ""
        return "Press Conference: Morale $morSign$moraleShift, Reputation $repSign$repShift"
    }

    fun skipPressConference(): String {
        val current = _state.value
        val updatedClubs = current.clubs.map { club ->
            if (club.id != current.userClubId) return@map club
            club.copy(reputation = (club.reputation - 2).coerceIn(10, 99))
        }
        val wireEntry = "PRESS: Manager declined post-match media duties (-2 Rep)."
        persist(
            current.copy(
                clubs = updatedClubs,
                pendingPressConference = null,
                recentNews = (listOf(wireEntry) + current.recentNews).take(5),
            ),
        )
        return "Skipped media duties (-2 Reputation)"
    }

    private fun applySessionStatsToClubs(
        clubs: List<Club>,
        session: LiveMatchSession,
        updateFitness: Boolean,
    ): List<Club> {
        val homeWin = session.homeScore > session.awayScore ||
            (session.homePenalties ?: 0) > (session.awayPenalties ?: 0)
        val awayWin = session.awayScore > session.homeScore ||
            (session.awayPenalties ?: 0) > (session.homePenalties ?: 0)

        return clubs.map { club ->
            if (club.id != session.homeClub.id && club.id != session.awayClub.id) {
                return@map club
            }
            val isHome = club.id == session.homeClub.id
            val staminaMap = if (isHome) session.homeStamina else session.awayStamina
            val moraleDelta = when {
                (isHome && homeWin) || (!isHome && awayWin) -> +5
                session.homeScore == session.awayScore -> +1
                else -> -4
            }
            val xiIds = if (isHome) session.homeClub.startingXiIds else session.awayClub.startingXiIds
            val activeIds = xiIds.toSet() + session.subbedOutIds

            val updatedSquad = club.squad.map { p ->
                val played = p.id in activeIds
                val newFitness = if (updateFitness && p.id in staminaMap) {
                    (staminaMap[p.id] ?: p.fitness).coerceIn(40, 100)
                } else {
                    p.fitness
                }
                p.copy(
                    goals = p.goals + (session.playerGoals[p.id] ?: 0),
                    assists = p.assists + (session.playerAssists[p.id] ?: 0),
                    yellowCards = p.yellowCards + (session.playerYellows[p.id] ?: 0),
                    redCards = p.redCards + (session.playerReds[p.id] ?: 0),
                    appearances = p.appearances + if (played) 1 else 0,
                    fitness = newFitness,
                    morale = (p.morale + moraleDelta).coerceIn(35, 100),
                )
            }
            club.copy(squad = updatedSquad)
        }
    }

    private fun recoverSquadFitness(clubs: List<Club>): List<Club> {
        return clubs.map { club ->
            club.copy(
                squad = club.squad.map { p ->
                    p.copy(fitness = (p.fitness + 12).coerceAtMost(98))
                },
            )
        }
    }

    private fun updateStandings(
        current: List<LeagueStanding>,
        fixtures: List<MatchFixture>,
    ): List<LeagueStanding> {
        val map = current.associateBy { it.clubId }.toMutableMap()
        fixtures.forEach { f ->
            val h = map[f.homeClubId] ?: LeagueStanding(f.homeClubId, f.divisionId)
            val a = map[f.awayClubId] ?: LeagueStanding(f.awayClubId, f.divisionId)

            val hPts: Int
            val aPts: Int
            val hRes: String
            val aRes: String
            when {
                f.homeScore > f.awayScore -> {
                    hPts = 3; aPts = 0; hRes = "W"; aRes = "L"
                }
                f.awayScore > f.homeScore -> {
                    hPts = 0; aPts = 3; hRes = "L"; aRes = "W"
                }
                else -> {
                    hPts = 1; aPts = 1; hRes = "D"; aRes = "D"
                }
            }

            map[f.homeClubId] = h.copy(
                played = h.played + 1,
                won = h.won + if (hRes == "W") 1 else 0,
                drawn = h.drawn + if (hRes == "D") 1 else 0,
                lost = h.lost + if (hRes == "L") 1 else 0,
                goalsFor = h.goalsFor + f.homeScore,
                goalsAgainst = h.goalsAgainst + f.awayScore,
                points = h.points + hPts,
                form = (h.form + hRes).takeLast(5),
            )

            map[f.awayClubId] = a.copy(
                played = a.played + 1,
                won = a.won + if (aRes == "W") 1 else 0,
                drawn = a.drawn + if (aRes == "D") 1 else 0,
                lost = a.lost + if (aRes == "L") 1 else 0,
                goalsFor = a.goalsFor + f.awayScore,
                goalsAgainst = a.goalsAgainst + f.homeScore,
                points = a.points + aPts,
                form = (a.form + aRes).takeLast(5),
            )
        }
        return map.values.toList()
    }
}
