package com.example.engine

import com.example.data.SampleCompetitions
import com.example.model.CareerSaveState
import com.example.model.PressConferenceSession
import com.example.model.PressOption
import com.example.model.PressQuestion
import com.example.model.PressTone

/**
 * Generates contextual post-match Press Conferences after key fixtures
 * (debuts, domestic cups, continental clashes, play-offs, title/top-table duels, big wins/losses, red cards).
 */
object PressConferenceGenerator {

    fun createIfKeyMatch(
        session: LiveMatchSession,
        state: CareerSaveState,
    ): PressConferenceSession? {
        val fixture = session.fixture
        val userClub = session.userClub
        val opponent = session.opponentClub

        val userGoals = if (session.userIsHome) session.homeScore else session.awayScore
        val oppGoals = if (session.userIsHome) session.awayScore else session.homeScore
        val userPens = if (session.userIsHome) session.homePenalties else session.awayPenalties
        val oppPens = if (session.userIsHome) session.awayPenalties else session.homePenalties

        val userWon = userGoals > oppGoals || (userPens ?: 0) > (oppPens ?: 0)
        val isDraw = userGoals == oppGoals && userPens == null
        val goalMargin = userGoals - oppGoals

        val sortedStandings = SampleCompetitions.sortedStandingsForDivision(
            userClub.divisionId,
            state.standings,
            state.clubs,
        )
        val userRank = sortedStandings.indexOfFirst { it.clubId == userClub.id } + 1
        val oppRank = sortedStandings.indexOfFirst { it.clubId == opponent.id } + 1

        val hasRedCard = session.redCardedIds.isNotEmpty()
        val isDebut = !fixture.isCup && fixture.round == 1
        val isFinalDay = !fixture.isCup && fixture.round == state.totalLeagueRounds
        val isTopClash = !fixture.isCup && userRank in 1..3 && oppRank in 1..3 && fixture.round >= 3

        val contextBadge: String? = when {
            fixture.isPlayoff -> "PROMOTION PLAY-OFF"
            fixture.isContinental -> "${fixture.cupStageName?.uppercase() ?: "CONTINENTAL"} PRESSER"
            fixture.isCup -> "${userClub.country.domesticCupName.uppercase()} • ${fixture.cupStageName?.uppercase() ?: "CUP"}"
            isDebut -> "MANAGERIAL DEBUT"
            isFinalDay -> "SEASON FINALE"
            isTopClash -> "PROMOTION / TITLE CLASH"
            goalMargin >= 3 -> "STATEMENT VICTORY"
            goalMargin <= -2 -> "HEAVY DEFEAT"
            hasRedCard -> "TOUCHLINE DRAMA"
            else -> null
        }

        if (contextBadge == null) return null

        val scoreLine = buildString {
            append("${session.homeClub.shortName} ${session.homeScore} - ${session.awayScore} ${session.awayClub.shortName}")
            if (session.homePenalties != null && session.awayPenalties != null) {
                append(" (${session.homePenalties}-${session.awayPenalties} pens)")
            }
        }

        val compLabel = when {
            fixture.isPlayoff -> "Promotion Play-off"
            fixture.isContinental -> state.continentalCompetitionName
            fixture.isCup -> userClub.country.domesticCupName
            else -> "League"
        }

        val matchHeadline = when {
            fixture.isCup && userWon -> "${userClub.name} advance in the $compLabel!"
            fixture.isCup && !userWon -> "${userClub.name} fall short in the $compLabel"
            userWon && goalMargin >= 3 -> "${userClub.name} dismantle ${opponent.name} in style"
            userWon -> "${userClub.name} edge past ${opponent.name}"
            isDraw -> "Points shared between ${userClub.shortName} and ${opponent.shortName}"
            else -> "${opponent.name} stun ${userClub.name} on matchday"
        }

        val starPlayer = session.manOfTheMatch
            ?: userClub.startingXiPlayers().maxByOrNull { it.overall }?.name
            ?: "your captain"

        val questions = buildQuestions(
            userClubName = userClub.name,
            opponentName = opponent.name,
            starPlayer = starPlayer,
            userWon = userWon,
            isDraw = isDraw,
            isCup = fixture.isCup,
            compLabel = compLabel,
            isDebut = isDebut,
            hasRedCard = hasRedCard,
            subsUsed = session.subsUsedByUser,
            formationCode = userClub.formation.code,
        )

        return PressConferenceSession(
            matchHeadline = matchHeadline,
            contextBadge = contextBadge,
            opponentName = opponent.name,
            scoreSummary = scoreLine,
            questions = questions,
        )
    }

    private fun buildQuestions(
        userClubName: String,
        opponentName: String,
        starPlayer: String,
        userWon: Boolean,
        isDraw: Boolean,
        isCup: Boolean,
        compLabel: String,
        isDebut: Boolean,
        hasRedCard: Boolean,
        subsUsed: Int,
        formationCode: String,
    ): List<PressQuestion> {
        val q1 = PressQuestion(
            id = "q_result",
            journalistName = "Sarah Jenkins",
            outletName = "Touchline Global",
            question = when {
                isDebut && userWon -> "A dream debut against $opponentName! Does this prove $userClubName are built for a promotion or trophy push?"
                isDebut && isDraw -> "A point on your managerial bow against $opponentName. Are you pleased with how $userClubName executed your plan?"
                isDebut -> "A tough opening test against $opponentName. How do you rally the dressing room immediately after this debut setback?"
                isCup && userWon -> "Knockout pressure in the $compLabel, and you delivered against $opponentName. How far can $userClubName go?"
                isCup -> "Elimination from the $compLabel stings. Did $opponentName deserve it, or is full focus now on the league table?"
                userWon -> "Huge result against $opponentName! What gave $userClubName the decisive edge out there?"
                isDraw -> "A tight stalemate with $opponentName. Is that a point gained or two dropped in the table?"
                else -> "Supporters looked frustrated after that defeat to $opponentName. Do you take full responsibility?"
            },
            options = if (userWon) {
                listOf(
                    PressOption(
                        id = "q1_conf",
                        tone = PressTone.CONFIDENT,
                        quote = "\"This squad has fearless ambition. When we play at our tempo, we can beat anyone in front of us.\"",
                        moraleDelta = +4,
                        reputationDelta = +3,
                        headlineSnippet = "Fearless $userClubName boss issues statement after beating $opponentName.",
                    ),
                    PressOption(
                        id = "q1_calm",
                        tone = PressTone.CALM,
                        quote = "\"Credit to the players for executing our $formationCode structure. Now we stay grounded and recover.\"",
                        moraleDelta = +3,
                        reputationDelta = +2,
                        headlineSnippet = "Measured manager praises $userClubName discipline after $opponentName test.",
                    ),
                    PressOption(
                        id = "q1_humble",
                        tone = PressTone.HUMBLE,
                        quote = "\"The supporters carried us over the line, and $opponentName made us fight for every inch.\"",
                        moraleDelta = +2,
                        reputationDelta = +3,
                        headlineSnippet = "$userClubName boss salutes supporters after victory over $opponentName.",
                    ),
                )
            } else {
                listOf(
                    PressOption(
                        id = "q1_prot",
                        tone = PressTone.PROTECTIVE,
                        quote = "\"Put the scrutiny on me, not the dressing room. This group works tirelessly and will bounce back.\"",
                        moraleDelta = +4,
                        reputationDelta = +1,
                        headlineSnippet = "$userClubName manager shields squad from criticism after $opponentName clash.",
                    ),
                    PressOption(
                        id = "q1_fiery",
                        tone = PressTone.FIERY,
                        quote = "\"That standard wasn't good enough for this badge. Training tomorrow will be intense—places are up for grabs.\"",
                        moraleDelta = -2,
                        reputationDelta = +3,
                        headlineSnippet = "Furious $userClubName boss demands reaction after $opponentName setback.",
                    ),
                    PressOption(
                        id = "q1_calm",
                        tone = PressTone.CALM,
                        quote = "\"Margins were razor-thin. We will review the video, tighten our shape, and respond next matchday.\"",
                        moraleDelta = +1,
                        reputationDelta = +2,
                        headlineSnippet = "Composed $userClubName coach vows tactical response following $opponentName.",
                    ),
                )
            },
        )

        val q2 = PressQuestion(
            id = "q_tactics",
            journalistName = "Marcus Cole",
            outletName = "World Football Digest",
            question = when {
                hasRedCard -> "Discipline boiled over with a red card in the match. How will you keep cool heads in high-stakes fixtures?"
                subsUsed > 0 -> "You used $subsUsed substitution(s) in your $formationCode setup. How vital is squad rotation across league and cup?"
                else -> "You trusted your starting XI in the $formationCode system and $starPlayer stood out. Is this your settled core?"
            },
            options = listOf(
                PressOption(
                    id = "q2_prot",
                    tone = PressTone.PROTECTIVE,
                    quote = "\"Every player in this squad—starter or sub—knows their role. We win and lose as one unit.\"",
                    moraleDelta = +3,
                    reputationDelta = +1,
                    headlineSnippet = "Unity first: $userClubName boss backs entire squad.",
                ),
                PressOption(
                    id = "q2_conf",
                    tone = PressTone.CONFIDENT,
                    quote = "\"Our $formationCode tactical blueprint gives us control. Top performers like $starPlayer thrive in this system.\"",
                    moraleDelta = +2,
                    reputationDelta = +3,
                    headlineSnippet = "Tactical blueprint spotlight: $starPlayer shines in $formationCode.",
                ),
                PressOption(
                    id = "q2_fiery",
                    tone = PressTone.FIERY,
                    quote = "\"Nobody is guaranteed a shirt. If legs tire or discipline slips, someone hungry from the bench takes the spot.\"",
                    moraleDelta = -1,
                    reputationDelta = +2,
                    headlineSnippet = "$userClubName manager warns squad: no comfort zones.",
                ),
            ),
        )

        val q3 = PressQuestion(
            id = "q_ambition",
            journalistName = "Elena Vance",
            outletName = "Broadcast Live",
            question = "With the league pyramid and cup schedule heating up, what message do you have for rival clubs watching $userClubName?",
            options = listOf(
                PressOption(
                    id = "q3_conf",
                    tone = PressTone.CONFIDENT,
                    quote = "\"Expect fearless football. We are building a project here that clubs at every tier will respect.\"",
                    moraleDelta = +2,
                    reputationDelta = +4,
                    headlineSnippet = "$userClubName project turns heads with bold promise to rivals.",
                ),
                PressOption(
                    id = "q3_calm",
                    tone = PressTone.CALM,
                    quote = "\"One matchday at a time. We focus on fitness, smart transfers, and letting our football speak.\"",
                    moraleDelta = +2,
                    reputationDelta = +2,
                    headlineSnippet = "Steady hand at $userClubName keeps focus on next matchday.",
                ),
                PressOption(
                    id = "q3_humble",
                    tone = PressTone.HUMBLE,
                    quote = "\"We respect every club in the pyramid, keep grafting in training, and trust our squad's hunger.\"",
                    moraleDelta = +3,
                    reputationDelta = +1,
                    headlineSnippet = "Humility and graft: $userClubName stay grounded.",
                ),
            ),
        )

        return listOf(q1, q2, q3)
    }
}
