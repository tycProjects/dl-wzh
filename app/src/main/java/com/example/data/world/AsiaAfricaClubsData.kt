package com.example.data.world

import com.example.data.badges.ClubSvgBadges.defaultSpec
import com.example.model.Club
import com.example.model.CountryCode
import com.example.model.CrestPattern
import com.example.model.CrestShape
import com.example.model.CrestSymbol
import com.example.model.DivisionInfo
import com.example.model.Formation
import com.example.model.Position

/**
 * Modular Real-World Asia & Africa Leagues:
 * - Iraq: Iraq Stars League
 * - UAE: UAE Pro League
 * - Qatar: Qatar Stars League
 * - Egypt: Egyptian Premier League
 * - Japan: J1 League
 * - South Korea: K League 1
 * - AFC International (West & East) real clubs for AFC Champions League Elite (West vs East).
 */
object AsiaAfricaClubsData {

    val divisions: List<DivisionInfo> = listOf(
        DivisionInfo("irq_t1", "Iraq Stars League", CountryCode.IRQ, 1, 0, 0, 0, aclSpots = 1),
        DivisionInfo("uae_t1", "UAE Pro League", CountryCode.UAE, 1, 0, 0, 0, aclSpots = 2),
        DivisionInfo("qat_t1", "Qatar Stars League", CountryCode.QAT, 1, 0, 0, 0, aclSpots = 2),
        DivisionInfo("egy_t1", "Egyptian Premier League", CountryCode.EGY, 1, 0, 0, 0),
        DivisionInfo("jpn_t1", "J1 League", CountryCode.JPN, 1, 0, 0, 0, aclSpots = 3),
        DivisionInfo("kor_t1", "K League 1", CountryCode.KOR, 1, 0, 0, 0, aclSpots = 3),
    )

    private fun p(name: String, pos: Position, ovr: Int, age: Int, nat: String) =
        RealPlayerSeed(name, pos, ovr, age, nat)

    private val westAsiaPool = listOf(
        p("Jalal Hassan", Position.GK, 71, 33, "IRQ"), p("Ahmed Basil", Position.GK, 68, 28, "IRQ"),
        p("Khalid Eisa", Position.GK, 75, 35, "UAE"), p("Meshaal Barsham", Position.GK, 74, 26, "QAT"),
        p("Salah Zakaria", Position.GK, 71, 25, "QAT"), p("Ali Khaseif", Position.GK, 72, 37, "UAE"),
        p("Saad Natiq", Position.DEF, 69, 30, "IRQ"), p("Rebin Sulaka", Position.DEF, 69, 32, "IRQ"),
        p("Munaf Younus", Position.DEF, 67, 28, "IRQ"), p("Lucas Mendes", Position.DEF, 73, 34, "QAT"),
        p("Boualem Khoukhi", Position.DEF, 73, 34, "QAT"), p("Khalifa Al-Hammadi", Position.DEF, 72, 26, "UAE"),
        p("Khalid Al-Hashemi", Position.DEF, 71, 27, "UAE"), p("Giorgi Gvelesiani", Position.DEF, 72, 33, "GEO"),
        p("Amjad Attwan", Position.MID, 70, 27, "IRQ"), p("Aymen Hussein", Position.FWD, 73, 28, "IRQ"),
        p("Mohanad Ali", Position.FWD, 71, 24, "IRQ"), p("Alaa Abbas", Position.FWD, 67, 27, "IRQ"),
        p("Akram Afif", Position.FWD, 79, 28, "QAT"), p("Almoez Ali", Position.FWD, 75, 28, "QAT"),
        p("Soufiane Rahimi", Position.FWD, 79, 28, "MAR"), p("Kaku", Position.MID, 76, 29, "PAR"),
        p("Fabio Lima", Position.MID, 74, 31, "UAE"), p("Caio Canedo", Position.FWD, 73, 34, "UAE"),
        p("Sardar Azmoun", Position.FWD, 79, 29, "IRN"), p("Mehdi Torabi", Position.MID, 73, 30, "IRN"),
        p("Oston Urunov", Position.MID, 74, 23, "UZB"), p("Igor Sergeev", Position.FWD, 72, 31, "UZB"),
        p("Michael Olunga", Position.FWD, 76, 30, "KEN"), p("Joselu", Position.FWD, 79, 34, "ESP"),
    )

    private val egyptPool = listOf(
        p("Mohamed El-Shenawy", Position.GK, 77, 35, "EGY"), p("Mohamed Sobhy", Position.GK, 71, 25, "EGY"),
        p("Ahmed El-Shenawy", Position.GK, 73, 33, "EGY"), p("Mostafa Shobeir", Position.GK, 73, 24, "EGY"),
        p("Mohamed Abdelmonem", Position.DEF, 76, 25, "EGY"), p("Ramy Rabia", Position.DEF, 73, 31, "EGY"),
        p("Mohamed Hany", Position.DEF, 72, 28, "EGY"), p("Yahia Attiat-Allah", Position.DEF, 74, 29, "MAR"),
        p("Hossam Abdelmaguid", Position.DEF, 72, 23, "EGY"), p("Ali Gabr", Position.DEF, 71, 35, "EGY"),
        p("Emam Ashour", Position.MID, 77, 26, "EGY"), p("Marwan Attia", Position.MID, 74, 26, "EGY"),
        p("Afsha", Position.MID, 73, 28, "EGY"), p("Zizo", Position.MID, 77, 28, "EGY"),
        p("Nabil Emad Donga", Position.MID, 72, 28, "EGY"), p("Blati Toure", Position.MID, 73, 30, "BFA"),
        p("Wessam Abou Ali", Position.FWD, 76, 25, "PLE"), p("Hussein El Shahat", Position.FWD, 74, 32, "EGY"),
        p("Seifeddine Jaziri", Position.FWD, 72, 31, "TUN"), p("Fiston Mayele", Position.FWD, 74, 30, "COD"),
        p("Ramadan Sobhi", Position.FWD, 73, 27, "EGY"), p("Ibrahim Adel", Position.FWD, 74, 23, "EGY"),
        p("Mabululu", Position.FWD, 72, 32, "ANG"), p("Karim Bambo", Position.FWD, 70, 31, "EGY"),
    )

    private val eastAsiaPool = listOf(
        p("Daiya Maekawa", Position.GK, 73, 30, "JPN"), p("Keisuke Osako", Position.GK, 74, 25, "JPN"),
        p("Shusaku Nishikawa", Position.GK, 73, 38, "JPN"), p("Jo Hyeon-woo", Position.GK, 76, 33, "KOR"),
        p("Song Bum-keun", Position.GK, 72, 27, "KOR"), p("Hwang In-jae", Position.GK, 71, 30, "KOR"),
        p("Gotoku Sakai", Position.DEF, 72, 33, "JPN"), p("Shinnosuke Hatanaka", Position.DEF, 72, 29, "JPN"),
        p("Marius Hoibraten", Position.DEF, 73, 29, "NOR"), p("Kim Young-gwon", Position.DEF, 74, 34, "KOR"),
        p("Park Jin-seob", Position.DEF, 72, 29, "KOR"), p("Yazan Al-Arab", Position.DEF, 72, 28, "JOR"),
        p("Takahiro Ogihara", Position.MID, 72, 33, "JPN"), p("Hotaru Yamaguchi", Position.MID, 72, 34, "JPN"),
        p("Yasuto Wakizaka", Position.MID, 74, 29, "JPN"), p("Gaku Shibasaki", Position.MID, 72, 32, "JPN"),
        p("Ki Sung-yueng", Position.MID, 73, 35, "KOR"), p("Jesse Lingard", Position.MID, 75, 31, "ENG"),
        p("Lee Dong-gyeong", Position.MID, 73, 27, "KOR"), p("Oscar", Position.MID, 78, 33, "BRA"),
        p("Yuya Osako", Position.FWD, 76, 34, "JPN"), p("Yoshinori Muto", Position.FWD, 75, 32, "JPN"),
        p("Anderson Lopes", Position.FWD, 76, 31, "BRA"), p("Yuma Suzuki", Position.FWD, 74, 28, "JPN"),
        p("Mutsuki Kato", Position.FWD, 72, 27, "JPN"), p("Joo Min-kyu", Position.FWD, 74, 34, "KOR"),
        p("Tiago Orobo", Position.FWD, 72, 31, "BRA"), p("Supachai Chaided", Position.FWD, 71, 26, "THA"),
        p("Bergson", Position.FWD, 74, 33, "BRA"), p("Matias Vargas", Position.MID, 75, 27, "ARG"),
    )

    fun createClubs(): List<Club> {
        val irq = createIraqSeeds().map { RealPlayerRosterBuilder.buildClub(it, westAsiaPool) }
        val uae = createUaeSeeds().map { RealPlayerRosterBuilder.buildClub(it, westAsiaPool) }
        val qat = createQatarSeeds().map { RealPlayerRosterBuilder.buildClub(it, westAsiaPool) }
        val egy = createEgyptSeeds().map { RealPlayerRosterBuilder.buildClub(it, egyptPool) }
        val jpn = createJapanSeeds().map { RealPlayerRosterBuilder.buildClub(it, eastAsiaPool) }
        val kor = createKoreaSeeds().map { RealPlayerRosterBuilder.buildClub(it, eastAsiaPool) }
        val afcOther = createAfcOtherSeeds().map {
            val pool = if (it.divisionId == "afc_west_pool") westAsiaPool else eastAsiaPool
            RealPlayerRosterBuilder.buildClub(it, pool)
        }
        return irq + uae + qat + egy + jpn + kor + afcOther
    }

    private fun createIraqSeeds(): List<RealClubSeed> = listOf(
        RealClubSeed("sho_irq", "Al-Shorta", "SHO", CountryCode.IRQ, "irq_t1", 1, "Baghdad", "Al-Shaab Stadium", "#00843D", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FACC15"), 4.8, 69, Formation.F4231, listOf(p("Ahmed Basil", Position.GK, 68, 28, "IRQ"), p("Alaa Abdul-Zahra", Position.MID, 69, 36, "IRQ"), p("Mohanad Ali", Position.FWD, 71, 24, "IRQ"))),
        RealClubSeed("qwj", "Al-Quwa Al-Jawiya", "QWJ", CountryCode.IRQ, "irq_t1", 1, "Baghdad", "Al-Madina Stadium", "#0055A5", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FACC15"), 4.6, 69, Formation.F433, listOf(p("Saad Natiq", Position.DEF, 69, 30, "IRQ"), p("Aymen Hussein", Position.FWD, 73, 28, "IRQ"))),
        RealClubSeed("zaw", "Al-Zawraa", "ZAW", CountryCode.IRQ, "irq_t1", 1, "Baghdad", "Al-Zawraa Stadium", "#FFFFFF", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.EAGLE, "#111111"), 4.5, 68, Formation.F4231, listOf(p("Jalal Hassan", Position.GK, 71, 33, "IRQ"), p("Hasan Abdulkareem", Position.MID, 69, 25, "IRQ"))),
        RealClubSeed("tal_irq", "Al-Talaba", "TAL", CountryCode.IRQ, "irq_t1", 1, "Baghdad", "Al-Madina Stadium", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 3.2, 66, Formation.F442, listOf(p("Ali Jasim", Position.FWD, 71, 20, "IRQ"))),
        RealClubSeed("erb", "Erbil SC", "ERB", CountryCode.IRQ, "irq_t1", 1, "Erbil", "Franso Hariri", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#111111"), 2.8, 65, Formation.F433, listOf(p("Sherko Kareem", Position.FWD, 67, 28, "IRQ"))),
        RealClubSeed("duh", "Duhok SC", "DUH", CountryCode.IRQ, "irq_t1", 1, "Duhok", "Duhok Stadium", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.EAGLE, "#FFFFFF"), 3.0, 66, Formation.F4231, listOf(p("Pashang Abdulla", Position.FWD, 67, 30, "IRQ"))),
        RealClubSeed("zak", "Zakho SC", "ZAK", CountryCode.IRQ, "irq_t1", 1, "Zakho", "Zakho International", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 2.9, 66, Formation.F4231, listOf(p("Amjad Attwan", Position.MID, 70, 27, "IRQ"))),
        RealClubSeed("naj_irq", "Al-Najaf", "NAJ", CountryCode.IRQ, "irq_t1", 1, "Najaf", "Al-Najaf International", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.CRESCENT, "#FACC15"), 2.6, 65, Formation.F442, listOf(p("Mohammed Qasim", Position.MID, 68, 27, "IRQ"))),
        // Additional Iraq Stars League clubs (8 -> 20)
        RealClubSeed("irq1_min", "Al-Minaa", "MIN", CountryCode.IRQ, "irq_t1", 1, "Basra", "Al-Minaa Olympic", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 2.4, 64, Formation.F442, listOf(p("Alaa Khalil", Position.GK, 64, 30, "IRQ"))),
        RealClubSeed("irq1_naf", "Al-Naft", "NAF", CountryCode.IRQ, "irq_t1", 1, "Baghdad", "Al-Saher Stadium", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.FLAME, "#FACC15"), 2.5, 65, Formation.F4231, listOf(p("Sadeq Zamel", Position.DEF, 65, 25, "IRQ"))),
        RealClubSeed("irq1_kar", "Al-Kahrabaa", "KHR", CountryCode.IRQ, "irq_t1", 1, "Baghdad", "Al-Zawraa Stadium", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.BOLT, "#111111"), 2.5, 65, Formation.F4231, listOf(p("Murtadha Hlayel", Position.MID, 65, 24, "IRQ"))),
        RealClubSeed("irq1_kark", "Al-Karkh", "KRK", CountryCode.IRQ, "irq_t1", 1, "Baghdad", "Al-Saher Stadium", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.EAGLE, "#111111"), 2.2, 64, Formation.F442, listOf(p("Hassan Ahmed", Position.MID, 64, 24, "IRQ"))),
        RealClubSeed("irq1_hud", "Al-Hudood", "HUD", CountryCode.IRQ, "irq_t1", 1, "Baghdad", "Al-Shaab Stadium", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 2.1, 64, Formation.F442, listOf(p("Suhaib Raad", Position.DEF, 64, 26, "IRQ"))),
        RealClubSeed("irq1_naw", "Newroz SC", "NEW", CountryCode.IRQ, "irq_t1", 1, "Sulaymaniyah", "Newroz Stadium", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.FLAME, "#FACC15"), 2.5, 65, Formation.F4231, listOf(p("Marwan Hussein", Position.FWD, 66, 32, "IRQ"))),
        RealClubSeed("irq1_nmi", "Naft Maysan", "NMY", CountryCode.IRQ, "irq_t1", 1, "Amarah", "Maysan Olympic", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"), 2.2, 64, Formation.F442, listOf(p("Alaa Raad", Position.FWD, 64, 26, "IRQ"))),
        RealClubSeed("irq1_nba", "Naft Al-Basra", "NBS", CountryCode.IRQ, "irq_t1", 1, "Basra", "Basra Sports City", "#DC2626", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.FLAME, "#FACC15"), 2.1, 64, Formation.F442, listOf(p("Haidar Sari", Position.MID, 64, 27, "IRQ"))),
        RealClubSeed("irq1_karb", "Karbala SC", "KRB", CountryCode.IRQ, "irq_t1", 1, "Karbala", "Karbala International", "#FFFFFF", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CRESCENT, "#16A34A"), 2.1, 64, Formation.F442, listOf(p("Mustafa Radhi", Position.FWD, 64, 27, "IRQ"))),
        RealClubSeed("irq1_qas", "Al-Qasim", "QAS", CountryCode.IRQ, "irq_t1", 1, "Babil", "Al-Najaf Stadium", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 2.0, 63, Formation.F442, listOf(p("Al-Harith Hatem", Position.MID, 64, 23, "IRQ"))),
        RealClubSeed("irq1_krm", "Al-Karma", "KRM", CountryCode.IRQ, "irq_t1", 1, "Ramadi", "Al-Ramadi Stadium", "#F97316", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 2.4, 64, Formation.F4231, listOf(p("Aymen Mahmoud", Position.DEF, 65, 28, "TUN"))),
        RealClubSeed("irq1_diy", "Diyala SC", "DIY", CountryCode.IRQ, "irq_t1", 1, "Baqubah", "Al-Madina Stadium", "#F97316", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LAUREL, "#FFFFFF"), 2.0, 63, Formation.F442, listOf(p("Muntadher Sattar", Position.MID, 63, 24, "IRQ"))),
    )

    private fun createUaeSeeds(): List<RealClubSeed> = listOf(
        RealClubSeed("ain", "Al Ain", "AIN", CountryCode.UAE, "uae_t1", 1, "Al Ain", "Hazza bin Zayed", "#7C3AED", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 32.0, 77, Formation.F4231, listOf(p("Khalid Eisa", Position.GK, 75, 35, "UAE"), p("Soufiane Rahimi", Position.FWD, 79, 28, "MAR"), p("Kaku", Position.MID, 76, 29, "PAR"), p("Kodjo Laba", Position.FWD, 76, 32, "TOG"))),
        RealClubSeed("saa_uae", "Shabab Al Ahli", "SAA", CountryCode.UAE, "uae_t1", 1, "Dubai", "Rashid Stadium", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FACC15"), 28.0, 76, Formation.F4231, listOf(p("Sardar Azmoun", Position.FWD, 79, 29, "IRN"), p("Saeid Ezatolahi", Position.MID, 75, 28, "IRN"))),
        RealClubSeed("was", "Al Wasl", "WAS", CountryCode.UAE, "uae_t1", 1, "Dubai", "Zabeel Stadium", "#FACC15", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#111111"), 24.0, 75, Formation.F4231, listOf(p("Fabio Lima", Position.MID, 74, 31, "UAE"), p("Haris Seferovic", Position.FWD, 74, 32, "SUI"))),
        RealClubSeed("jaz", "Al Jazira", "JAZ", CountryCode.UAE, "uae_t1", 1, "Abu Dhabi", "Mohammed bin Zayed", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#DC2626"), 22.0, 74, Formation.F433, listOf(p("Ali Khaseif", Position.GK, 72, 37, "UAE"), p("Mohamed Elneny", Position.MID, 75, 32, "EGY"), p("Nabil Fekir", Position.MID, 78, 31, "FRA"))),
        RealClubSeed("shj", "Sharjah FC", "SHJ", CountryCode.UAE, "uae_t1", 1, "Sharjah", "Sharjah Stadium", "#FFFFFF", "#D71920", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.CROWN, "#D71920"), 20.0, 74, Formation.F4231, listOf(p("Adel Taarabt", Position.MID, 74, 35, "MAR"), p("Caio Lucas", Position.FWD, 74, 30, "BRA"))),
        RealClubSeed("wah_uae", "Al Wahda", "WAH", CountryCode.UAE, "uae_t1", 1, "Abu Dhabi", "Al Nahyan Stadium", "#7F1D1D", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FACC15"), 18.0, 73, Formation.F4231, listOf(p("Omar Khribin", Position.FWD, 74, 30, "SYR"))),
        RealClubSeed("nas_uae", "Al Nasr Dubai", "NAS", CountryCode.UAE, "uae_t1", 1, "Dubai", "Al Maktoum Stadium", "#0055A5", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 16.0, 72, Formation.F433, listOf(p("Manolo Gabbiadini", Position.FWD, 74, 32, "ITA"))),
        RealClubSeed("kal_uae", "Kalba FC", "KAL", CountryCode.UAE, "uae_t1", 1, "Kalba", "Ittihad Kalba Stadium", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#111111"), 10.0, 70, Formation.F442, listOf(p("Mehdi Ghayedi", Position.FWD, 73, 25, "IRN"))),
        // Additional UAE Pro League clubs (8 -> 14)
        RealClubSeed("uae1_ajm", "Ajman Club", "AJM", CountryCode.UAE, "uae_t1", 1, "Ajman", "Rashid bin Saeed", "#F97316", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 8.5, 69, Formation.F4231, listOf(p("Walid Azaro", Position.FWD, 71, 29, "MAR"))),
        RealClubSeed("uae1_ban", "Baniyas", "BAN", CountryCode.UAE, "uae_t1", 1, "Abu Dhabi", "Baniyas Stadium", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 9.0, 70, Formation.F4231, listOf(p("Youssoufou Niakate", Position.FWD, 71, 31, "FRA"))),
        RealClubSeed("uae1_bat", "Al Bataeh", "BAT", CountryCode.UAE, "uae_t1", 1, "Sharjah", "Khalid bin Mohammed", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 8.0, 69, Formation.F442, listOf(p("Anatole Abang", Position.FWD, 70, 28, "CMR"))),
        RealClubSeed("uae1_kho", "Khor Fakkan", "KHO", CountryCode.UAE, "uae_t1", 1, "Khor Fakkan", "Saqr bin Mohammad", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 8.0, 69, Formation.F4231, listOf(p("Lourency", Position.FWD, 70, 28, "BRA"))),
        RealClubSeed("uae1_dib", "Dibba Al-Hisn", "DAH", CountryCode.UAE, "uae_t1", 1, "Dibba Al-Hisn", "Dibba Stadium", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 6.5, 68, Formation.F442, listOf(p("Ibrahima Cisse", Position.MID, 69, 30, "GUI"))),
        RealClubSeed("uae1_oro", "Al Urooba", "URO", CountryCode.UAE, "uae_t1", 1, "Fujairah", "Al Urooba Stadium", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 6.0, 67, Formation.F442, listOf(p("Bubacarr Trawally", Position.FWD, 68, 30, "GAM"))),
    )

    private fun createQatarSeeds(): List<RealClubSeed> = listOf(
        RealClubSeed("sad", "Al Sadd", "SAD", CountryCode.QAT, "qat_t1", 1, "Doha", "Jassim bin Hamad", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#F59E0B"), 35.0, 77, Formation.F433, listOf(p("Meshaal Barsham", Position.GK, 74, 26, "QAT"), p("Akram Afif", Position.FWD, 79, 28, "QAT"), p("Mateus Uribe", Position.MID, 76, 33, "COL"), p("Claudinho", Position.MID, 78, 27, "BRA"))),
        RealClubSeed("duh_qat", "Al Duhail", "DUH", CountryCode.QAT, "qat_t1", 1, "Doha", "Abdullah bin Khalifa", "#D71920", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FACC15"), 32.0, 76, Formation.F4231, listOf(p("Luis Alberto", Position.MID, 80, 32, "ESP"), p("Edmilson Junior", Position.FWD, 75, 30, "QAT"), p("Michael Olunga", Position.FWD, 76, 30, "KEN"))),
        RealClubSeed("gha", "Al Gharafa", "GHA", CountryCode.QAT, "qat_t1", 1, "Al Rayyan", "Thani bin Jassim", "#FACC15", "#0038A8", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HALF_AND_HALF, CrestSymbol.LION, "#FFFFFF"), 24.0, 75, Formation.F4231, listOf(p("Yacine Brahimi", Position.FWD, 77, 34, "ALG"), p("Joselu", Position.FWD, 79, 34, "ESP"), p("Ferjani Sassi", Position.MID, 74, 32, "TUN"))),
        RealClubSeed("ray", "Al Rayyan", "RAY", CountryCode.QAT, "qat_t1", 1, "Al Rayyan", "Ahmad bin Ali", "#D71920", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.LION, "#FFFFFF"), 25.0, 75, Formation.F4231, listOf(p("Roger Guedes", Position.FWD, 78, 28, "BRA"), p("Trezeguet", Position.FWD, 76, 30, "EGY"))),
        RealClubSeed("wak", "Al Wakrah", "WAK", CountryCode.QAT, "qat_t1", 1, "Al Wakrah", "Al Janoub Stadium", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 15.0, 72, Formation.F433, listOf(p("Gelson Dala", Position.FWD, 74, 28, "ANG"))),
        RealClubSeed("ara_qat", "Al Arabi", "ARA", CountryCode.QAT, "qat_t1", 1, "Doha", "Al Thumama", "#D71920", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 20.0, 73, Formation.F4231, listOf(p("Marco Verratti", Position.MID, 81, 32, "ITA"), p("Rodri Sanchez", Position.MID, 74, 24, "ESP"))),
        RealClubSeed("qsc", "Qatar SC", "QSC", CountryCode.QAT, "qat_t1", 1, "Doha", "Suhaim bin Hamad", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#111111"), 14.0, 71, Formation.F4231, listOf(p("Percy Tau", Position.FWD, 73, 30, "RSA"))),
        RealClubSeed("ahl_qat", "Al Ahli Doha", "AHD", CountryCode.QAT, "qat_t1", 1, "Doha", "Hamad bin Khalifa", "#00853E", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 14.0, 71, Formation.F433, listOf(p("Julian Draxler", Position.MID, 76, 31, "GER"))),
        // Additional Qatar Stars League clubs (8 -> 12)
        RealClubSeed("qat1_sha", "Al Shamal", "SHM", CountryCode.QAT, "qat_t1", 1, "Al Shamal", "Al-Bayt Stadium", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 12.0, 70, Formation.F4231, listOf(p("Baghdad Bounedjah", Position.FWD, 75, 33, "ALG"))),
        RealClubSeed("qat1_umm", "Umm Salal", "UMM", CountryCode.QAT, "qat_t1", 1, "Umm Salal", "Grand Hamad", "#F97316", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 11.0, 70, Formation.F4231, listOf(p("Antonio Mance", Position.FWD, 70, 29, "CRO"))),
        RealClubSeed("qat1_kho", "Al Khor", "KHR", CountryCode.QAT, "qat_t1", 1, "Al Khor", "Al Khor Stadium", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 10.0, 69, Formation.F442, listOf(p("Sofiane Hanni", Position.MID, 71, 33, "ALG"))),
        RealClubSeed("qat1_sha2", "Al Shahaniya", "SHH", CountryCode.QAT, "qat_t1", 1, "Al-Shahaniya", "Suhaim bin Hamad", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FACC15"), 10.0, 69, Formation.F4231, listOf(p("Pelle van Amersfoort", Position.MID, 71, 28, "NED"))),
    )

    private fun createEgyptSeeds(): List<RealClubSeed> = listOf(
        RealClubSeed("ahl_egy", "Al Ahly SC", "AHL", CountryCode.EGY, "egy_t1", 1, "Cairo", "Cairo International", "#D71920", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#D4AF37"), 28.0, 76, Formation.F4231, listOf(p("Mohamed El-Shenawy", Position.GK, 77, 35, "EGY"), p("Emam Ashour", Position.MID, 77, 26, "EGY"), p("Wessam Abou Ali", Position.FWD, 76, 25, "PLE"), p("Achraf Bencharki", Position.FWD, 75, 30, "MAR"))),
        RealClubSeed("zam", "Zamalek", "ZAM", CountryCode.EGY, "egy_t1", 1, "Giza", "Cairo International", "#FFFFFF", "#D71920", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HOOPS, CrestSymbol.CROWN, "#D71920"), 18.0, 74, Formation.F433, listOf(p("Zizo", Position.MID, 77, 28, "EGY"), p("Mostafa Shalaby", Position.FWD, 72, 30, "EGY"), p("Seifeddine Jaziri", Position.FWD, 72, 31, "TUN"))),
        RealClubSeed("pyr", "Pyramids FC", "PYR", CountryCode.EGY, "egy_t1", 1, "Cairo", "30 June Stadium", "#1E3A8A", "#38BDF8", defaultSpec(CrestShape.DIAMOND, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 22.0, 74, Formation.F4231, listOf(p("Ahmed El-Shenawy", Position.GK, 73, 33, "EGY"), p("Ibrahim Adel", Position.FWD, 74, 23, "EGY"), p("Fiston Mayele", Position.FWD, 74, 30, "COD"))),
        RealClubSeed("mas_egy", "Al Masry", "MAS", CountryCode.EGY, "egy_t1", 1, "Port Said", "Borg El Arab", "#00853E", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 8.0, 70, Formation.F4231, listOf(p("Salah Mohsen", Position.FWD, 70, 26, "EGY"))),
        RealClubSeed("ism", "Ismaily", "ISM", CountryCode.EGY, "egy_t1", 1, "Ismailia", "Ismailia Stadium", "#FACC15", "#0055A5", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#0055A5"), 5.5, 68, Formation.F433, listOf(p("Marwan Hamdy", Position.FWD, 69, 27, "EGY"))),
        RealClubSeed("itt_egy", "Al Ittihad Alex", "ITA", CountryCode.EGY, "egy_t1", 1, "Alexandria", "Alexandria Stadium", "#00853E", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 6.0, 69, Formation.F442, listOf(p("Mabululu", Position.FWD, 72, 32, "ANG"))),
        RealClubSeed("cer_egy", "Ceramica Cleopatra", "CER", CountryCode.EGY, "egy_t1", 1, "Giza", "Osman Ahmed Osman", "#854D0E", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 7.5, 69, Formation.F4231, listOf(p("Ahmed Kendouci", Position.MID, 72, 25, "ALG"))),
        RealClubSeed("mod_egy", "Modern Sport", "MOD", CountryCode.EGY, "egy_t1", 1, "Cairo", "Al Salam Stadium", "#D71920", "#111111", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.BOLT, "#FFFFFF"), 7.0, 69, Formation.F4231, listOf(p("Mahmoud Abdelaati", Position.MID, 69, 31, "EGY"))),
        // Additional Egyptian Premier League clubs (8 -> 18)
        RealClubSeed("egy1_ban", "National Bank of Egypt", "NBE", CountryCode.EGY, "egy_t1", 1, "Cairo", "Cairo International", "#F97316", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 7.0, 69, Formation.F4231, listOf(p("Osama Faisal", Position.FWD, 70, 23, "EGY"), p("Yaw Annor", Position.FWD, 70, 27, "GHA"))),
        RealClubSeed("egy1_smo", "Smouha", "SMO", CountryCode.EGY, "egy_t1", 1, "Alexandria", "Alexandria Stadium", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 6.0, 68, Formation.F4231, listOf(p("El Hany Soliman", Position.GK, 68, 40, "EGY"), p("Hossam Hassan", Position.FWD, 69, 31, "EGY"))),
        RealClubSeed("egy1_zed", "ZED FC", "ZED", CountryCode.EGY, "egy_t1", 1, "Cairo", "Cairo International", "#16A34A", "#111111", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.BOLT, "#FFFFFF"), 7.5, 69, Formation.F433, listOf(p("Ali Lotfi", Position.GK, 69, 35, "EGY"), p("Shady Hussein", Position.FWD, 69, 31, "EGY"))),
        RealClubSeed("egy1_enp", "ENPPI", "ENP", CountryCode.EGY, "egy_t1", 1, "Cairo", "Petrosport Stadium", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"), 5.5, 68, Formation.F442, listOf(p("Rafik Kabou", Position.FWD, 68, 32, "TUN"))),
        RealClubSeed("egy1_tal", "Tala'ea El Gaish", "TGS", CountryCode.EGY, "egy_t1", 1, "Cairo", "Gehaz El Reyada", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FACC15"), 5.5, 68, Formation.F4231, listOf(p("Godwin Chika", Position.FWD, 68, 25, "NGA"))),
        RealClubSeed("egy1_pha", "Pharco FC", "PHA", CountryCode.EGY, "egy_t1", 1, "Alexandria", "Harras El Hodoud", "#F97316", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 5.2, 68, Formation.F4231, listOf(p("Amr Nasser", Position.FWD, 68, 25, "EGY"))),
        RealClubSeed("egy1_gou", "El Gouna", "GOU", CountryCode.EGY, "egy_t1", 1, "El Gouna", "Khaled Bichara", "#111111", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 5.0, 67, Formation.F442, listOf(p("Mohamed Alaa", Position.GK, 67, 25, "EGY"))),
        RealClubSeed("egy1_har", "Haras El Hodoud", "HEH", CountryCode.EGY, "egy_t1", 1, "Alexandria", "Harras El Hodoud", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 4.8, 67, Formation.F442, listOf(p("Fawzi El Henawy", Position.FWD, 67, 25, "EGY"))),
        RealClubSeed("egy1_pet", "Petrojet", "PTJ", CountryCode.EGY, "egy_t1", 1, "Suez", "Suez Stadium", "#1D4ED8", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"), 5.0, 67, Formation.F4231, listOf(p("Badr Moussa", Position.FWD, 67, 25, "PLE"))),
        RealClubSeed("egy1_mah", "Ghazl El Mahalla", "GEM", CountryCode.EGY, "egy_t1", 1, "El Mahalla", "El Mahalla Stadium", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 4.8, 67, Formation.F442, listOf(p("Abdo Yehia", Position.FWD, 67, 25, "EGY"))),
    )

    private fun createJapanSeeds(): List<RealClubSeed> = listOf(
        RealClubSeed("vis", "Vissel Kobe", "VIS", CountryCode.JPN, "jpn_t1", 1, "Kobe", "Noevir Stadium", "#9E1B32", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 24.0, 75, Formation.F433, listOf(p("Daiya Maekawa", Position.GK, 73, 30, "JPN"), p("Gotoku Sakai", Position.DEF, 72, 33, "JPN"), p("Yoshinori Muto", Position.FWD, 75, 32, "JPN"), p("Yuya Osako", Position.FWD, 76, 34, "JPN"))),
        RealClubSeed("hir", "Sanfrecce Hiroshima", "SAN", CountryCode.JPN, "jpn_t1", 1, "Hiroshima", "Edion Peace Wing", "#5B2C82", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FACC15"), 20.0, 74, Formation.F352, listOf(p("Keisuke Osako", Position.GK, 74, 25, "JPN"), p("Sho Sasaki", Position.DEF, 73, 35, "JPN"), p("Mutsuki Kato", Position.FWD, 72, 27, "JPN"))),
        RealClubSeed("mac_jpn", "Machida Zelvia", "MAC", CountryCode.JPN, "jpn_t1", 1, "Machida", "GION Stadium", "#0038A8", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.BOLT, "#FACC15"), 18.0, 73, Formation.F442, listOf(p("Kosei Tani", Position.GK, 72, 24, "JPN"), p("Yuki Soma", Position.FWD, 73, 27, "JPN"))),
        RealClubSeed("gam", "Gamba Osaka", "GAM", CountryCode.JPN, "jpn_t1", 1, "Suita", "Panasonic Stadium", "#0038A8", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.FLAME, "#FFFFFF"), 18.0, 73, Formation.F4231, listOf(p("Takashi Usami", Position.FWD, 74, 32, "JPN"))),
        RealClubSeed("kas", "Kashima Antlers", "KAS", CountryCode.JPN, "jpn_t1", 1, "Kashima", "Kashima Stadium", "#9E1B32", "#00205B", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 20.0, 74, Formation.F4231, listOf(p("Gaku Shibasaki", Position.MID, 72, 32, "JPN"), p("Yuma Suzuki", Position.FWD, 74, 28, "JPN"))),
        RealClubSeed("yfm", "Yokohama F. Marinos", "YFM", CountryCode.JPN, "jpn_t1", 1, "Yokohama", "Nissan Stadium", "#1D4ED8", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.ANCHOR, "#FFFFFF"), 24.0, 75, Formation.F433, listOf(p("Anderson Lopes", Position.FWD, 76, 31, "BRA"), p("Yan Matheus", Position.FWD, 74, 26, "BRA"))),
        RealClubSeed("kaw_jpn", "Kawasaki Frontale", "KAW", CountryCode.JPN, "jpn_t1", 1, "Kawasaki", "Todoroki Stadium", "#00AEEF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.BOLT, "#FFFFFF"), 22.0, 74, Formation.F4231, listOf(p("Yasuto Wakizaka", Position.MID, 74, 29, "JPN"), p("Marcinho", Position.FWD, 74, 29, "BRA"))),
        RealClubSeed("ura", "Urawa Red Diamonds", "URA", CountryCode.JPN, "jpn_t1", 1, "Saitama", "Saitama Stadium 2002", "#E30613", "#111111", defaultSpec(CrestShape.DIAMOND, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 22.0, 74, Formation.F4231, listOf(p("Shusaku Nishikawa", Position.GK, 73, 38, "JPN"), p("Marius Hoibraten", Position.DEF, 73, 29, "NOR"), p("Thiago Santana", Position.FWD, 74, 31, "BRA"))),
        // Additional J1 League clubs (8 -> 20)
        RealClubSeed("jpn1_tok", "FC Tokyo", "TOK", CountryCode.JPN, "jpn_t1", 1, "Tokyo", "Ajinomoto Stadium", "#1D4ED8", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.FLAME, "#FFFFFF"), 18.0, 73, Formation.F4231, listOf(p("Yuto Nagatomo", Position.DEF, 72, 38, "JPN"), p("Ryotaro Araki", Position.MID, 73, 22, "JPN"))),
        RealClubSeed("jpn1_ver", "Tokyo Verdy", "VER", CountryCode.JPN, "jpn_t1", 1, "Tokyo", "Ajinomoto Stadium", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FACC15"), 14.0, 72, Formation.F352, listOf(p("Matheus Vidotto", Position.GK, 72, 31, "BRA"), p("Itsuki Someno", Position.FWD, 72, 23, "JPN"))),
        RealClubSeed("jpn1_cer", "Cerezo Osaka", "CER", CountryCode.JPN, "jpn_t1", 1, "Osaka", "Yodoko Sakura", "#F472B6", "#1E3A8A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LAUREL, "#FFFFFF"), 17.0, 73, Formation.F4231, listOf(p("Kim Jin-hyeon", Position.GK, 72, 37, "KOR"), p("Leo Ceara", Position.FWD, 74, 29, "BRA"))),
        RealClubSeed("jpn1_nag", "Nagoya Grampus", "NAG", CountryCode.JPN, "jpn_t1", 1, "Nagoya", "Toyota Stadium", "#DC2626", "#F59E0B", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#F59E0B"), 18.0, 73, Formation.F352, listOf(p("Mitchell Langerak", Position.GK, 73, 36, "AUS"), p("Kasper Junker", Position.FWD, 73, 30, "DEN"))),
        RealClubSeed("jpn1_avi", "Avispa Fukuoka", "AVI", CountryCode.JPN, "jpn_t1", 1, "Fukuoka", "Best Denki Stadium", "#00205B", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 14.0, 72, Formation.F352, listOf(p("Masaaki Murakami", Position.GK, 71, 32, "JPN"), p("Shahab Zahedi", Position.FWD, 72, 29, "IRN"))),
        RealClubSeed("jpn1_alb", "Albirex Niigata", "ALB", CountryCode.JPN, "jpn_t1", 1, "Niigata", "Denka Big Swan", "#F97316", "#1D4ED8", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 13.0, 71, Formation.F4231, listOf(p("Ryosuke Kojima", Position.GK, 71, 27, "JPN"), p("Kaito Taniguchi", Position.FWD, 71, 29, "JPN"))),
        RealClubSeed("jpn1_kyo", "Kyoto Sanga", "KYO", CountryCode.JPN, "jpn_t1", 1, "Kyoto", "Sanga Stadium", "#6B21A8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"), 13.0, 71, Formation.F433, listOf(p("Gu Sung-yun", Position.GK, 71, 30, "KOR"), p("Rafael Elias", Position.FWD, 73, 25, "BRA"))),
        RealClubSeed("jpn1_sho", "Shonan Bellmare", "SHO", CountryCode.JPN, "jpn_t1", 1, "Hiratsuka", "Lemon Gas Stadium", "#16A34A", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 12.0, 71, Formation.F352, listOf(p("Naoto Kamifukumoto", Position.GK, 70, 35, "JPN"), p("Lukian", Position.FWD, 72, 33, "BRA"))),
        RealClubSeed("jpn1_ber", "Kashiwa Reysol", "REY", CountryCode.JPN, "jpn_t1", 1, "Kashiwa", "Hitachi Kashiwa", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.BOLT, "#111111"), 13.5, 71, Formation.F442, listOf(p("Kenta Matsumoto", Position.GK, 70, 27, "JPN"), p("Mao Hosoya", Position.FWD, 73, 23, "JPN"))),
        RealClubSeed("jpn1_yfc", "Yokohama FC", "YFC", CountryCode.JPN, "jpn_t1", 1, "Yokohama", "Mitsuzawa Stadium", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 11.5, 70, Formation.F352, listOf(p("Akinori Ichikawa", Position.GK, 70, 26, "JPN"), p("Towa Yamane", Position.MID, 70, 25, "JPN"))),
        RealClubSeed("jpn1_shi", "Shimizu S-Pulse", "SSP", CountryCode.JPN, "jpn_t1", 1, "Shizuoka", "IAI Stadium", "#F97316", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 13.0, 71, Formation.F4231, listOf(p("Shuichi Gonda", Position.GK, 72, 35, "JPN"), p("Takashi Inui", Position.MID, 72, 36, "JPN"))),
        RealClubSeed("jpn1_oka", "Fagiano Okayama", "OKA", CountryCode.JPN, "jpn_t1", 1, "Okayama", "City Light Stadium", "#7F1D1D", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 11.0, 70, Formation.F352, listOf(p("Svend Brodersen", Position.GK, 70, 27, "GER"), p("Lucao", Position.FWD, 70, 29, "BRA"))),
    )

    private fun createKoreaSeeds(): List<RealClubSeed> = listOf(
        RealClubSeed("uls", "Ulsan HD", "ULS", CountryCode.KOR, "kor_t1", 1, "Ulsan", "Munsu Stadium", "#1D4ED8", "#FACC15", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FACC15"), 22.0, 75, Formation.F4231, listOf(p("Jo Hyeon-woo", Position.GK, 76, 33, "KOR"), p("Kim Young-gwon", Position.DEF, 74, 34, "KOR"), p("Joo Min-kyu", Position.FWD, 74, 34, "KOR"))),
        RealClubSeed("gan", "Gangwon FC", "GAN", CountryCode.KOR, "kor_t1", 1, "Chuncheon", "Chuncheon Songam", "#F97316", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 12.0, 72, Formation.F442, listOf(p("Yang Min-hyeok", Position.FWD, 73, 18, "KOR"))),
        RealClubSeed("fcs", "FC Seoul", "SEO", CountryCode.KOR, "kor_t1", 1, "Seoul", "Seoul World Cup", "#D71920", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#D4AF37"), 18.0, 73, Formation.F4231, listOf(p("Yazan Al-Arab", Position.DEF, 72, 28, "JOR"), p("Ki Sung-yueng", Position.MID, 73, 35, "KOR"), p("Jesse Lingard", Position.MID, 75, 31, "ENG"))),
        RealClubSeed("poh", "Pohang Steelers", "POH", CountryCode.KOR, "kor_t1", 1, "Pohang", "Steel Yard", "#D71920", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.STAR, "#FFFFFF"), 15.0, 73, Formation.F442, listOf(p("Wanderson", Position.DEF, 72, 35, "BRA"), p("Jorge Luiz", Position.FWD, 72, 25, "BRA"))),
        RealClubSeed("jeo", "Jeonbuk Hyundai", "JEO", CountryCode.KOR, "kor_t1", 1, "Jeonju", "Jeonju World Cup", "#00853E", "#1E3A8A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"), 20.0, 74, Formation.F4231, listOf(p("Song Bum-keun", Position.GK, 72, 27, "KOR"), p("Park Jin-seob", Position.DEF, 72, 29, "KOR"), p("Lee Seung-woo", Position.FWD, 73, 26, "KOR"))),
        RealClubSeed("gim_kor", "Gimcheon Sangmu", "GIM", CountryCode.KOR, "kor_t1", 1, "Gimcheon", "Gimcheon Stadium", "#D71920", "#00205B", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FACC15"), 14.0, 72, Formation.F433, listOf(p("Lee Dong-gyeong", Position.MID, 73, 27, "KOR"))),
        RealClubSeed("gwa", "Gwangju FC", "GWA", CountryCode.KOR, "kor_t1", 1, "Gwangju", "Gwangju World Cup", "#FACC15", "#D71920", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.FLAME, "#D71920"), 11.0, 72, Formation.F442, listOf(p("Jasir Asani", Position.FWD, 73, 29, "ALB"))),
        RealClubSeed("suw", "Suwon FC", "SUW", CountryCode.KOR, "kor_t1", 1, "Suwon", "Suwon Sports Complex", "#D71920", "#0038A8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.TOWER, "#FFFFFF"), 10.0, 70, Formation.F433, listOf(p("Anderson Oliveira", Position.FWD, 72, 26, "BRA"))),
        // Additional K League 1 clubs (8 -> 12)
        RealClubSeed("kor1_dae", "Daejeon Hana Citizen", "DHN", CountryCode.KOR, "kor_t1", 1, "Daejeon", "Daejeon World Cup", "#7F1D1D", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 13.0, 71, Formation.F4231, listOf(p("Lee Chang-geun", Position.GK, 71, 31, "KOR"), p("Masatoshi Ishida", Position.MID, 71, 29, "JPN"))),
        RealClubSeed("kor1_dgu", "Daegu FC", "DGU", CountryCode.KOR, "kor_t1", 1, "Daegu", "DGB Daegu Bank Park", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"), 11.0, 71, Formation.F352, listOf(p("Oh Seung-hoon", Position.GK, 71, 36, "KOR"), p("Cesinha", Position.FWD, 74, 35, "BRA"))),
        RealClubSeed("kor1_jej", "Jeju SK", "JEJ", CountryCode.KOR, "kor_t1", 1, "Seogwipo", "Jeju World Cup", "#F97316", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.BOLT, "#FFFFFF"), 11.5, 71, Formation.F4231, listOf(p("Kim Dong-jun", Position.GK, 71, 29, "KOR"), p("Yuri Jonathan", Position.FWD, 71, 26, "BRA"))),
        RealClubSeed("kor1_any", "FC Anyang", "ANY", CountryCode.KOR, "kor_t1", 1, "Anyang", "Anyang Stadium", "#6B21A8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 10.0, 70, Formation.F4231, listOf(p("Kim Da-sol", Position.GK, 70, 35, "KOR"), p("Matheus Oliveira", Position.MID, 71, 27, "BRA"))),
    )

    private fun createAfcOtherSeeds(): List<RealClubSeed> = listOf(
        // ACL Elite West Pool (Iran & Uzbekistan)
        RealClubSeed("per", "Persepolis", "PER", CountryCode.AFC_OTHER, "afc_west_pool", 1, "Tehran", "Azadi Stadium", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 18.0, 74, Formation.F442, listOf(p("Alexis Guendouz", Position.GK, 73, 28, "ALG"), p("Giorgi Gvelesiani", Position.DEF, 72, 33, "GEO"), p("Oston Urunov", Position.MID, 74, 23, "UZB"))),
        RealClubSeed("est_irn", "Esteghlal", "EST", CountryCode.AFC_OTHER, "afc_west_pool", 1, "Tehran", "Azadi Stadium", "#0047AB", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 16.0, 73, Formation.F352, listOf(p("Hossein Hosseini", Position.GK, 72, 32, "IRN"), p("Didier Ndong", Position.MID, 72, 30, "GAB"))),
        RealClubSeed("sep", "Sepahan", "SEP", CountryCode.AFC_OTHER, "afc_west_pool", 1, "Isfahan", "Naghsh-e Jahan", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#111111"), 15.0, 73, Formation.F433, listOf(p("Payam Niazmand", Position.GK, 72, 29, "IRN"), p("Steven Nzonzi", Position.MID, 74, 35, "FRA"))),
        RealClubSeed("pak", "Pakhtakor", "PAK", CountryCode.AFC_OTHER, "afc_west_pool", 1, "Tashkent", "Pakhtakor Stadium", "#FACC15", "#0038A8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 12.0, 71, Formation.F4231, listOf(p("Dragan Ceran", Position.FWD, 72, 37, "SRB"))),
        // ACL Elite East Pool (China, Australia, Thailand, Malaysia)
        RealClubSeed("shp", "Shanghai Port", "SHP", CountryCode.AFC_OTHER, "afc_east_pool", 1, "Shanghai", "Pudong Football Stadium", "#D71920", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FACC15"), 26.0, 74, Formation.F4231, listOf(p("Yan Junling", Position.GK, 72, 33, "CHN"), p("Wu Lei", Position.FWD, 75, 33, "CHN"), p("Matias Vargas", Position.MID, 75, 27, "ARG"))),
        RealClubSeed("shs", "Shanghai Shenhua", "SHS", CountryCode.AFC_OTHER, "afc_east_pool", 1, "Shanghai", "Shanghai Stadium", "#0038A8", "#D71920", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 22.0, 73, Formation.F442, listOf(p("Joao Carlos Teixeira", Position.MID, 73, 31, "POR"), p("Andre Luis", Position.FWD, 73, 30, "BRA"))),
        RealClubSeed("sdt", "Shandong Taishan", "SDT", CountryCode.AFC_OTHER, "afc_east_pool", 1, "Jinan", "Jinan Olympic Sports", "#F97316", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 20.0, 73, Formation.F4231, listOf(p("Wang Dalei", Position.GK, 72, 35, "CHN"), p("Cryzan", Position.FWD, 74, 28, "BRA"))),
        RealClubSeed("bur_tha", "Buriram United", "BUR", CountryCode.AFC_OTHER, "afc_east_pool", 1, "Buriram", "Chang Arena", "#00205B", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.BOLT, "#FACC15"), 12.0, 71, Formation.F352, listOf(p("Theerathon Bunmathan", Position.DEF, 71, 34, "THA"), p("Supachai Chaided", Position.FWD, 71, 26, "THA"))),
        RealClubSeed("jdt", "Johor Darul Ta'zim", "JDT", CountryCode.AFC_OTHER, "afc_east_pool", 1, "Johor Bahru", "Sultan Ibrahim Stadium", "#0038A8", "#D71920", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.LION, "#FACC15"), 16.0, 72, Formation.F433, listOf(p("Arif Aiman", Position.FWD, 72, 22, "MAS"), p("Bergson", Position.FWD, 74, 33, "BRA"))),
        RealClubSeed("ccm", "Central Coast Mariners", "CCM", CountryCode.AFC_OTHER, "afc_east_pool", 1, "Gosford", "Industree Group Stadium", "#FACC15", "#0038A8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.ANCHOR, "#0038A8"), 8.5, 69, Formation.F442, listOf(p("Brian Kaltak", Position.DEF, 69, 31, "VAN"), p("Alou Kuol", Position.FWD, 69, 23, "AUS"))),
    )
}
