package com.example.data.world

import com.example.model.Player
import com.example.model.Position
import com.example.model.SquadRole

/**
 * Modular real-world transfer market catalog spanning every nation and rating tier:
 * - Grassroots & Non-League (45-58 OVR, €20K-€300K)
 * - Tier 2-3 & Emerging Markets (60-74 OVR, €400K-€8M)
 * - Global Top Flight & Elite Stars (77-88 OVR, €12M-€95M)
 */
object TransferMarketCatalog {

    private data class MarketEntry(
        val seed: RealPlayerSeed,
        val role: SquadRole,
        val sellingClubId: String?,
        val sellingClubName: String,
    )

    private fun m(
        name: String,
        pos: Position,
        ovr: Int,
        age: Int,
        nat: String,
        role: SquadRole,
        clubId: String?,
        clubName: String,
    ) = MarketEntry(
        seed = RealPlayerSeed(name, pos, ovr, age, nat),
        role = role,
        sellingClubId = clubId,
        sellingClubName = clubName,
    )

    fun createMarketPool(): List<Player> {
        val entries = listOf(
            // Elite & Top-Flight Stars (78-88 OVR)
            m("Viktor Gyokeres", Position.FWD, 86, 26, "SWE", SquadRole.CRUCIAL, "scp_por", "Sporting CP"),
            m("Alexander Isak", Position.FWD, 87, 25, "SWE", SquadRole.CRUCIAL, "new", "Newcastle United"),
            m("Benjamin Sesko", Position.FWD, 82, 21, "SVN", SquadRole.IMPORTANT, "rbl", "RB Leipzig"),
            m("Victor Osimhen", Position.FWD, 87, 25, "NGA", SquadRole.CRUCIAL, "gal", "Galatasaray"),
            m("Jonathan David", Position.FWD, 83, 24, "CAN", SquadRole.CRUCIAL, "losc", "Lille OSC"),
            m("Liam Delap", Position.FWD, 78, 21, "ENG", SquadRole.IMPORTANT, "ips", "Ipswich Town"),
            m("Matheus Cunha", Position.FWD, 82, 25, "BRA", SquadRole.CRUCIAL, "wol", "Wolverhampton"),
            m("Bryan Mbeumo", Position.FWD, 82, 25, "CMR", SquadRole.CRUCIAL, "bre", "Brentford"),
            m("Eberechi Eze", Position.MID, 82, 26, "ENG", SquadRole.CRUCIAL, "cry", "Crystal Palace"),
            m("Morgan Gibbs-White", Position.MID, 81, 24, "ENG", SquadRole.CRUCIAL, "nfo", "Nottingham Forest"),
            m("Adam Wharton", Position.MID, 79, 20, "ENG", SquadRole.IMPORTANT, "cry", "Crystal Palace"),
            m("Martin Zubimendi", Position.MID, 84, 25, "ESP", SquadRole.CRUCIAL, "rso", "Real Sociedad"),
            m("Arda Guler", Position.MID, 80, 19, "TUR", SquadRole.ROTATION, "rma", "Real Madrid"),
            m("Tijjani Reijnders", Position.MID, 84, 26, "NED", SquadRole.CRUCIAL, "acm", "AC Milan"),
            m("Marc Guehi", Position.DEF, 83, 24, "ENG", SquadRole.CRUCIAL, "cry", "Crystal Palace"),
            m("Jarrad Branthwaite", Position.DEF, 81, 22, "ENG", SquadRole.IMPORTANT, "eve", "Everton"),
            m("Milos Kerkez", Position.DEF, 79, 21, "HUN", SquadRole.IMPORTANT, "bou_eng", "AFC Bournemouth"),
            m("Antonee Robinson", Position.DEF, 81, 27, "USA", SquadRole.IMPORTANT, "ful", "Fulham"),
            m("Diogo Costa", Position.GK, 85, 25, "POR", SquadRole.CRUCIAL, "fcp", "FC Porto"),
            m("Giorgi Mamardashvili", Position.GK, 84, 24, "GEO", SquadRole.CRUCIAL, "val", "Valencia"),
            m("Bart Verbruggen", Position.GK, 79, 22, "NED", SquadRole.IMPORTANT, "bha", "Brighton"),
            m("Geovany Quenda", Position.FWD, 78, 17, "POR", SquadRole.PROSPECT, "scp_por", "Sporting CP"),
            m("Orkun Kokcu", Position.MID, 80, 23, "TUR", SquadRole.IMPORTANT, "slb", "Benfica"),
            m("Joey Veerman", Position.MID, 80, 26, "NED", SquadRole.IMPORTANT, "psv", "PSV Eindhoven"),
            m("Jorrel Hato", Position.DEF, 79, 18, "NED", SquadRole.IMPORTANT, "aja", "Ajax"),
            m("Gerson", Position.MID, 80, 27, "BRA", SquadRole.CRUCIAL, "fla", "Flamengo"),
            m("Estevao", Position.FWD, 79, 17, "BRA", SquadRole.IMPORTANT, "pal_bra", "Palmeiras"),
            m("Thiago Almada", Position.MID, 80, 23, "ARG", SquadRole.IMPORTANT, "lyon", "Olympique Lyonnais"),
            m("Riqui Puig", Position.MID, 80, 25, "ESP", SquadRole.CRUCIAL, "lag", "LA Galaxy"),
            m("Baris Alper Yilmaz", Position.FWD, 79, 24, "TUR", SquadRole.IMPORTANT, "gal", "Galatasaray"),

            // Mid-Tier, Americas, Asia/Africa & Tier 2-3 Targets (64-77 OVR)
            m("Jobe Bellingham", Position.MID, 74, 19, "ENG", SquadRole.IMPORTANT, "sun", "Sunderland"),
            m("Finn Azaz", Position.MID, 74, 24, "IRL", SquadRole.IMPORTANT, "mid", "Middlesbrough"),
            m("Shea Charles", Position.MID, 72, 21, "NIR", SquadRole.ROTATION, "shw", "Sheffield Wednesday"),
            m("Ronnie Edwards", Position.DEF, 70, 21, "ENG", SquadRole.PROSPECT, "sou_eng", "Southampton"),
            m("Ashley Phillips", Position.DEF, 69, 19, "ENG", SquadRole.PROSPECT, "sto", "Stoke City"),
            m("Richard Kone", Position.FWD, 68, 21, "CIV", SquadRole.CRUCIAL, "wyc", "Wycombe Wanderers"),
            m("Louie Barry", Position.FWD, 69, 21, "ENG", SquadRole.IMPORTANT, "stk", "Stockport County"),
            m("Matteo Tramoni", Position.MID, 73, 24, "FRA", SquadRole.IMPORTANT, "pis", "Pisa"),
            m("Saud Abdulhamid", Position.DEF, 74, 25, "KSA", SquadRole.ROTATION, "rom", "AS Roma"),
            m("Faisal Al-Ghamdi", Position.MID, 71, 23, "KSA", SquadRole.ROTATION, "al-ittihad", "Al-Ittihad"),
            m("Abdullah Radif", Position.FWD, 69, 21, "KSA", SquadRole.PROSPECT, "al-hilal", "Al-Hilal"),
            m("Marek Rodak", Position.GK, 74, 27, "SVK", SquadRole.IMPORTANT, "al-ettifaq", "Al-Ettifaq"),
            m("Kwame Poku", Position.FWD, 69, 23, "GHA", SquadRole.CRUCIAL, "pet", "Peterborough United"),
            m("Ethan Galbraith", Position.MID, 67, 23, "NIR", SquadRole.IMPORTANT, "ley", "Leyton Orient"),
            m("Thelo Aasgaard", Position.MID, 68, 22, "NOR", SquadRole.IMPORTANT, "lut", "Luton Town"),
            m("Franco Mastantuono", Position.MID, 75, 17, "ARG", SquadRole.IMPORTANT, "riv", "River Plate"),
            m("Kevin Zenon", Position.MID, 76, 23, "ARG", SquadRole.IMPORTANT, "boc", "Boca Juniors"),
            m("Valentin Gomez", Position.DEF, 75, 21, "ARG", SquadRole.CRUCIAL, "vel", "Velez Sarsfield"),
            m("Igor Jesus", Position.FWD, 77, 23, "BRA", SquadRole.CRUCIAL, "bot", "Botafogo"),
            m("Gabriel Pec", Position.FWD, 76, 23, "BRA", SquadRole.IMPORTANT, "lag", "LA Galaxy"),
            m("Patrick Schulte", Position.GK, 74, 23, "USA", SquadRole.IMPORTANT, "clb", "Columbus Crew"),
            m("Semih Kilicsoy", Position.FWD, 76, 19, "TUR", SquadRole.IMPORTANT, "bes", "Besiktas"),
            m("Emam Ashour", Position.MID, 77, 26, "EGY", SquadRole.CRUCIAL, "ahl_egy", "Al Ahly SC"),
            m("Ibrahim Adel", Position.FWD, 74, 23, "EGY", SquadRole.CRUCIAL, "pyr", "Pyramids FC"),
            m("Ali Jasim", Position.FWD, 71, 20, "IRQ", SquadRole.PROSPECT, "tal_irq", "Al-Talaba"),
            m("Aymen Hussein", Position.FWD, 73, 28, "IRQ", SquadRole.CRUCIAL, "qwj", "Al-Quwa Al-Jawiya"),
            m("Akram Afif", Position.FWD, 77, 28, "QAT", SquadRole.CRUCIAL, "sad", "Al Sadd"),
            m("Soufiane Rahimi", Position.FWD, 77, 28, "MAR", SquadRole.CRUCIAL, "ain", "Al Ain"),
            m("Keisuke Osako", Position.GK, 74, 25, "JPN", SquadRole.IMPORTANT, "hir", "Sanfrecce Hiroshima"),
            m("Yuma Suzuki", Position.FWD, 74, 28, "JPN", SquadRole.CRUCIAL, "kas", "Kashima Antlers"),
            m("Yang Min-hyeok", Position.FWD, 73, 18, "KOR", SquadRole.PROSPECT, "gan", "Gangwon FC"),
            m("Lee Dong-gyeong", Position.MID, 73, 27, "KOR", SquadRole.IMPORTANT, "gim_kor", "Gimcheon Sangmu"),
            m("Troy Parrott", Position.FWD, 75, 22, "IRL", SquadRole.IMPORTANT, "az", "AZ Alkmaar"),
            m("Tomas Araujo", Position.DEF, 77, 22, "POR", SquadRole.IMPORTANT, "slb", "Benfica"),

            // Lower-League & Grassroots Gems (45-62 OVR, €25K - €450K)
            m("Ollie Pearce", Position.FWD, 60, 28, "ENG", SquadRole.CRUCIAL, "yrk", "York City"),
            m("Matt Dennis", Position.FWD, 58, 22, "ENG", SquadRole.IMPORTANT, "old", "Oldham Athletic"),
            m("Nick Tsaroulla", Position.DEF, 59, 25, "CYP", SquadRole.IMPORTANT, "not", "Notts County"),
            m("Joe Kizzi", Position.DEF, 56, 30, "ENG", SquadRole.IMPORTANT, "sut", "Sutton United"),
            m("Finlay Sinclair-Smith", Position.FWD, 53, 24, "ENG", SquadRole.IMPORTANT, "mar", "Marine"),
            m("Olly Box", Position.MID, 53, 19, "ENG", SquadRole.PROSPECT, "dar", "Dartford"),
            m("Sean Doyle", Position.FWD, 49, 26, "IOM", SquadRole.CRUCIAL, "iom", "FC Isle of Man"),
            m("Lorne Bickley", Position.FWD, 50, 25, "JEY", SquadRole.CRUCIAL, "jer", "Jersey Bulls"),
            m("Noah Adejokun", Position.FWD, 47, 24, "ENG", SquadRole.IMPORTANT, "cla", "Clapton CFC"),
            m("Nicky Meta", Position.FWD, 46, 25, "ENG", SquadRole.IMPORTANT, "pec", "Peckham Town"),
            m("Jack Francis", Position.GK, 46, 26, "ENG", SquadRole.IMPORTANT, "cla", "Clapton CFC"),
            m("Toby Bull", Position.GK, 47, 22, "ENG", SquadRole.IMPORTANT, "too", "Tooting & Mitcham"),
            m("Sam King", Position.MID, 49, 24, "ENG", SquadRole.IMPORTANT, "red", "Redhill"),
            m("Owen Dean", Position.FWD, 51, 23, "ENG", SquadRole.IMPORTANT, "far", "Farnham Town"),
            m("Rustam Stepans", Position.FWD, 52, 23, "LVA", SquadRole.IMPORTANT, "bry", "Bury"),
            m("Rakan Al-Shamlan", Position.FWD, 61, 26, "KSA", SquadRole.IMPORTANT, "ada", "Al-Adalah"),
            m("Abdulaziz Al-Bishi", Position.FWD, 62, 30, "KSA", SquadRole.ROTATION, "dam", "Damac"),
            m("Mohammed Al-Qahtani", Position.MID, 62, 22, "KSA", SquadRole.PROSPECT, "al-hilal", "Al-Hilal"),
        )

        return entries.mapIndexed { idx, entry ->
            RealPlayerRosterBuilder.buildPlayer(
                id = "mkt_real_${idx + 1}",
                seed = entry.seed,
                role = entry.role,
                sellingClubId = entry.sellingClubId,
                sellingClubName = entry.sellingClubName,
            )
        }
    }
}
