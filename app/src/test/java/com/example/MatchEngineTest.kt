package com.example

import com.example.data.SampleClubs
import com.example.data.SampleCompetitions
import com.example.data.badges.ClubSvgBadges
import com.example.data.world.WorldDatabase
import com.example.engine.CareerEventsEngine
import com.example.engine.ContinentalEngine
import com.example.engine.MatchEngine
import com.example.engine.PressConferenceGenerator
import com.example.model.CareerSaveState
import com.example.model.CountryCode
import com.example.model.MatchFixture
import com.example.model.MatchMentality
import com.example.model.MatchPressing
import com.example.ui.components.SvgCrestParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MatchEngineTest {

    @Test
    fun saudiClubsCustomSvgCrestsParseCleanly() {
        val expectedIds = listOf(
            "al-hilal", "al-nassr", "al-ittihad", "al-ahli",
            "al-shabab", "al-ettifaq", "al-taawoun", "al-fateh",
            "al-fayha", "al-khaleej", "al-kholood", "al-riyadh",
            "al-hazem", "abha", "al-faisaly-fc", "neom",
        )
        val clubsById = SampleClubs.createInitialClubs().associateBy { it.id }

        expectedIds.forEach { id ->
            val club = clubsById[id]
            assertNotNull("Missing Saudi club $id", club)
            assertTrue("Missing Arabic name for $id", !club!!.nameAr.isNullOrBlank())
            val svg = ClubSvgBadges.toSvgMarkup(club)
            assertTrue("Expected customSvg for $id", svg.contains("<svg"))
            val parsed = SvgCrestParser.parse(svg)
            assertTrue("Expected parsed SVG elements for $id", parsed.size >= 2)
        }
    }

    @Test
    fun globalWorldDatabaseHasZeroDuplicatesAndValidRatings() {
        val divisions = WorldDatabase.allDivisions
        val clubs = SampleClubs.createInitialClubs()

        assertEquals(18, WorldDatabase.playableCountries.size)
        assertEquals(43, divisions.size)
        assertTrue("Expected >= 340 real clubs, was ${clubs.size}", clubs.size >= 340)

        // Zero duplicate club IDs
        val uniqueIds = clubs.map { it.id }.toSet()
        assertEquals("Duplicate club IDs detected!", clubs.size, uniqueIds.size)

        // Every club has valid 16-man squad, 11 starters, 5 bench, and exportable SVG badge
        clubs.forEach { club ->
            assertEquals("Failed XI count for ${club.name}", 11, club.startingXiIds.size)
            assertEquals("Failed bench count for ${club.name}", 5, club.benchIds.size)
            assertEquals("Failed squad count for ${club.name}", 16, club.squad.size)

            val svg = ClubSvgBadges.toSvgMarkup(club)
            assertTrue(svg.contains("<svg"))
            assertTrue(svg.contains("</svg>"))
            if (club.crestSpec.customSvg == null) {
                assertTrue(svg.contains("<path"))
                assertTrue(svg.contains(club.shortName))
            }
        }

        // Verify realistic OVR scaling: Tier 1 elite >= 83, Tier 12 grassroots <= 55
        val engTier1Max = clubs.filter { it.divisionId == "eng_t1" }.maxOf { it.averageOverall }
        val engTier12Max = clubs.filter { it.divisionId == "eng_t12" }.maxOf { it.averageOverall }
        assertTrue("Expected Tier 1 max OVR >= 83, was $engTier1Max", engTier1Max >= 83)
        assertTrue("Expected Tier 12 max OVR <= 55, was $engTier12Max", engTier12Max <= 55)

        // Domestic Cup handles both 16-club nations (ENG) and 8-club nations (TUR/IRQ)
        val engT12Club = clubs.first { it.divisionId == "eng_t12" }
        val faCupR16 = SampleCompetitions.generateDomesticCupRoundOf16(CountryCode.ENG, engT12Club.id, clubs)
        assertEquals(8, faCupR16.size)
        assertTrue(faCupR16.any { it.homeClubId == engT12Club.id || it.awayClubId == engT12Club.id })

        val turClub = clubs.first { it.country == CountryCode.TUR }
        val turCup = SampleCompetitions.generateDomesticCupRoundOf16(CountryCode.TUR, turClub.id, clubs)
        assertEquals(4, turCup.size)
        assertEquals(2, turCup.first().round)
    }

    @Test
    fun uefaSwissAndAclEliteFormatsProgressCleanly() {
        val divisions = WorldDatabase.allDivisions
        val clubs = SampleClubs.createInitialClubs()
        val clubMap = clubs.associateBy { it.id }
        val standings = SampleCompetitions.createInitialStandings(clubs)
        val userClub = clubs.first { it.id == "ars" }

        val bundle = ContinentalEngine.seedSeasonTournaments(
            clubs = clubs,
            divisions = divisions,
            standings = standings,
            userClubId = userClub.id,
            random = Random(7),
        )

        // 36 clubs in UCL, 36 in UEL, 36 in UECL with zero overlap
        assertEquals(36, bundle.ucl.participantIds.size)
        assertEquals(36, bundle.uel.participantIds.size)
        assertEquals(36, bundle.uecl.participantIds.size)
        val allEuroIds = (bundle.ucl.participantIds + bundle.uel.participantIds + bundle.uecl.participantIds).toSet()
        assertEquals(108, allEuroIds.size)

        // Exact Swiss fixture counts: 8*18=144 (UCL & UEL), 6*18=108 (UECL)
        assertEquals(144, bundle.ucl.swissFixtures.size)
        assertEquals(144, bundle.uel.swissFixtures.size)
        assertEquals(108, bundle.uecl.swissFixtures.size)

        // Simulate UCL through Swiss -> PLAYOFF_L1 -> PLAYOFF_L2 -> R16 -> QF -> SF -> FINAL -> DONE
        var ucl = bundle.ucl
        val phasesSeen = mutableListOf(ucl.currentPhase)
        var safety = 0
        while (ucl.currentPhase != "DONE" && safety++ < 20) {
            val roundFixtures = ContinentalEngine.currentSwissRoundFixtures(ucl)
            assertTrue("Expected fixtures in phase ${ucl.currentPhase}", roundFixtures.isNotEmpty())
            val done = roundFixtures.map { f ->
                val sim = MatchEngine.simulateAiMatch(
                    fixture = f,
                    homeClub = clubMap.getValue(f.homeClubId),
                    awayClub = clubMap.getValue(f.awayClubId),
                    random = Random(safety),
                )
                f.copy(
                    homeScore = sim.homeScore,
                    awayScore = sim.awayScore,
                    homePenalties = sim.homePenalties,
                    awayPenalties = sim.awayPenalties,
                    played = true,
                )
            }
            ucl = ContinentalEngine.applyCompletedSwissFixtures(ucl, done, clubs)
            phasesSeen += ucl.currentPhase
        }
        assertEquals("DONE", ucl.currentPhase)
        assertNotNull(ucl.championClubId)
        assertTrue(phasesSeen.containsAll(listOf("SWISS", "PLAYOFF_L1", "PLAYOFF_L2", "R16", "QF", "SF", "FINAL", "DONE")))
        assertEquals(8, ucl.playoffFirstLeg.size)
        assertEquals(8, ucl.playoffSecondLeg.size)

        // ACL Elite: 12 West + 12 East, 8 rounds each -> Regional R16 -> Centralized Cross QF -> SF -> FINAL -> DONE
        var acl = bundle.acl
        assertEquals(12, acl.westClubIds.size)
        assertEquals(12, acl.eastClubIds.size)
        assertEquals(48, acl.westFixtures.size)
        assertEquals(48, acl.eastFixtures.size)

        val aclPhases = mutableListOf(acl.currentPhase)
        safety = 0
        while (acl.currentPhase != "DONE" && safety++ < 20) {
            val roundFixtures = ContinentalEngine.currentAclRoundFixtures(acl)
            assertTrue(roundFixtures.isNotEmpty())
            val done = roundFixtures.map { f ->
                val sim = MatchEngine.simulateAiMatch(
                    fixture = f,
                    homeClub = clubMap.getValue(f.homeClubId),
                    awayClub = clubMap.getValue(f.awayClubId),
                    random = Random(safety + 100),
                )
                f.copy(
                    homeScore = sim.homeScore,
                    awayScore = sim.awayScore,
                    homePenalties = sim.homePenalties,
                    awayPenalties = sim.awayPenalties,
                    played = true,
                )
            }
            acl = ContinentalEngine.applyCompletedAclFixtures(acl, done, clubs)
            aclPhases += acl.currentPhase
        }
        assertEquals("DONE", acl.currentPhase)
        assertNotNull(acl.championClubId)
        assertTrue(aclPhases.containsAll(listOf("LEAGUE", "R16", "QF", "SF", "FINAL", "DONE")))
        assertEquals(4, acl.knockoutFixtures.count { it.round == 2 && it.region == "CROSS" })
    }

    @Test
    fun matchEngineTacticsRatingsAndCareerEventsWork() {
        val clubs = SampleClubs.createInitialClubs()
        val engTop = clubs.filter { it.divisionId == "eng_t1" }
        val home = engTop[0]
        val away = engTop[1]
        val fixture = MatchFixture(
            id = "test_cup_1",
            round = 1,
            divisionId = home.divisionId,
            isCup = true,
            cupStageName = "Cup Final",
            homeClubId = home.id,
            awayClubId = away.id,
        )

        var session = MatchEngine.createSession(fixture, home, away, home.id)
        session = MatchEngine.setUserTactics(session, MatchMentality.ALL_OUT, MatchPressing.HIGH_PRESS)
        assertEquals(MatchMentality.ALL_OUT, session.userMentality)
        assertEquals(MatchPressing.HIGH_PRESS, session.userPressing)

        val outId = session.homeClub.startingXiIds.last()
        val inId = session.homeClub.benchIds.first()
        session = MatchEngine.substituteUserPlayer(session, outId, inId)
        assertEquals(1, session.subsUsedByUser)

        val finished = MatchEngine.simulateRestOfMatch(session, Random(42))
        assertTrue(finished.isFinished)
        assertEquals(90, finished.minute)
        assertEquals(100, finished.homeStats.possession + finished.awayStats.possession)
        assertNotNull(finished.manOfTheMatch)
        assertTrue(finished.playerRatings.isNotEmpty())
        assertTrue(finished.playerRatings.values.all { it in 4.0..10.0 })

        // Youth Academy & Board confidence check
        val prospects = CareerEventsEngine.generateYouthProspects(home, 3, seedOffset = 1, random = Random(9))
        assertEquals(3, prospects.size)
        val promoted = CareerEventsEngine.prospectToPlayer(prospects.first())
        assertEquals(4, promoted.contractYearsLeft)

        val state = CareerSaveState(
            careerStarted = true,
            userClubId = home.id,
            divisions = WorldDatabase.allDivisions,
            clubs = clubs,
            standings = SampleCompetitions.createInitialStandings(clubs),
        )
        val presser = PressConferenceGenerator.createIfKeyMatch(finished, state)
        assertNotNull(presser)
        assertEquals(3, presser!!.questions.size)
        assertTrue(presser.questions.all { it.options.size == 3 })
    }
}
