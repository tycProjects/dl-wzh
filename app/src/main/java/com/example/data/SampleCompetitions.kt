package com.example.data

import com.example.data.world.WorldDatabase
import com.example.model.Club
import com.example.model.CountryCode
import com.example.model.DivisionInfo
import com.example.model.LeagueStanding
import com.example.model.MatchFixture

/**
 * Generates multi-division league schedules, domestic cups (FA Cup, King's Cup, Copa del Rey, etc.),
 * and promotion play-off brackets. Continental tournaments (UCL, UEL, UECL, ACL Elite) are handled
 * by [com.example.engine.ContinentalEngine].
 */
object SampleCompetitions {

    fun createInitialStandings(clubs: List<Club>): List<LeagueStanding> {
        return clubs
            .filter { it.divisionId !in WorldDatabase.poolDivisionIds }
            .map { LeagueStanding(clubId = it.id, divisionId = it.divisionId) }
    }

    fun generateAllLeagueFixtures(
        divisions: List<DivisionInfo>,
        clubs: List<Club>,
    ): List<MatchFixture> {
        val all = mutableListOf<MatchFixture>()
        divisions.forEach { div ->
            val divClubs = clubs.filter { it.divisionId == div.id }
            all += generateDivisionFixtures(div.id, divClubs)
        }
        return all
    }

    fun generateDivisionFixtures(divisionId: String, clubs: List<Club>): List<MatchFixture> {
        val ids = clubs.map { it.id }.shuffled()
        val n = ids.size
        if (n < 2) return emptyList()

        val rotation = ids.toMutableList()
        val firstHalf = mutableListOf<MatchFixture>()
        val rounds = n - 1

        for (round in 1..rounds) {
            for (i in 0 until n / 2) {
                val t1 = rotation[i]
                val t2 = rotation[n - 1 - i]
                val isEvenRound = round % 2 == 0
                val home = if (i == 0 && isEvenRound) t2 else if (i % 2 == 1) t2 else t1
                val away = if (home == t1) t2 else t1
                firstHalf += MatchFixture(
                    id = "lg_${divisionId}_r${round}_m${i + 1}",
                    round = round,
                    divisionId = divisionId,
                    isCup = false,
                    homeClubId = home,
                    awayClubId = away,
                )
            }
            val last = rotation.removeAt(rotation.lastIndex)
            rotation.add(1, last)
        }

        val secondHalf = firstHalf.mapIndexed { idx, f ->
            val round = f.round + rounds
            MatchFixture(
                id = "lg_${divisionId}_r${round}_m${(idx % (n / 2)) + 1}",
                round = round,
                divisionId = divisionId,
                isCup = false,
                homeClubId = f.awayClubId,
                awayClubId = f.homeClubId,
            )
        }

        return firstHalf + secondHalf
    }

    /**
     * Generates a Domestic Cup Round of 16 (or 8-team Quarter-Final if country has 8 clubs)
     * across the user's country pyramid, guaranteeing the user's club is drawn into the bracket.
     */
    fun generateDomesticCupRoundOf16(
        country: CountryCode,
        userClubId: String,
        clubs: List<Club>,
    ): List<MatchFixture> {
        val countryClubs = clubs.filter { it.country == country && it.divisionId !in WorldDatabase.poolDivisionIds }
        val userClub = countryClubs.find { it.id == userClubId }
        val others = countryClubs.filter { it.id != userClubId }
            .sortedWith(compareBy<Club> { it.tier }.thenByDescending { it.prestige })

        val targetSize = if (countryClubs.size >= 16) 16 else 8
        val topSeeds = others.filter { it.tier == 1 }.take(6)
        val midSeeds = others.filter { it.tier in 2..4 }.shuffled().take(5)
        val cinderella = others.filter { it.tier >= 5 }.shuffled().take(4)
        val pool = (listOfNotNull(userClub) + topSeeds + midSeeds + cinderella + others)
            .distinctBy { it.id }
            .take(targetSize)
            .shuffled()

        val roundNum = if (pool.size <= 8) 2 else 1
        val stageName = if (roundNum == 2) "Quarter-Final" else "Round of 16"
        return (0 until pool.size / 2).map { i ->
            MatchFixture(
                id = "cup_r${roundNum}_${i + 1}",
                round = roundNum,
                isCup = true,
                cupStageName = stageName,
                homeClubId = pool[i * 2].id,
                awayClubId = pool[i * 2 + 1].id,
            )
        }
    }

    fun generateNextKnockoutRound(
        completedStage: Int,
        existingFixtures: List<MatchFixture>,
        isContinental: Boolean,
        isSaudi: Boolean,
    ): List<MatchFixture> {
        val winners = existingFixtures
            .filter { it.round == completedStage && it.played }
            .mapNotNull { it.winnerClubId() }
        if (winners.size < 2) return emptyList()

        val nextStage = completedStage + 1
        val stageName = if (isContinental) {
            val prefixLabel = if (isSaudi) "ACL" else "UCL"
            when (nextStage) {
                2 -> "$prefixLabel Semi-Final"
                3 -> "$prefixLabel Final"
                else -> prefixLabel
            }
        } else {
            when (nextStage) {
                2 -> "Quarter-Final"
                3 -> "Semi-Final"
                4 -> "Cup Final"
                else -> "Cup"
            }
        }
        val idPrefix = if (isContinental) "cont_r$nextStage" else "cup_r$nextStage"

        return (0 until winners.size / 2).map { i ->
            MatchFixture(
                id = "${idPrefix}_${i + 1}",
                round = nextStage,
                isCup = true,
                isContinental = isContinental,
                cupStageName = stageName,
                homeClubId = winners[i * 2],
                awayClubId = winners[i * 2 + 1],
            )
        }
    }

    fun generatePlayoffSemisOrFinal(
        division: DivisionInfo,
        sortedStandings: List<LeagueStanding>,
    ): List<MatchFixture> {
        if (division.playoffSpots <= 0) return emptyList()
        val startIdx = division.autoPromoteSpots
        val playoffTeams = sortedStandings.drop(startIdx).take(division.playoffSpots)
        if (playoffTeams.size < 2) return emptyList()

        return if (playoffTeams.size >= 4) {
            listOf(
                MatchFixture(
                    id = "po_${division.id}_sf1",
                    round = 1,
                    divisionId = division.id,
                    isCup = true,
                    isPlayoff = true,
                    cupStageName = "Play-off Semi-Final",
                    homeClubId = playoffTeams[0].clubId,
                    awayClubId = playoffTeams[3].clubId,
                ),
                MatchFixture(
                    id = "po_${division.id}_sf2",
                    round = 1,
                    divisionId = division.id,
                    isCup = true,
                    isPlayoff = true,
                    cupStageName = "Play-off Semi-Final",
                    homeClubId = playoffTeams[1].clubId,
                    awayClubId = playoffTeams[2].clubId,
                ),
            )
        } else {
            listOf(
                MatchFixture(
                    id = "po_${division.id}_final",
                    round = 2,
                    divisionId = division.id,
                    isCup = true,
                    isPlayoff = true,
                    cupStageName = "Promotion Play-off Final",
                    homeClubId = playoffTeams[0].clubId,
                    awayClubId = playoffTeams[1].clubId,
                ),
            )
        }
    }

    fun sortedStandingsForDivision(
        divisionId: String,
        standings: List<LeagueStanding>,
        clubs: List<Club>,
    ): List<LeagueStanding> {
        val clubMap = clubs.associateBy { it.id }
        return standings
            .filter { it.divisionId == divisionId }
            .sortedWith(
                compareByDescending<LeagueStanding> { it.points }
                    .thenByDescending { it.goalDifference }
                    .thenByDescending { it.goalsFor }
                    .thenBy { clubMap[it.clubId]?.name ?: "" },
            )
    }
}
