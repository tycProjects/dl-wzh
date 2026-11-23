package com.example.engine

import com.example.model.Club
import com.example.model.MatchEvent
import com.example.model.MatchEventType
import com.example.model.MatchFixture
import com.example.model.MatchMentality
import com.example.model.MatchPressing
import com.example.model.MatchTeamStats
import com.example.model.Player
import com.example.model.Position
import kotlin.math.roundToInt
import kotlin.random.Random

data class LiveMatchSession(
    val fixture: MatchFixture,
    val homeClub: Club,
    val awayClub: Club,
    val userIsHome: Boolean,
    val minute: Int = 0,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val homeStats: MatchTeamStats = MatchTeamStats(),
    val awayStats: MatchTeamStats = MatchTeamStats(),
    val events: List<MatchEvent> = listOf(
        MatchEvent(
            minute = 0,
            type = MatchEventType.KICKOFF,
            clubId = null,
            commentary = "Kick-off! Both sides are locked in for 90 minutes.",
        ),
    ),
    val homeStamina: Map<String, Int> = homeClub.startingXiPlayers().associate { it.id to it.fitness },
    val awayStamina: Map<String, Int> = awayClub.startingXiPlayers().associate { it.id to it.fitness },
    val yellowCardedIds: Set<String> = emptySet(),
    val redCardedIds: Set<String> = emptySet(),
    val subbedOutIds: Set<String> = emptySet(),
    val subsUsedByUser: Int = 0,
    val subsUsedByAi: Int = 0,
    val playerGoals: Map<String, Int> = emptyMap(),
    val playerAssists: Map<String, Int> = emptyMap(),
    val playerYellows: Map<String, Int> = emptyMap(),
    val playerReds: Map<String, Int> = emptyMap(),
    val playerInjuries: Map<String, Int> = emptyMap(), // playerId -> matches out
    val playerRatings: Map<String, Double> = (homeClub.startingXiPlayers() + awayClub.startingXiPlayers())
        .associate { it.id to 6.4 },
    val userMentality: MatchMentality = MatchMentality.BALANCED,
    val userPressing: MatchPressing = MatchPressing.MID_PRESS,
    val homePenalties: Int? = null,
    val awayPenalties: Int? = null,
    val isPaused: Boolean = false,
    val speedMultiplier: Int = 2,
    val isFinished: Boolean = false,
    val manOfTheMatch: String? = null,
) {
    val userClub: Club get() = if (userIsHome) homeClub else awayClub
    val opponentClub: Club get() = if (userIsHome) awayClub else homeClub
}

object MatchEngine {

    private data class TeamStrength(
        val attack: Double,
        val midfield: Double,
        val defense: Double,
        val goalkeeper: Double,
    )

    fun createSession(
        fixture: MatchFixture,
        homeClub: Club,
        awayClub: Club,
        userClubId: String,
    ): LiveMatchSession {
        return LiveMatchSession(
            fixture = fixture,
            homeClub = homeClub,
            awayClub = awayClub,
            userIsHome = fixture.homeClubId == userClubId,
        )
    }

    fun setUserTactics(
        session: LiveMatchSession,
        mentality: MatchMentality = session.userMentality,
        pressing: MatchPressing = session.userPressing,
    ): LiveMatchSession {
        if (session.isFinished) return session
        return session.copy(userMentality = mentality, userPressing = pressing)
    }

    /**
     * Performs a live substitution for the user's club during a match.
     */
    fun substituteUserPlayer(
        session: LiveMatchSession,
        outgoingPlayerId: String,
        incomingPlayerId: String,
    ): LiveMatchSession {
        if (session.isFinished || session.subsUsedByUser >= 5) return session
        if (outgoingPlayerId in session.redCardedIds) return session

        val club = session.userClub
        val xi = club.startingXiIds.toMutableList()
        val bench = club.benchIds.toMutableList()

        val xiIndex = xi.indexOf(outgoingPlayerId)
        val benchIndex = bench.indexOf(incomingPlayerId)
        if (xiIndex == -1 || benchIndex == -1) return session

        val outPlayer = club.squad.find { it.id == outgoingPlayerId } ?: return session
        val inPlayer = club.squad.find { it.id == incomingPlayerId } ?: return session

        xi[xiIndex] = incomingPlayerId
        bench[benchIndex] = outgoingPlayerId

        val updatedClub = club.copy(startingXiIds = xi, benchIds = bench)
        val subEvent = MatchEvent(
            minute = session.minute,
            type = MatchEventType.SUBSTITUTION,
            clubId = club.id,
            primaryPlayerName = inPlayer.name,
            secondaryPlayerName = outPlayer.name,
            commentary = "SUB (${club.shortName}): ${inPlayer.name} replaces ${outPlayer.name}.",
        )
        val updatedRatings = session.playerRatings + (inPlayer.id to (session.playerRatings[inPlayer.id] ?: 6.3))

        return if (session.userIsHome) {
            session.copy(
                homeClub = updatedClub,
                homeStamina = session.homeStamina + (inPlayer.id to inPlayer.fitness),
                playerRatings = updatedRatings,
                subbedOutIds = session.subbedOutIds + outgoingPlayerId,
                subsUsedByUser = session.subsUsedByUser + 1,
                events = session.events + subEvent,
            )
        } else {
            session.copy(
                awayClub = updatedClub,
                awayStamina = session.awayStamina + (inPlayer.id to inPlayer.fitness),
                playerRatings = updatedRatings,
                subbedOutIds = session.subbedOutIds + outgoingPlayerId,
                subsUsedByUser = session.subsUsedByUser + 1,
                events = session.events + subEvent,
            )
        }
    }

    fun stepMinute(
        session: LiveMatchSession,
        random: Random = Random.Default,
    ): LiveMatchSession {
        if (session.isFinished) return session

        val nextMinute = session.minute + 1
        var homeClub = session.homeClub
        var awayClub = session.awayClub
        var homeScore = session.homeScore
        var awayScore = session.awayScore
        var homeStats = session.homeStats
        var awayStats = session.awayStats
        val events = session.events.toMutableList()
        val homeStamina = session.homeStamina.toMutableMap()
        val awayStamina = session.awayStamina.toMutableMap()
        val yellows = session.yellowCardedIds.toMutableSet()
        val reds = session.redCardedIds.toMutableSet()
        val goalsMap = session.playerGoals.toMutableMap()
        val assistsMap = session.playerAssists.toMutableMap()
        val yellowCountMap = session.playerYellows.toMutableMap()
        val redCountMap = session.playerReds.toMutableMap()
        val injuryMap = session.playerInjuries.toMutableMap()
        val ratingsMap = session.playerRatings.toMutableMap()
        var subsByAi = session.subsUsedByAi
        val subbedOut = session.subbedOutIds.toMutableSet()

        fun bumpRating(playerId: String, delta: Double) {
            val cur = ratingsMap[playerId] ?: 6.4
            val next = ((cur + delta) * 10.0).roundToInt() / 10.0
            ratingsMap[playerId] = next.coerceIn(4.2, 10.0)
        }

        // Drain stamina slightly every 3 minutes (extra drain for Gegenpress)
        if (nextMinute % 3 == 0) {
            val homeDrain = 1 + if (session.userIsHome) session.userPressing.extraStaminaDrain else 0
            val awayDrain = 1 + if (!session.userIsHome && session.fixture.homeClubId != session.fixture.awayClubId && session.userClub.id == awayClub.id) {
                session.userPressing.extraStaminaDrain
            } else {
                0
            }
            homeClub.startingXiIds.forEach { id ->
                if (id !in reds) {
                    homeStamina[id] = ((homeStamina[id] ?: 80) - homeDrain).coerceAtLeast(32)
                }
            }
            awayClub.startingXiIds.forEach { id ->
                if (id !in reds) {
                    awayStamina[id] = ((awayStamina[id] ?: 80) - awayDrain).coerceAtLeast(32)
                }
            }
        }

        // Halftime marker
        if (nextMinute == 45) {
            events += MatchEvent(
                minute = 45,
                type = MatchEventType.HALFTIME,
                clubId = null,
                commentary = "Half-time whistle: ${homeClub.shortName} $homeScore - $awayScore ${awayClub.shortName}.",
            )
        }

        // Occasional AI substitution in second half
        if (nextMinute in 58..82 && subsByAi < 3 && random.nextDouble() < 0.08) {
            val aiClub = if (session.userIsHome) awayClub else homeClub
            val aiStaminaMap = if (session.userIsHome) awayStamina else homeStamina
            val tiredStarter = aiClub.startingXiPlayers()
                .filter { it.id !in reds && it.position != Position.GK }
                .minByOrNull { aiStaminaMap[it.id] ?: 80 }
            val freshBench = aiClub.benchPlayers()
                .filter { it.id !in subbedOut && it.position == tiredStarter?.position }
                .maxByOrNull { it.overall }
                ?: aiClub.benchPlayers().firstOrNull { it.id !in subbedOut && it.position != Position.GK }

            if (tiredStarter != null && freshBench != null) {
                val xi = aiClub.startingXiIds.toMutableList()
                val bench = aiClub.benchIds.toMutableList()
                val xiIdx = xi.indexOf(tiredStarter.id)
                val bIdx = bench.indexOf(freshBench.id)
                if (xiIdx != -1 && bIdx != -1) {
                    xi[xiIdx] = freshBench.id
                    bench[bIdx] = tiredStarter.id
                    val updatedAi = aiClub.copy(startingXiIds = xi, benchIds = bench)
                    if (session.userIsHome) {
                        awayClub = updatedAi
                        awayStamina[freshBench.id] = freshBench.fitness
                    } else {
                        homeClub = updatedAi
                        homeStamina[freshBench.id] = freshBench.fitness
                    }
                    ratingsMap[freshBench.id] = 6.3
                    subbedOut += tiredStarter.id
                    subsByAi++
                    events += MatchEvent(
                        minute = nextMinute,
                        type = MatchEventType.SUBSTITUTION,
                        clubId = aiClub.id,
                        primaryPlayerName = freshBench.name,
                        secondaryPlayerName = tiredStarter.name,
                        commentary = "SUB (${aiClub.shortName}): ${freshBench.name} on for ${tiredStarter.name}.",
                    )
                }
            }
        }

        val homeMentality = if (session.userIsHome) session.userMentality else MatchMentality.BALANCED
        val homePress = if (session.userIsHome) session.userPressing else MatchPressing.MID_PRESS
        val awayMentality = if (!session.userIsHome && session.userClub.id == awayClub.id) session.userMentality else MatchMentality.BALANCED
        val awayPress = if (!session.userIsHome && session.userClub.id == awayClub.id) session.userPressing else MatchPressing.MID_PRESS

        val homeStr = calculateStrength(homeClub, homeStamina, reds, isHome = true, homeMentality, homePress)
        val awayStr = calculateStrength(awayClub, awayStamina, reds, isHome = false, awayMentality, awayPress)

        // Smooth live possession calculation
        val midTotal = (homeStr.midfield + awayStr.midfield).coerceAtLeast(1.0)
        val targetHomePoss = ((homeStr.midfield / midTotal) * 100.0).roundToInt().coerceIn(32, 68)
        val homePoss = ((homeStats.possession * 0.85) + (targetHomePoss * 0.15)).roundToInt().coerceIn(30, 70)
        homeStats = homeStats.copy(possession = homePoss)
        awayStats = awayStats.copy(possession = 100 - homePoss)

        // Chance generation (~17.5% chance of a highlight per minute)
        if (random.nextDouble() < 0.175) {
            val homeAttackShare = homeStr.attack / (homeStr.attack + awayStr.attack).coerceAtLeast(1.0)
            val isHomeAttacking = random.nextDouble() < homeAttackShare
            val attClub = if (isHomeAttacking) homeClub else awayClub
            val defClub = if (isHomeAttacking) awayClub else homeClub
            val attStr = if (isHomeAttacking) homeStr else awayStr
            val defStr = if (isHomeAttacking) awayStr else homeStr

            val activeAttackers = attClub.startingXiPlayers().filter { it.id !in reds && it.position != Position.GK }
            val shooter = pickWeightedShooter(activeAttackers, random)
            val assister = activeAttackers.filter { it.id != shooter?.id }.maxByOrNull {
                it.passing + random.nextInt(18)
            }
            val gk = defClub.startingXiPlayers().firstOrNull { it.position == Position.GK && it.id !in reds }
                ?: defClub.startingXiPlayers().firstOrNull()

            if (shooter != null) {
                val xgAdded = (0.06 + random.nextDouble() * 0.24).let { (it * 100.0).roundToInt() / 100.0 }
                val qualityEdge = (attStr.attack - defStr.defense) * 0.008
                val gkFactor = ((gk?.overall ?: 75) - 75) * 0.004
                val onTargetProb = (0.48 + qualityEdge).coerceIn(0.28, 0.72)
                val isOnTarget = random.nextDouble() < onTargetProb

                val cornerEarned = random.nextDouble() < 0.30
                if (isHomeAttacking) {
                    homeStats = homeStats.copy(
                        shots = homeStats.shots + 1,
                        shotsOnTarget = homeStats.shotsOnTarget + if (isOnTarget) 1 else 0,
                        expectedGoals = ((homeStats.expectedGoals + xgAdded) * 100.0).roundToInt() / 100.0,
                        corners = homeStats.corners + if (cornerEarned) 1 else 0,
                    )
                } else {
                    awayStats = awayStats.copy(
                        shots = awayStats.shots + 1,
                        shotsOnTarget = awayStats.shotsOnTarget + if (isOnTarget) 1 else 0,
                        expectedGoals = ((awayStats.expectedGoals + xgAdded) * 100.0).roundToInt() / 100.0,
                        corners = awayStats.corners + if (cornerEarned) 1 else 0,
                    )
                }

                if (isOnTarget) {
                    val goalProb = (0.31 + qualityEdge - gkFactor).coerceIn(0.16, 0.52)
                    if (random.nextDouble() < goalProb) {
                        if (isHomeAttacking) homeScore++ else awayScore++
                        goalsMap[shooter.id] = (goalsMap[shooter.id] ?: 0) + 1
                        bumpRating(shooter.id, +0.9)
                        if (assister != null && random.nextDouble() < 0.78) {
                            assistsMap[assister.id] = (assistsMap[assister.id] ?: 0) + 1
                            bumpRating(assister.id, +0.5)
                        }
                        if (gk != null) bumpRating(gk.id, -0.25)

                        val assistText = if (assister != null) " (Assist: ${assister.name})" else ""
                        events += MatchEvent(
                            minute = nextMinute,
                            type = MatchEventType.GOAL,
                            clubId = attClub.id,
                            primaryPlayerName = shooter.name,
                            secondaryPlayerName = assister?.name,
                            commentary = "GOAL! ${shooter.name} fires home for ${attClub.shortName}!$assistText",
                        )
                    } else {
                        if (gk != null) bumpRating(gk.id, +0.35)
                        events += MatchEvent(
                            minute = nextMinute,
                            type = MatchEventType.SAVE,
                            clubId = defClub.id,
                            primaryPlayerName = gk?.name ?: "Keeper",
                            secondaryPlayerName = shooter.name,
                            commentary = "Big save by ${gk?.name ?: "the keeper"} to deny ${shooter.name}!",
                        )
                    }
                } else {
                    events += MatchEvent(
                        minute = nextMinute,
                        type = MatchEventType.CHANCE,
                        clubId = attClub.id,
                        primaryPlayerName = shooter.name,
                        commentary = "Close! ${shooter.name} (${attClub.shortName}) lashes an effort just wide.",
                    )
                }
            }
        } else if (random.nextDouble() < 0.045) {
            // Foul, card & rare minor injury check
            val isHomeFoul = random.nextBoolean()
            val foulClub = if (isHomeFoul) homeClub else awayClub
            if (isHomeFoul) {
                homeStats = homeStats.copy(fouls = homeStats.fouls + 1)
            } else {
                awayStats = awayStats.copy(fouls = awayStats.fouls + 1)
            }

            if (random.nextDouble() < 0.42) {
                val offender = foulClub.startingXiPlayers()
                    .filter { it.id !in reds }
                    .randomOrNull(random)
                if (offender != null) {
                    if (offender.id in yellows || random.nextDouble() < 0.06) {
                        reds += offender.id
                        redCountMap[offender.id] = (redCountMap[offender.id] ?: 0) + 1
                        bumpRating(offender.id, -1.4)
                        events += MatchEvent(
                            minute = nextMinute,
                            type = MatchEventType.RED_CARD,
                            clubId = foulClub.id,
                            primaryPlayerName = offender.name,
                            commentary = "RED CARD! ${offender.name} (${foulClub.shortName}) is sent off!",
                        )
                    } else {
                        yellows += offender.id
                        yellowCountMap[offender.id] = (yellowCountMap[offender.id] ?: 0) + 1
                        bumpRating(offender.id, -0.3)
                        events += MatchEvent(
                            minute = nextMinute,
                            type = MatchEventType.YELLOW_CARD,
                            clubId = foulClub.id,
                            primaryPlayerName = offender.name,
                            commentary = "Yellow card shown to ${offender.name} (${foulClub.shortName}) for a late challenge.",
                        )
                    }
                }
            } else if (random.nextDouble() < 0.08) {
                // Rare minor injury (1-2 matches out)
                val targetClub = if (isHomeFoul) awayClub else homeClub
                val hurtPlayer = targetClub.startingXiPlayers()
                    .filter { it.id !in reds && it.position != Position.GK && it.id !in injuryMap }
                    .randomOrNull(random)
                if (hurtPlayer != null) {
                    val outMatches = 1 + random.nextInt(2)
                    injuryMap[hurtPlayer.id] = outMatches
                    events += MatchEvent(
                        minute = nextMinute,
                        type = MatchEventType.INJURY,
                        clubId = targetClub.id,
                        primaryPlayerName = hurtPlayer.name,
                        commentary = "INJURY: ${hurtPlayer.name} (${targetClub.shortName}) picks up a knock ($outMatches match recovery).",
                    )
                }
            }
        }

        // Subtle rating reward for clean minutes on the pitch
        if (nextMinute % 15 == 0) {
            (homeClub.startingXiIds + awayClub.startingXiIds).forEach { id ->
                if (id !in reds) bumpRating(id, +0.1)
            }
        }

        // Full-time check at 90'
        var finished = false
        var homePens = session.homePenalties
        var awayPens = session.awayPenalties
        var motm: String? = session.manOfTheMatch

        if (nextMinute >= 90) {
            finished = true
            if (session.fixture.isCup && homeScore == awayScore) {
                val edgeHome = homeStr.goalkeeper + homeStr.attack > awayStr.goalkeeper + awayStr.attack
                var hp = 4 + random.nextInt(2)
                var ap = 4 + random.nextInt(2)
                if (hp == ap) {
                    if (edgeHome && random.nextDouble() < 0.65) hp++ else ap++
                }
                homePens = hp
                awayPens = ap
                val winnerName = if (hp > ap) homeClub.shortName else awayClub.shortName
                events += MatchEvent(
                    minute = 90,
                    type = MatchEventType.PENALTY_SHOOTOUT,
                    clubId = if (hp > ap) homeClub.id else awayClub.id,
                    commentary = "Penalties! $winnerName win the shootout ($hp - $ap) to advance!",
                )
            }

            val allStarters = (homeClub.startingXiPlayers() + awayClub.startingXiPlayers())
            val bestRatedId = ratingsMap.maxByOrNull { it.value }?.key
            motm = allStarters.find { it.id == bestRatedId }?.name
                ?: allStarters.find { it.id == goalsMap.maxByOrNull { g -> g.value }?.key }?.name
                ?: allStarters.maxByOrNull { it.overall }?.name
                ?: homeClub.name

            events += MatchEvent(
                minute = 90,
                type = MatchEventType.FULLTIME,
                clubId = null,
                commentary = "Full-time: ${homeClub.name} $homeScore - $awayScore ${awayClub.name}. MOTM: $motm",
            )
        }

        return session.copy(
            homeClub = homeClub,
            awayClub = awayClub,
            minute = nextMinute,
            homeScore = homeScore,
            awayScore = awayScore,
            homeStats = homeStats,
            awayStats = awayStats,
            events = events,
            homeStamina = homeStamina,
            awayStamina = awayStamina,
            yellowCardedIds = yellows,
            redCardedIds = reds,
            subbedOutIds = subbedOut,
            subsUsedByAi = subsByAi,
            playerGoals = goalsMap,
            playerAssists = assistsMap,
            playerYellows = yellowCountMap,
            playerReds = redCountMap,
            playerInjuries = injuryMap,
            playerRatings = ratingsMap,
            homePenalties = homePens,
            awayPenalties = awayPens,
            isFinished = finished,
            manOfTheMatch = motm,
        )
    }

    fun simulateRestOfMatch(
        session: LiveMatchSession,
        random: Random = Random.Default,
    ): LiveMatchSession {
        var current = session
        while (!current.isFinished) {
            current = stepMinute(current, random)
        }
        return current
    }

    fun simulateAiMatch(
        fixture: MatchFixture,
        homeClub: Club,
        awayClub: Club,
        random: Random = Random.Default,
    ): LiveMatchSession {
        val initial = createSession(fixture, homeClub, awayClub, userClubId = "")
        return simulateRestOfMatch(initial, random)
    }

    private fun calculateStrength(
        club: Club,
        stamina: Map<String, Int>,
        reds: Set<String>,
        isHome: Boolean,
        mentality: MatchMentality,
        pressing: MatchPressing,
    ): TeamStrength {
        val slots = club.formation.slots
        val xi = club.startingXiPlayers()

        var attSum = 0.0
        var attCount = 0
        var midSum = 0.0
        var midCount = 0
        var defSum = 0.0
        var defCount = 0
        var gkRating = 72.0

        xi.forEachIndexed { idx, player ->
            if (player.id in reds) return@forEachIndexed
            val slotPos = slots.getOrNull(idx)?.position ?: player.position
            val fitBonus = if (player.position == slotPos) 1.0 else 0.88
            val stam = (stamina[player.id] ?: player.fitness).coerceIn(30, 100)
            val staminaFactor = 0.82 + (stam / 100.0) * 0.18
            val moraleFactor = 0.92 + (player.morale / 100.0) * 0.10
            val effective = player.overall * fitBonus * staminaFactor * moraleFactor

            when (slotPos) {
                Position.GK -> gkRating = effective
                Position.DEF -> {
                    defSum += effective
                    defCount++
                }
                Position.MID -> {
                    midSum += effective
                    midCount++
                }
                Position.FWD -> {
                    attSum += effective
                    attCount++
                }
            }
        }

        val homeBoost = if (isHome) 2.2 else 0.0
        val att = (if (attCount > 0) attSum / attCount else 72.0) + homeBoost + mentality.attackFactor
        val mid = (if (midCount > 0) midSum / midCount else 72.0) + homeBoost + pressing.midfieldFactor
        val def = (if (defCount > 0) defSum / defCount else 72.0) + homeBoost + mentality.defenseFactor

        return TeamStrength(
            attack = att.coerceAtLeast(35.0),
            midfield = mid.coerceAtLeast(35.0),
            defense = def.coerceAtLeast(35.0),
            goalkeeper = gkRating,
        )
    }

    private fun pickWeightedShooter(players: List<Player>, random: Random): Player? {
        if (players.isEmpty()) return null
        val weights = players.map { p ->
            when (p.position) {
                Position.FWD -> 45 + (p.shooting / 3)
                Position.MID -> 22 + (p.shooting / 5)
                Position.DEF -> 6
                Position.GK -> 0
            }
        }
        val total = weights.sum().coerceAtLeast(1)
        var roll = random.nextInt(total)
        players.forEachIndexed { idx, p ->
            roll -= weights[idx]
            if (roll < 0) return p
        }
        return players.first()
    }
}
