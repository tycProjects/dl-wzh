package com.example.model

import com.squareup.moshi.JsonClass

enum class Confederation {
    UEFA,
    AFC,
    CONMEBOL,
    CAF,
    CONCACAF,
}

enum class CountryCode(
    val displayName: String,
    val flagCode: String,
    val domesticCupName: String,
    val continentalName: String,
    val confederation: Confederation,
    val isPlayable: Boolean = true,
) {
    ENG("England", "ENG", "FA Cup", "UEFA Champions League", Confederation.UEFA),
    KSA("Saudi Arabia", "KSA", "King's Cup", "AFC Champions League Elite", Confederation.AFC),
    ESP("Spain", "ESP", "Copa del Rey", "UEFA Champions League", Confederation.UEFA),
    ITA("Italy", "ITA", "Coppa Italia", "UEFA Champions League", Confederation.UEFA),
    GER("Germany", "GER", "DFB-Pokal", "UEFA Champions League", Confederation.UEFA),
    FRA("France", "FRA", "Coupe de France", "UEFA Champions League", Confederation.UEFA),
    POR("Portugal", "POR", "Taca de Portugal", "UEFA Champions League", Confederation.UEFA),
    NED("Netherlands", "NED", "KNVB Cup", "UEFA Champions League", Confederation.UEFA),
    TUR("Turkey", "TUR", "Turkiye Kupasi", "UEFA Champions League", Confederation.UEFA),
    BRA("Brazil", "BRA", "Copa do Brasil", "Copa Libertadores", Confederation.CONMEBOL),
    ARG("Argentina", "ARG", "Copa Argentina", "Copa Libertadores", Confederation.CONMEBOL),
    IRQ("Iraq", "IRQ", "Iraq FA Cup", "AFC Champions League Elite", Confederation.AFC),
    UAE("UAE", "UAE", "President's Cup", "AFC Champions League Elite", Confederation.AFC),
    QAT("Qatar", "QAT", "Amir Cup", "AFC Champions League Elite", Confederation.AFC),
    EGY("Egypt", "EGY", "Egypt Cup", "CAF Champions League", Confederation.CAF),
    JPN("Japan", "JPN", "Emperor's Cup", "AFC Champions League Elite", Confederation.AFC),
    KOR("South Korea", "KOR", "Korea Cup", "AFC Champions League Elite", Confederation.AFC),
    USA("USA", "USA", "U.S. Open Cup", "CONCACAF Champions", Confederation.CONCACAF),
    EUR_OTHER("Europe", "EUR", "European Cup", "UEFA Champions League", Confederation.UEFA, isPlayable = false),
    AFC_OTHER("Asia", "AFC", "Asian Cup", "AFC Champions League Elite", Confederation.AFC, isPlayable = false),
}

enum class ContinentalTournamentId(
    val displayName: String,
    val shortCode: String,
    val totalLeagueRounds: Int,
) {
    UCL("UEFA Champions League", "UCL", 8),
    UEL("UEFA Europa League", "UEL", 8),
    UECL("UEFA Conference League", "UECL", 6),
    ACL_ELITE("AFC Champions League Elite", "ACL Elite", 8),
}

enum class ContinentalTier(val displayName: String) {
    NONE("None"),
    UCL("UEFA Champions League"),
    UEL("UEFA Europa League"),
    UECL("UEFA Conference League"),
    ACL_ELITE("AFC Champions League Elite"),
}

enum class CrestShape(val displayName: String) {
    CLASSIC_SHIELD("Classic Shield"),
    MODERN_CREST("Modern Crest"),
    ROUNDEL("Heritage Roundel"),
    ITALIAN_OVAL("Oval Crest"),
    SPANISH_CROWN("Royal Shield"),
    DIAMOND("Diamond Crest"),
}

enum class CrestPattern(val displayName: String) {
    SOLID("Solid"),
    VERTICAL_STRIPES("Stripes"),
    HOOPS("Hoops"),
    HALF_AND_HALF("Halves"),
    DIAGONAL_SASH("Sash"),
    CHEVRON("Chevron"),
    CROSS("Cross"),
    QUARTERS("Quarters"),
}

enum class CrestSymbol(val displayName: String) {
    NONE("Monogram"),
    LION("Lion"),
    CROWN("Crown"),
    EAGLE("Eagle"),
    STAR("Star"),
    CANNON("Cannon"),
    ANCHOR("Anchor"),
    BOLT("Bolt"),
    TOWER("Castle"),
    CRESCENT("Crescent"),
    LAUREL("Laurel"),
    FLAME("Flame"),
}

@JsonClass(generateAdapter = true)
data class ClubCrestSpec(
    val shape: CrestShape = CrestShape.CLASSIC_SHIELD,
    val pattern: CrestPattern = CrestPattern.SOLID,
    val symbol: CrestSymbol = CrestSymbol.STAR,
    val accentHex: String = "#F8FAFC",
    val customSvg: String? = null,
)

enum class Position(val label: String, val sortOrder: Int) {
    GK("GK", 0),
    DEF("DEF", 1),
    MID("MID", 2),
    FWD("FWD", 3),
}

enum class BadgeStyle(val displayName: String) {
    SHIELD("Classic Shield"),
    CREST("Split Crest"),
    CROWN("Royal Crown"),
    BOLT("Strike Bolt"),
    STAR("All-Star"),
    EAGLE("Winged Crest"),
}

data class PitchSlot(
    val slotIndex: Int,
    val roleLabel: String,
    val position: Position,
    val xFraction: Float,
    val yFraction: Float,
)

enum class Formation(
    val code: String,
    val description: String,
    val defCount: Int,
    val midCount: Int,
    val fwdCount: Int,
    val slots: List<PitchSlot>,
) {
    F433(
        code = "4-3-3",
        description = "High-press wingers & fluid front three",
        defCount = 4,
        midCount = 3,
        fwdCount = 3,
        slots = listOf(
            PitchSlot(0, "GK", Position.GK, 0.50f, 0.89f),
            PitchSlot(1, "LB", Position.DEF, 0.14f, 0.70f),
            PitchSlot(2, "LCB", Position.DEF, 0.37f, 0.72f),
            PitchSlot(3, "RCB", Position.DEF, 0.63f, 0.72f),
            PitchSlot(4, "RB", Position.DEF, 0.86f, 0.70f),
            PitchSlot(5, "LCM", Position.MID, 0.25f, 0.47f),
            PitchSlot(6, "CDM", Position.MID, 0.50f, 0.52f),
            PitchSlot(7, "RCM", Position.MID, 0.75f, 0.47f),
            PitchSlot(8, "LW", Position.FWD, 0.18f, 0.21f),
            PitchSlot(9, "ST", Position.FWD, 0.50f, 0.15f),
            PitchSlot(10, "RW", Position.FWD, 0.82f, 0.21f),
        ),
    ),
    F442(
        code = "4-4-2",
        description = "Balanced two-bank shape & twin strikers",
        defCount = 4,
        midCount = 4,
        fwdCount = 2,
        slots = listOf(
            PitchSlot(0, "GK", Position.GK, 0.50f, 0.89f),
            PitchSlot(1, "LB", Position.DEF, 0.14f, 0.71f),
            PitchSlot(2, "LCB", Position.DEF, 0.37f, 0.73f),
            PitchSlot(3, "RCB", Position.DEF, 0.63f, 0.73f),
            PitchSlot(4, "RB", Position.DEF, 0.86f, 0.71f),
            PitchSlot(5, "LM", Position.MID, 0.14f, 0.46f),
            PitchSlot(6, "LCM", Position.MID, 0.38f, 0.49f),
            PitchSlot(7, "RCM", Position.MID, 0.62f, 0.49f),
            PitchSlot(8, "RM", Position.MID, 0.86f, 0.46f),
            PitchSlot(9, "LST", Position.FWD, 0.36f, 0.18f),
            PitchSlot(10, "RST", Position.FWD, 0.64f, 0.18f),
        ),
    ),
    F352(
        code = "3-5-2",
        description = "Midfield overload with dynamic wingbacks",
        defCount = 3,
        midCount = 5,
        fwdCount = 2,
        slots = listOf(
            PitchSlot(0, "GK", Position.GK, 0.50f, 0.89f),
            PitchSlot(1, "LCB", Position.DEF, 0.25f, 0.72f),
            PitchSlot(2, "CB", Position.DEF, 0.50f, 0.74f),
            PitchSlot(3, "RCB", Position.DEF, 0.75f, 0.72f),
            PitchSlot(4, "LWB", Position.MID, 0.12f, 0.49f),
            PitchSlot(5, "LCM", Position.MID, 0.33f, 0.50f),
            PitchSlot(6, "CAM", Position.MID, 0.50f, 0.39f),
            PitchSlot(7, "RCM", Position.MID, 0.67f, 0.50f),
            PitchSlot(8, "RWB", Position.MID, 0.88f, 0.49f),
            PitchSlot(9, "LST", Position.FWD, 0.36f, 0.17f),
            PitchSlot(10, "RST", Position.FWD, 0.64f, 0.17f),
        ),
    ),
    F4231(
        code = "4-2-3-1",
        description = "Double pivot control behind creative #10",
        defCount = 4,
        midCount = 5,
        fwdCount = 1,
        slots = listOf(
            PitchSlot(0, "GK", Position.GK, 0.50f, 0.89f),
            PitchSlot(1, "LB", Position.DEF, 0.14f, 0.71f),
            PitchSlot(2, "LCB", Position.DEF, 0.37f, 0.73f),
            PitchSlot(3, "RCB", Position.DEF, 0.63f, 0.73f),
            PitchSlot(4, "RB", Position.DEF, 0.86f, 0.71f),
            PitchSlot(5, "LDM", Position.MID, 0.36f, 0.55f),
            PitchSlot(6, "RDM", Position.MID, 0.64f, 0.55f),
            PitchSlot(7, "LAM", Position.MID, 0.18f, 0.34f),
            PitchSlot(8, "CAM", Position.MID, 0.50f, 0.33f),
            PitchSlot(9, "RAM", Position.MID, 0.82f, 0.34f),
            PitchSlot(10, "ST", Position.FWD, 0.50f, 0.14f),
        ),
    ),
}

enum class TrainingFocus(
    val title: String,
    val subtitle: String,
    val fitnessDelta: Int,
    val moraleDelta: Int,
    val statNote: String,
) {
    FITNESS("Conditioning", "High-intensity stamina drills", +12, -2, "+12 Fitness, slight fatigue risk"),
    ATTACKING("Finishing & Final Third", "Shooting patterns & set pieces", -5, +4, "+4 Morale, sharpens attackers"),
    DEFENDING("Compact Shape", "Pressing traps & zonal marking", -5, +3, "+3 Morale, tightens backline"),
    TACTICAL("Match Prep", "Video review & formation chemistry", -3, +6, "+6 Morale, balanced boost"),
    RECOVERY("Active Recovery", "Light pitch session & hydrotherapy", +16, +5, "+16 Fitness & +5 Morale"),
}

enum class MatchMentality(
    val label: String,
    val attackFactor: Double,
    val defenseFactor: Double,
) {
    DEFENSIVE("Defensive", -3.2, +3.6),
    BALANCED("Balanced", 0.0, 0.0),
    ATTACKING("Attacking", +3.2, -2.6),
    ALL_OUT("All-Out", +5.8, -5.2),
}

enum class MatchPressing(
    val label: String,
    val midfieldFactor: Double,
    val extraStaminaDrain: Int,
) {
    LOW_BLOCK("Low Block", -2.0, 0),
    MID_PRESS("Mid Press", 0.0, 0),
    HIGH_PRESS("Gegenpress", +3.4, 1),
}

@JsonClass(generateAdapter = true)
data class DivisionInfo(
    val id: String,
    val name: String,
    val country: CountryCode,
    val tier: Int,
    val autoPromoteSpots: Int,
    val playoffSpots: Int,
    val relegateSpots: Int,
    val uclSpots: Int = 0,
    val uelSpots: Int = 0,
    val ueclSpots: Int = 0,
    val aclSpots: Int = 0,
)

enum class SquadRole(val label: String, val rank: Int) {
    CRUCIAL("Crucial", 3),
    IMPORTANT("Important", 2),
    ROTATION("Rotation", 1),
    PROSPECT("Prospect", 0),
}

@JsonClass(generateAdapter = true)
data class Player(
    val id: String,
    val name: String,
    val age: Int,
    val nationality: String,
    val position: Position,
    val overall: Int,
    val pace: Int,
    val shooting: Int,
    val passing: Int,
    val defending: Int,
    val fitness: Int = 92,
    val morale: Int = 80,
    val marketValueM: Double,
    val wageK: Int,
    val goals: Int = 0,
    val assists: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val appearances: Int = 0,
    val contractYearsLeft: Int = 3,
    val potential: Int = (overall + 3).coerceAtMost(94),
    val injuredMatches: Int = 0,
    val suspendedMatches: Int = 0,
    val role: SquadRole = SquadRole.ROTATION,
    val sellingClubId: String? = null,
    val sellingClubName: String? = null,
)

@JsonClass(generateAdapter = true)
data class YouthProspect(
    val id: String,
    val name: String,
    val age: Int,
    val nationality: String,
    val position: Position,
    val overall: Int,
    val potentialMin: Int,
    val potentialMax: Int,
    val potential: Int,
    val marketValueM: Double,
    val wageK: Int,
    val scoutNote: String,
)

@JsonClass(generateAdapter = true)
data class Club(
    val id: String,
    val name: String,
    val nameAr: String? = null,
    val shortName: String,
    val country: CountryCode = CountryCode.ENG,
    val divisionId: String = "eng_t1",
    val tier: Int = 1,
    val city: String,
    val stadium: String,
    val primaryColorHex: String,
    val secondaryColorHex: String,
    val badgeStyle: BadgeStyle = BadgeStyle.SHIELD,
    val crestSpec: ClubCrestSpec = ClubCrestSpec(),
    val budgetM: Double,
    val prestige: Int,
    val reputation: Int = prestige,
    val formation: Formation = Formation.F433,
    val squad: List<Player>,
    val startingXiIds: List<String>,
    val benchIds: List<String>,
    val weeklyWageBudgetK: Int = 0,
) {
    val averageOverall: Int
        get() {
            val xi = startingXiPlayers()
            return if (xi.isEmpty()) prestige else xi.map { it.overall }.average().toInt()
        }

    val totalWeeklyWageK: Int
        get() = squad.sumOf { it.wageK }

    val effectiveWageBudgetK: Int
        get() = if (weeklyWageBudgetK > 0) {
            weeklyWageBudgetK
        } else {
            ((totalWeeklyWageK * 1.25).toInt()).coerceAtLeast(totalWeeklyWageK + 18)
        }

    val remainingWageHeadroomK: Int
        get() = effectiveWageBudgetK - totalWeeklyWageK

    fun startingXiPlayers(): List<Player> {
        val byId = squad.associateBy { it.id }
        return startingXiIds.mapNotNull { byId[it] }
    }

    fun benchPlayers(): List<Player> {
        val byId = squad.associateBy { it.id }
        return benchIds.mapNotNull { byId[it] }
    }
}

enum class MatchEventType {
    KICKOFF,
    GOAL,
    CHANCE,
    SAVE,
    YELLOW_CARD,
    RED_CARD,
    INJURY,
    SUBSTITUTION,
    HALFTIME,
    FULLTIME,
    PENALTY_SHOOTOUT,
}

@JsonClass(generateAdapter = true)
data class MatchEvent(
    val minute: Int,
    val type: MatchEventType,
    val clubId: String?,
    val primaryPlayerName: String? = null,
    val secondaryPlayerName: String? = null,
    val commentary: String,
)

@JsonClass(generateAdapter = true)
data class MatchTeamStats(
    val possession: Int = 50,
    val shots: Int = 0,
    val shotsOnTarget: Int = 0,
    val expectedGoals: Double = 0.0,
    val corners: Int = 0,
    val fouls: Int = 0,
)

@JsonClass(generateAdapter = true)
data class MatchFixture(
    val id: String,
    val round: Int,
    val divisionId: String = "",
    val isCup: Boolean = false,
    val isContinental: Boolean = false,
    val isPlayoff: Boolean = false,
    val continentalId: ContinentalTournamentId? = null,
    val region: String? = null, // "WEST", "EAST", "CROSS" for ACL Elite
    val cupStageName: String? = null,
    val aggregateNote: String? = null,
    val homeClubId: String,
    val awayClubId: String,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val homePenalties: Int? = null,
    val awayPenalties: Int? = null,
    val played: Boolean = false,
    val homeStats: MatchTeamStats = MatchTeamStats(),
    val awayStats: MatchTeamStats = MatchTeamStats(),
    val events: List<MatchEvent> = emptyList(),
    val manOfTheMatch: String? = null,
) {
    fun winnerClubId(): String? {
        if (!played) return null
        return when {
            homeScore > awayScore -> homeClubId
            awayScore > homeScore -> awayClubId
            (homePenalties ?: 0) > (awayPenalties ?: 0) -> homeClubId
            (awayPenalties ?: 0) > (homePenalties ?: 0) -> awayClubId
            else -> homeClubId
        }
    }
}

@JsonClass(generateAdapter = true)
data class LeagueStanding(
    val clubId: String,
    val divisionId: String = "",
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val points: Int = 0,
    val form: List<String> = emptyList(),
) {
    val goalDifference: Int get() = goalsFor - goalsAgainst
}

/**
 * Complete UEFA 36-Team Swiss-System Tournament State (UCL, UEL, UECL).
 */
@JsonClass(generateAdapter = true)
data class SwissTournamentState(
    val tournamentId: ContinentalTournamentId,
    val participantIds: List<String>,
    val standings: List<LeagueStanding>,
    val swissFixtures: List<MatchFixture>,
    val playoffFirstLeg: List<MatchFixture> = emptyList(),
    val playoffSecondLeg: List<MatchFixture> = emptyList(),
    val knockoutFixtures: List<MatchFixture> = emptyList(), // round: 1=R16, 2=QF, 3=SF, 4=Final
    val currentPhase: String = "SWISS", // SWISS, PLAYOFF_L1, PLAYOFF_L2, R16, QF, SF, FINAL, DONE
    val swissRoundsPlayed: Int = 0,
    val championClubId: String? = null,
)

/**
 * Complete AFC Champions League Elite State (West & East Regions -> Regional R16 -> Centralized QF/SF/Final).
 */
@JsonClass(generateAdapter = true)
data class AclEliteState(
    val westClubIds: List<String>,
    val eastClubIds: List<String>,
    val westStandings: List<LeagueStanding>,
    val eastStandings: List<LeagueStanding>,
    val westFixtures: List<MatchFixture>,
    val eastFixtures: List<MatchFixture>,
    val knockoutFixtures: List<MatchFixture> = emptyList(), // round: 1=R16 (West+East), 2=Centralized QF, 3=SF, 4=Final
    val currentPhase: String = "LEAGUE", // LEAGUE, R16, QF, SF, FINAL, DONE
    val leagueRoundsPlayed: Int = 0,
    val championClubId: String? = null,
)

enum class InboxCategory(val label: String) {
    TRANSFER_OFFER("Offer"),
    BOARD("Board"),
    MEDICAL("Medical"),
    DISCIPLINE("Discipline"),
    CONTINENTAL("Continental"),
    NEWS("Club News"),
}

enum class OfferStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
}

@JsonClass(generateAdapter = true)
data class TransferOfferPayload(
    val playerId: String,
    val playerName: String,
    val buyingClubId: String,
    val buyingClubName: String,
    val offerAmountM: Double,
    val status: OfferStatus = OfferStatus.PENDING,
)

@JsonClass(generateAdapter = true)
data class InboxMessage(
    val id: String,
    val category: InboxCategory,
    val title: String,
    val body: String,
    val matchdayLabel: String,
    val isRead: Boolean = false,
    val offer: TransferOfferPayload? = null,
)

enum class PressTone(val label: String) {
    CONFIDENT("Confident"),
    CALM("Measured"),
    FIERY("Fiery"),
    PROTECTIVE("Protective"),
    HUMBLE("Humble"),
}

@JsonClass(generateAdapter = true)
data class PressOption(
    val id: String,
    val tone: PressTone,
    val quote: String,
    val moraleDelta: Int,
    val reputationDelta: Int,
    val headlineSnippet: String,
)

@JsonClass(generateAdapter = true)
data class PressQuestion(
    val id: String,
    val journalistName: String,
    val outletName: String,
    val question: String,
    val options: List<PressOption>,
)

@JsonClass(generateAdapter = true)
data class PressConferenceSession(
    val matchHeadline: String,
    val contextBadge: String,
    val opponentName: String,
    val scoreSummary: String,
    val questions: List<PressQuestion>,
    val currentQuestionIndex: Int = 0,
    val totalMoraleDelta: Int = 0,
    val totalReputationDelta: Int = 0,
    val lastHeadlineSnippet: String? = null,
    val isComplete: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class SeasonSummaryReport(
    val seasonNumber: Int,
    val userDivisionName: String,
    val userFinishPosition: Int,
    val leagueChampionName: String,
    val domesticCupWinnerName: String?,
    val continentalWinnerName: String?,
    val uclWinnerName: String? = null,
    val uelWinnerName: String? = null,
    val ueclWinnerName: String? = null,
    val aclWinnerName: String? = null,
    val promotionStatusText: String,
    val promotedClubs: List<String>,
    val relegatedClubs: List<String>,
)

enum class TransferNegotiationPhase {
    CLUB_TALKS,
    PLAYER_TALKS,
    ACCEPTED,
    COLLAPSED,
}

@JsonClass(generateAdapter = true)
data class TransferNegotiationSession(
    val playerId: String,
    val playerName: String,
    val position: Position,
    val overall: Int,
    val age: Int,
    val sellingClubId: String?,
    val sellingClubName: String,
    val sellingClubPrimaryHex: String = "#334155",
    val sellingClubSecondaryHex: String = "#F8FAFC",
    val phase: TransferNegotiationPhase = TransferNegotiationPhase.CLUB_TALKS,
    val marketValueM: Double,
    val askingFeeM: Double,
    val minAcceptableFeeM: Double,
    val currentBidM: Double,
    val agreedFeeM: Double = 0.0,
    val addOnPercent: Int = 0,
    val isDirectRival: Boolean = false,
    val isKeyPlayer: Boolean = false,
    val willingnessLabel: String = "Open to Talks",
    val clubRoundsRemaining: Int = 3,
    val playerRoundsRemaining: Int = 3,
    val desiredWageK: Int,
    val minAcceptableWageK: Int,
    val desiredRole: SquadRole,
    val desiredYears: Int = 4,
    val offeredWageK: Int,
    val offeredYears: Int = 4,
    val offeredRole: SquadRole,
    val signingBonusM: Double = 0.0,
    val statusMessage: String,
    val lastActionAccepted: Boolean? = null,
)

enum class ContractNegotiationStatus {
    IN_PROGRESS,
    ACCEPTED,
    WALKED_AWAY,
}

@JsonClass(generateAdapter = true)
data class ContractNegotiationSession(
    val playerId: String,
    val playerName: String,
    val position: Position,
    val overall: Int,
    val currentWageK: Int,
    val currentYearsLeft: Int,
    val currentRole: SquadRole,
    val desiredWageK: Int,
    val minAcceptableWageK: Int,
    val desiredMinYears: Int,
    val desiredRole: SquadRole,
    val offeredWageK: Int,
    val offeredExtraYears: Int,
    val offeredRole: SquadRole,
    val roundsRemaining: Int = 3,
    val status: ContractNegotiationStatus = ContractNegotiationStatus.IN_PROGRESS,
    val statusMessage: String,
)

@JsonClass(generateAdapter = true)
data class CareerSaveState(
    val careerStarted: Boolean = false,
    val seasonNumber: Int = 1,
    val currentMatchday: Int = 1,
    val totalLeagueRounds: Int = 14,
    val currentCupStage: Int = 1, // 1=R16, 2=QF, 3=SF, 4=Final, 5=Done
    val currentContinentalStage: Int = 1,
    val userClubId: String = "",
    val managerName: String = "Career Manager",
    val boardConfidence: Int = 78,
    val divisions: List<DivisionInfo> = emptyList(),
    val clubs: List<Club> = emptyList(),
    val transferMarket: List<Player> = emptyList(),
    val youthAcademy: List<YouthProspect> = emptyList(),
    val inboxMessages: List<InboxMessage> = emptyList(),
    val leagueFixtures: List<MatchFixture> = emptyList(),
    val cupFixtures: List<MatchFixture> = emptyList(),
    val continentalFixtures: List<MatchFixture> = emptyList(),
    val playoffFixtures: List<MatchFixture> = emptyList(),
    val continentalCompetitionName: String = "UEFA Champions League",
    val uclState: SwissTournamentState? = null,
    val uelState: SwissTournamentState? = null,
    val ueclState: SwissTournamentState? = null,
    val aclState: AclEliteState? = null,
    val standings: List<LeagueStanding> = emptyList(),
    val trainingUsedThisWeek: Boolean = false,
    val lastTrainingNote: String? = null,
    val cupChampionClubId: String? = null,
    val continentalChampionClubId: String? = null,
    val leagueChampionClubId: String? = null,
    val recentNews: List<String> = emptyList(),
    val pendingPressConference: PressConferenceSession? = null,
    val seasonSummary: SeasonSummaryReport? = null,
    val activeTransferNegotiation: TransferNegotiationSession? = null,
    val activeContractNegotiation: ContractNegotiationSession? = null,
)
