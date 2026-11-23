package com.example.engine

import com.example.data.world.RealPlayerRosterBuilder
import com.example.model.Club
import com.example.model.CountryCode
import com.example.model.DivisionInfo
import com.example.model.InboxCategory
import com.example.model.InboxMessage
import com.example.model.LeagueStanding
import com.example.model.OfferStatus
import com.example.model.Player
import com.example.model.Position
import com.example.model.TransferOfferPayload
import com.example.model.YouthProspect
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Generates dynamic career ecosystem events:
 * - Youth Academy scouting intakes (15-18yo prospects)
 * - Incoming AI transfer bids for user's players
 * - Weekly balanced wage cashflow & board confidence evaluation
 */
object CareerEventsEngine {

    private val firstNamesByCountry = mapOf(
        CountryCode.ENG to listOf("Ethan", "Noah", "Archie", "Jude", "Myles", "Leo", "Kieran", "Callum"),
        CountryCode.KSA to listOf("Tariq", "Nawaf", "Faisal", "Ziyad", "Rayan", "Bader", "Turki", "Sultan"),
        CountryCode.ESP to listOf("Mateo", "Hugo", "Pablo", "Iker", "Marc", "Diego", "Gael", "Lucas"),
        CountryCode.ITA to listOf("Lorenzo", "Mattia", "Giulio", "Tommaso", "Nicolo", "Davide", "Luca"),
        CountryCode.GER to listOf("Jonas", "Lukas", "Paul", "Finn", "Ben", "Maximilian", "Felix"),
        CountryCode.FRA to listOf("Theo", "Kylian", "Enzo", "Nathan", "Rayan", "Yanis", "Hugo"),
        CountryCode.POR to listOf("Rafael", "Rodrigo", "Tiago", "Diogo", "Joao", "Martim", "Tomas"),
        CountryCode.NED to listOf("Sem", "Daan", "Luuk", "Thijs", "Mees", "Bram", "Kees"),
        CountryCode.TUR to listOf("Emre", "Arda", "Kerem", "Yusuf", "Mert", "Can", "Kaan"),
        CountryCode.BRA to listOf("Gabriel", "Matheus", "Lucas", "Pedro", "Caua", "Vitor", "Joao"),
        CountryCode.ARG to listOf("Thiago", "Bautista", "Mateo", "Santino", "Valentin", "Lautaro"),
        CountryCode.IRQ to listOf("Mustafa", "Ali", "Zaid", "Hussein", "Karrar", "Yousif"),
        CountryCode.UAE to listOf("Rashid", "Mansoor", "Hamdan", "Saeed", "Khalifa"),
        CountryCode.QAT to listOf("Tamim", "Yusuf", "Khalid", "Hassan", "Jassim"),
        CountryCode.EGY to listOf("Omar", "Youssef", "Karim", "Ziad", "Mostafa", "Marwan"),
        CountryCode.JPN to listOf("Ren", "Haruto", "Sota", "Kaito", "Yuto", "Riku"),
        CountryCode.KOR to listOf("Min-jun", "Seo-jun", "Ji-ho", "Do-yun", "Ha-jun"),
        CountryCode.USA to listOf("Caleb", "Mason", "Owen", "Landon", "Cole", "Tyler"),
    )

    private val lastNamesByCountry = mapOf(
        CountryCode.ENG to listOf("Whitaker", "Bramwell", "Aldridge", "Naylor", "Fletcher", "Barton", "Kerr", "Houghton"),
        CountryCode.KSA to listOf("Al-Harbi", "Al-Ghamdi", "Al-Qahtani", "Al-Mutairi", "Al-Shahrani", "Al-Otaibi"),
        CountryCode.ESP to listOf("Morales", "Serrano", "Navarro", "Mendoza", "Dominguez", "Reyes", "Vidal"),
        CountryCode.ITA to listOf("Ricci", "Moretti", "Conti", "De Luca", "Ferraro", "Galli", "Marchetti"),
        CountryCode.GER to listOf("Hoffmann", "Weber", "Becker", "Wagner", "Richter", "Keller", "Schulz"),
        CountryCode.FRA to listOf("Lefevre", "Chevalier", "Morel", "Dupont", "Gauthier", "Benoit", "Diallo"),
        CountryCode.POR to listOf("Ferreira", "Pereira", "Oliveira", "Ribeiro", "Carvalho", "Teixeira"),
        CountryCode.NED to listOf("De Vries", "Bakker", "Van Dijk", "Visser", "Mulder", "Bos"),
        CountryCode.TUR to listOf("Yilmaz", "Kaya", "Demir", "Sahin", "Celik", "Yildiz", "Aydin"),
        CountryCode.BRA to listOf("Silva", "Santos", "Oliveira", "Souza", "Pereira", "Costa", "Almeida"),
        CountryCode.ARG to listOf("Gomez", "Fernandez", "Lopez", "Martinez", "Acosta", "Romero"),
        CountryCode.IRQ to listOf("Al-Dulaimi", "Al-Jubouri", "Kareem", "Naji", "Kadhim"),
        CountryCode.UAE to listOf("Al-Maktoum", "Al-Suwaidi", "Al-Nuaimi", "Al-Dhaheri"),
        CountryCode.QAT to listOf("Al-Thani", "Al-Kuwari", "Al-Sulaiti", "Al-Marri"),
        CountryCode.EGY to listOf("El-Masry", "Hassan", "Ibrahim", "Mansour", "Farouk"),
        CountryCode.JPN to listOf("Tanaka", "Sato", "Suzuki", "Takahashi", "Kobayashi", "Ito"),
        CountryCode.KOR to listOf("Kim", "Park", "Lee", "Choi", "Jung", "Kang"),
        CountryCode.USA to listOf("Miller", "Aaronson", "Paredes", "Sullivan", "Reynolds"),
    )

    private val scoutNotes = listOf(
        "Explosive acceleration in U18 academy friendlies.",
        "Composed under press with clean first touch.",
        "Strong aerial duel win rate & tactical discipline.",
        "Clinical finisher off both feet in academy cup.",
        "Tireless engine and sharp positional awareness.",
    )

    fun generateYouthProspects(
        club: Club,
        count: Int,
        seedOffset: Int = 0,
        random: Random = Random.Default,
    ): List<YouthProspect> {
        val firsts = firstNamesByCountry[club.country] ?: firstNamesByCountry.getValue(CountryCode.ENG)
        val lasts = lastNamesByCountry[club.country] ?: lastNamesByCountry.getValue(CountryCode.ENG)
        val natCode = club.country.flagCode

        val baseOvr = (club.averageOverall - 16).coerceIn(42, 70)
        return (0 until count).map { idx ->
            val pos = Position.entries[(idx + seedOffset + random.nextInt(4)) % Position.entries.size]
            val age = 15 + random.nextInt(4) // 15-18
            val ovr = (baseOvr + random.nextInt(-3, 5)).coerceIn(41, 73)
            val potMin = (ovr + 6 + random.nextInt(5)).coerceIn(ovr + 4, 88)
            val potMax = (potMin + 5 + random.nextInt(5)).coerceIn(potMin + 2, 93)
            val truePot = random.nextInt(potMin, potMax + 1)
            val valM = (((ovr - 38).coerceAtLeast(1) * 0.05) + (club.tier == 1).let { if (it) 0.35 else 0.03 })
                .let { (it * 100.0).roundToInt() / 100.0 }
            val wage = ((ovr * 0.22).roundToInt()).coerceIn(1, 18)

            YouthProspect(
                id = "yp_${club.id}_${System.currentTimeMillis()}_${seedOffset}_$idx",
                name = "${firsts.random(random)} ${lasts.random(random)}",
                age = age,
                nationality = natCode,
                position = pos,
                overall = ovr,
                potentialMin = potMin,
                potentialMax = potMax,
                potential = truePot,
                marketValueM = valM,
                wageK = wage,
                scoutNote = scoutNotes.random(random),
            )
        }
    }

    fun prospectToPlayer(prospect: YouthProspect): Player {
        val ovr = prospect.overall
        val pace = (ovr + if (prospect.position == Position.FWD) 4 else 1).coerceIn(40, 94)
        val sho = (ovr + if (prospect.position == Position.FWD) 3 else -6).coerceIn(30, 94)
        val pas = (ovr + if (prospect.position == Position.MID) 4 else -2).coerceIn(35, 94)
        val def = (ovr + if (prospect.position == Position.DEF || prospect.position == Position.GK) 4 else -14).coerceIn(25, 94)
        return Player(
            id = "grad_${prospect.id}",
            name = prospect.name,
            age = prospect.age,
            nationality = prospect.nationality,
            position = prospect.position,
            overall = ovr,
            pace = pace,
            shooting = sho,
            passing = pas,
            defending = def,
            fitness = 96,
            morale = 90,
            marketValueM = prospect.marketValueM,
            wageK = prospect.wageK,
            contractYearsLeft = 4,
            potential = prospect.potential,
        )
    }

    fun maybeGenerateTransferOffer(
        userClub: Club,
        allClubs: List<Club>,
        matchday: Int,
        random: Random = Random.Default,
    ): InboxMessage? {
        if (userClub.squad.size <= 14) return null
        if (random.nextDouble() > 0.38) return null

        val candidate = userClub.squad
            .filter { it.injuredMatches == 0 }
            .sortedByDescending { it.overall }
            .take(10)
            .randomOrNull(random) ?: return null

        val buyer = allClubs
            .filter { it.id != userClub.id && it.tier <= (userClub.tier + 1) }
            .shuffled(random)
            .firstOrNull() ?: return null

        val multiplier = 0.95 + random.nextDouble() * 0.30
        val offerM = ((candidate.marketValueM * multiplier) * 100.0).roundToInt() / 100.0
        val feeLabel = RealPlayerRosterBuilder.formatBudget(offerM)

        return InboxMessage(
            id = "offer_${candidate.id}_md${matchday}_${random.nextInt(999)}",
            category = InboxCategory.TRANSFER_OFFER,
            title = "Transfer Bid: ${candidate.name}",
            body = "${buyer.name} have tabled a $feeLabel offer for ${candidate.name} (${candidate.position.label}, ${candidate.overall} OVR). Accept to add funds to your budget or reject to keep him.",
            matchdayLabel = "MD $matchday",
            isRead = false,
            offer = TransferOfferPayload(
                playerId = candidate.id,
                playerName = candidate.name,
                buyingClubId = buyer.id,
                buyingClubName = buyer.name,
                offerAmountM = offerM,
                status = OfferStatus.PENDING,
            ),
        )
    }

    data class FinancialTickResult(
        val updatedBudgetM: Double,
        val netCashflowM: Double,
        val newBoardConfidence: Int,
        val boardWarningMessage: InboxMessage?,
    )

    fun evaluateWeeklyFinancesAndBoard(
        club: Club,
        sortedStandings: List<LeagueStanding>,
        division: DivisionInfo?,
        currentConfidence: Int,
        matchday: Int,
    ): FinancialTickResult {
        val weeklyWageM = club.totalWeeklyWageK / 1000.0
        // Balanced matchday commercial & gate revenue scales realistically by club tier & prestige
        val baseGateM = when (club.tier) {
            1 -> (club.prestige / 100.0) * 1.45
            2 -> 0.24
            3 -> 0.08
            4 -> 0.04
            else -> 0.015
        }
        val netM = (((baseGateM - weeklyWageM * 0.45) * 100.0).roundToInt() / 100.0)
        val updatedBudget = ((club.budgetM + netM).coerceAtLeast(0.0) * 100.0).roundToInt() / 100.0

        val pos = sortedStandings.indexOfFirst { it.clubId == club.id }.let { if (it == -1) 4 else it + 1 }
        val teamCount = sortedStandings.size.coerceAtLeast(6)
        val deltaConf = when {
            pos <= 2 -> +2
            pos <= teamCount / 2 -> +1
            division != null && division.relegateSpots > 0 && pos > teamCount - division.relegateSpots -> -3
            else -> -1
        }
        val newConf = (currentConfidence + deltaConf).coerceIn(20, 99)

        val warning = when {
            newConf < 45 && currentConfidence >= 45 -> InboxMessage(
                id = "board_warn_$matchday",
                category = InboxCategory.BOARD,
                title = "Board Confidence Review",
                body = "The ${club.name} board noted recent league form (#$pos). Immediate points are expected to restore confidence.",
                matchdayLabel = "MD $matchday",
            )
            updatedBudget < weeklyWageM * 2 && matchday % 3 == 0 -> InboxMessage(
                id = "fin_warn_$matchday",
                category = InboxCategory.BOARD,
                title = "Wage Bill Advisory",
                body = "Available transfer funds are tight relative to your weekly payroll (€${club.totalWeeklyWageK}k/wk). Consider player sales or youth promotions.",
                matchdayLabel = "MD $matchday",
            )
            else -> null
        }

        return FinancialTickResult(
            updatedBudgetM = updatedBudget,
            netCashflowM = netM,
            newBoardConfidence = newConf,
            boardWarningMessage = warning,
        )
    }
}
