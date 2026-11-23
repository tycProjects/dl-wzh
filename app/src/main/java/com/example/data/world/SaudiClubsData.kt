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
 * Modular 3-Tier Saudi Arabian Football Pyramid:
 * - Tier 1: Roshn Saudi League
 * - Tier 2: Yelo League (First Division)
 * - Tier 3: Saudi Second Division
 * Plus real Asian/Gulf guest clubs for the AFC Champions League Elite bracket.
 */
object SaudiClubsData {

    val divisions: List<DivisionInfo> = listOf(
        DivisionInfo("ksa_t1", "Roshn Saudi League", CountryCode.KSA, 1, 0, 0, 2, aclSpots = 3),
        DivisionInfo("ksa_t2", "Yelo League", CountryCode.KSA, 2, 2, 2, 2),
        DivisionInfo("ksa_t3", "Saudi Second Division", CountryCode.KSA, 3, 2, 2, 0),
    )

    private fun p(name: String, pos: Position, ovr: Int, age: Int, nat: String = "KSA") =
        RealPlayerSeed(name, pos, ovr, age, nat)

    private val saudiPool: List<RealPlayerSeed> = listOf(
        p("Nawaf Al-Aqidi", Position.GK, 68, 24), p("Mohammed Al-Owais", Position.GK, 73, 33),
        p("Fawaz Al-Qarni", Position.GK, 67, 32), p("Abdullah Al-Mayouf", Position.GK, 70, 37),
        p("Mohammed Al-Yami", Position.GK, 67, 27), p("Mazen Al-Qahtani", Position.GK, 60, 26),
        p("Rakan Al-Najjar", Position.GK, 61, 27), p("Waleed Abdullah", Position.GK, 65, 38),
        p("Sultan Al-Ghannam", Position.DEF, 75, 30), p("Yasser Al-Shahrani", Position.DEF, 74, 32),
        p("Ali Al-Bulaihi", Position.DEF, 75, 34), p("Hassan Al-Tambakti", Position.DEF, 75, 25),
        p("Abdulelah Al-Amri", Position.DEF, 72, 27), p("Ahmed Sharahili", Position.DEF, 71, 30),
        p("Muteb Al-Harbi", Position.DEF, 72, 24), p("Ziyad Al-Sahafi", Position.DEF, 66, 30),
        p("Mohammed Jahfali", Position.DEF, 67, 34), p("Khalifah Al-Dawsari", Position.DEF, 67, 25),
        p("Hussain Al-Shuwaish", Position.DEF, 62, 35), p("Fahad Al-Harbi", Position.DEF, 61, 27),
        p("Motaz Hawsawi", Position.DEF, 65, 32), p("Omar Hawsawi", Position.DEF, 64, 39),
        p("Abdullah Al-Zori", Position.DEF, 62, 37), p("Saad Al-Mousa", Position.DEF, 70, 22),
        p("Salman Al-Faraj", Position.MID, 73, 35), p("Nasser Al-Dawsari", Position.MID, 74, 25),
        p("Mohamed Kanno", Position.MID, 76, 30), p("Abdullah Al-Khaibari", Position.MID, 73, 28),
        p("Abdulelah Al-Malki", Position.MID, 72, 30), p("Sami Al-Najei", Position.MID, 72, 27),
        p("Faisal Al-Ghamdi", Position.MID, 71, 23), p("Musab Al-Juwayr", Position.MID, 72, 21),
        p("Hattan Bahebri", Position.MID, 68, 32), p("Abdulrahman Ghareeb", Position.MID, 73, 27),
        p("Mohammed Al-Kuwaykibi", Position.MID, 69, 30), p("Riyadh Sharahili", Position.MID, 66, 31),
        p("Ali Al-Hassan", Position.MID, 71, 27), p("Awad Khamis", Position.MID, 63, 36),
        p("Saleh Al-Shehri", Position.FWD, 72, 31), p("Firas Al-Buraikan", Position.FWD, 75, 24),
        p("Abdullah Al-Hamdan", Position.FWD, 70, 25), p("Abdullah Radif", Position.FWD, 69, 21),
        p("Haroune Camara", Position.FWD, 68, 26), p("Haitham Asiri", Position.FWD, 68, 23),
        p("Ayman Yahya", Position.FWD, 71, 23), p("Khalid Al-Ghannam", Position.FWD, 70, 24),
        p("Rakan Al-Shamlan", Position.FWD, 62, 26), p("Mansour Al-Muwallad", Position.FWD, 61, 27),
        p("Turki Al-Ammar", Position.FWD, 67, 25), p("Mohammed Maran", Position.FWD, 67, 23),
    )

    fun createClubs(): List<Club> {
        val seeds = listOf(
            // ==================== TIER 1: ROSHN SAUDI LEAGUE (18 CLUBS) ====================
            RealClubSeed(
                id = "al-hilal", name = "Al-Hilal", nameAr = "الهلال", shortName = "HIL",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Riyadh", stadium = "Kingdom Arena",
                primaryHex = "#0033FF", secondaryHex = "#FFFFFF",
                crestSpec = defaultSpec(
                    shape = CrestShape.MODERN_CREST,
                    pattern = CrestPattern.SOLID,
                    symbol = CrestSymbol.CRESCENT,
                    accentHex = "#FFFFFF",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' rx='18' fill='#0a0f1c'/><path d='M22 18 H36 V56 C27 56 22 61 22 70 Z M44 14 H56 V44 C47 44 44 49 44 56 Z M64 18 H78 V68 C69 68 64 73 64 82 Z' fill='#0033FF'/><circle cx='72' cy='24' r='6' fill='#FFFFFF'/></svg>",
                ),
                budgetM = 120.0, clubOvr = 83, formation = Formation.F4231,
                players = listOf(
                    p("Yassine Bounou", Position.GK, 84, 33, "MAR"), p("Mohammed Al-Owais", Position.GK, 74, 33),
                    p("Renan Lodi", Position.DEF, 80, 26, "BRA"), p("Kalidou Koulibaly", Position.DEF, 84, 33, "SEN"),
                    p("Ali Al-Bulaihi", Position.DEF, 76, 34), p("Joao Cancelo", Position.DEF, 85, 30, "POR"),
                    p("Hassan Al-Tambakti", Position.DEF, 76, 25),
                    p("Ruben Neves", Position.MID, 84, 27, "POR"), p("Sergej Milinkovic-Savic", Position.MID, 85, 29, "SRB"),
                    p("Malcom", Position.MID, 82, 27, "BRA"), p("Mohamed Kanno", Position.MID, 76, 30),
                    p("Nasser Al-Dawsari", Position.MID, 75, 25),
                    p("Salem Al-Dawsari", Position.FWD, 81, 33), p("Aleksandar Mitrovic", Position.FWD, 84, 30, "SRB"),
                    p("Marcos Leonardo", Position.FWD, 79, 21, "BRA"), p("Abdullah Al-Hamdan", Position.FWD, 71, 25),
                ),
            ),
            RealClubSeed(
                id = "al-nassr", name = "Al-Nassr", nameAr = "النصر", shortName = "NAS",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Riyadh", stadium = "Al-Awwal Park",
                primaryHex = "#FFD700", secondaryHex = "#0B1E5B",
                crestSpec = defaultSpec(
                    shape = CrestShape.ROUNDEL,
                    pattern = CrestPattern.HALF_AND_HALF,
                    symbol = CrestSymbol.CROWN,
                    accentHex = "#0B1E5B",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><circle cx='50' cy='50' r='44' fill='#0B1E5B' stroke='#FFD700' stroke-width='5'/><circle cx='50' cy='50' r='27' fill='#FFD700'/><polygon points='50,28 57,42 72,42 60,51 65,66 50,57 35,66 40,51 28,42 43,42' fill='#0B1E5B'/></svg>",
                ),
                budgetM = 110.0, clubOvr = 81, formation = Formation.F4231,
                players = listOf(
                    p("Bento", Position.GK, 80, 25, "BRA"), p("Nawaf Al-Aqidi", Position.GK, 70, 24),
                    p("Mohamed Simakan", Position.DEF, 81, 24, "FRA"), p("Aymeric Laporte", Position.DEF, 84, 30, "ESP"),
                    p("Sultan Al-Ghannam", Position.DEF, 76, 30), p("Salem Al-Najdi", Position.DEF, 70, 21),
                    p("Marcelo Brozovic", Position.MID, 82, 32, "CRO"), p("Otavio", Position.MID, 81, 29, "POR"),
                    p("Abdullah Al-Khaibari", Position.MID, 73, 28), p("Angelo Gabriel", Position.FWD, 75, 19, "BRA"),
                    p("Sadio Mane", Position.FWD, 83, 32, "SEN"), p("Cristiano Ronaldo", Position.FWD, 86, 39, "POR"),
                    p("Jhon Duran", Position.FWD, 80, 20, "COL"), p("Abdulrahman Ghareeb", Position.FWD, 74, 27),
                ),
            ),
            RealClubSeed(
                id = "al-ittihad", name = "Al-Ittihad", nameAr = "الاتحاد", shortName = "ITT",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Jeddah", stadium = "King Abdullah Sports City",
                primaryHex = "#FFE600", secondaryHex = "#111111",
                crestSpec = defaultSpec(
                    shape = CrestShape.CLASSIC_SHIELD,
                    pattern = CrestPattern.VERTICAL_STRIPES,
                    symbol = CrestSymbol.LION,
                    accentHex = "#111111",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect x='14' y='12' width='72' height='76' rx='12' fill='#FFE600' stroke='#111111' stroke-width='4'/><path d='M30 24 H70 V52 C70 66 50 74 50 74 C50 74 30 66 30 52 Z' fill='#111111'/><rect x='43' y='28' width='14' height='36' fill='#FFE600'/></svg>",
                ),
                budgetM = 95.0, clubOvr = 80, formation = Formation.F4231,
                players = listOf(
                    p("Predrag Rajkovic", Position.GK, 81, 29, "SRB"), p("Hamed Al-Shanqiti", Position.GK, 68, 19),
                    p("Mario Mitaj", Position.DEF, 75, 21, "ALB"), p("Danilo Pereira", Position.DEF, 81, 33, "POR"),
                    p("Abdulelah Al-Amri", Position.DEF, 73, 27), p("Muhannad Al-Shanqeeti", Position.DEF, 72, 25),
                    p("Fabinho", Position.MID, 82, 31, "BRA"), p("N'Golo Kante", Position.MID, 84, 33, "FRA"),
                    p("Houssem Aouar", Position.MID, 80, 26, "ALG"), p("Moussa Diaby", Position.FWD, 83, 25, "FRA"),
                    p("Steven Bergwijn", Position.FWD, 80, 27, "NED"), p("Karim Benzema", Position.FWD, 86, 36, "FRA"),
                    p("Saleh Al-Shehri", Position.FWD, 73, 31),
                ),
            ),
            RealClubSeed(
                id = "al-ahli", name = "Al-Ahli", nameAr = "الأهلي", shortName = "AHL",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Jeddah", stadium = "King Abdullah Sports City",
                primaryHex = "#007A3D", secondaryHex = "#FFFFFF",
                crestSpec = defaultSpec(
                    shape = CrestShape.CLASSIC_SHIELD,
                    pattern = CrestPattern.SOLID,
                    symbol = CrestSymbol.CRESCENT,
                    accentHex = "#FFFFFF",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><path d='M20 16 L50 8 L80 16 V52 C80 72 50 88 50 88 C50 88 20 72 20 52 Z' fill='#007A3D' stroke='#FFFFFF' stroke-width='4'/><rect x='20' y='44' width='60' height='12' fill='#FFFFFF'/><circle cx='50' cy='28' r='9' fill='#FFFFFF'/></svg>",
                ),
                budgetM = 85.0, clubOvr = 79, formation = Formation.F4231,
                players = listOf(
                    p("Edouard Mendy", Position.GK, 81, 32, "SEN"), p("Roger Ibanez", Position.DEF, 81, 26, "BRA"),
                    p("Merih Demiral", Position.DEF, 80, 26, "TUR"), p("Ali Majrashi", Position.DEF, 72, 25),
                    p("Franck Kessie", Position.MID, 82, 27, "CIV"), p("Gabri Veiga", Position.MID, 79, 22, "ESP"),
                    p("Ziyad Al-Johani", Position.MID, 71, 23), p("Riyad Mahrez", Position.FWD, 83, 33, "ALG"),
                    p("Galeno", Position.FWD, 81, 27, "BRA"), p("Ivan Toney", Position.FWD, 81, 28, "ENG"),
                    p("Roberto Firmino", Position.FWD, 79, 33, "BRA"), p("Firas Al-Buraikan", Position.FWD, 75, 24),
                ),
            ),
            RealClubSeed(
                id = "al-qadsiah", name = "Al Qadsiah", nameAr = "القادسية", shortName = "QAD",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Khobar", stadium = "Prince Saud bin Jalawi",
                primaryHex = "#DC2626", secondaryHex = "#FACC15",
                crestSpec = defaultSpec(CrestShape.MODERN_CREST, CrestPattern.DIAGONAL_SASH, CrestSymbol.STAR, "#FACC15"),
                budgetM = 50.0, clubOvr = 77, formation = Formation.F352,
                players = listOf(
                    p("Koen Casteels", Position.GK, 81, 32, "BEL"), p("Nacho Fernandez", Position.DEF, 81, 34, "ESP"),
                    p("Gaston Alvarez", Position.DEF, 76, 24, "URU"), p("Mohammed Abu Al-Shamat", Position.DEF, 71, 22),
                    p("Equi Fernandez", Position.MID, 78, 22, "ARG"), p("Nahitan Nandez", Position.MID, 78, 28, "URU"),
                    p("Cameron Puertas", Position.MID, 77, 26, "ESP"), p("Julian Quinones", Position.FWD, 79, 27, "MEX"),
                    p("Pierre-Emerick Aubameyang", Position.FWD, 80, 35, "GAB"),
                ),
            ),
            RealClubSeed(
                id = "al-shabab", name = "Al-Shabab", nameAr = "الشباب", shortName = "SHA",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Riyadh", stadium = "Al-Shabab Stadium",
                primaryHex = "#FFFFFF", secondaryHex = "#8B3A2B",
                crestSpec = defaultSpec(
                    shape = CrestShape.ROUNDEL,
                    pattern = CrestPattern.VERTICAL_STRIPES,
                    symbol = CrestSymbol.LION,
                    accentHex = "#8B3A2B",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><circle cx='50' cy='50' r='44' fill='#FFFFFF' stroke='#8B3A2B' stroke-width='5'/><circle cx='50' cy='50' r='28' fill='none' stroke='#8B3A2B' stroke-width='3'/><polygon points='50,28 66,50 50,72 34,50' fill='#111111'/></svg>",
                ),
                budgetM = 34.0, clubOvr = 75, formation = Formation.F352,
                players = listOf(
                    p("Georgiy Bushchan", Position.GK, 76, 30, "UKR"), p("Wesley Hoedt", Position.DEF, 74, 30, "NED"),
                    p("Robert Renan", Position.DEF, 75, 21, "BRA"), p("Giacomo Bonaventura", Position.MID, 77, 35, "ITA"),
                    p("Cristian Guanca", Position.MID, 75, 31, "ARG"), p("Yannick Carrasco", Position.FWD, 81, 31, "BEL"),
                    p("Daniel Podence", Position.FWD, 77, 29, "POR"), p("Abderrazak Hamdallah", Position.FWD, 77, 33, "MAR"),
                ),
            ),
            RealClubSeed(
                id = "al-ettifaq", name = "Al-Ettifaq", nameAr = "الاتفاق", shortName = "ETT",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Dammam", stadium = "EGO Stadium",
                primaryHex = "#1B4D3E", secondaryHex = "#8C2318",
                crestSpec = defaultSpec(
                    shape = CrestShape.ROUNDEL,
                    pattern = CrestPattern.HALF_AND_HALF,
                    symbol = CrestSymbol.EAGLE,
                    accentHex = "#FFFFFF",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><circle cx='50' cy='50' r='44' fill='#FFFFFF' stroke='#1B4D3E' stroke-width='5'/><circle cx='50' cy='50' r='31' fill='none' stroke='#8C2318' stroke-width='3'/><polygon points='50,24 68,54 50,48 32,54' fill='#1B4D3E'/></svg>",
                ),
                budgetM = 28.0, clubOvr = 74, formation = Formation.F433,
                players = listOf(
                    p("Marek Rodak", Position.GK, 74, 27, "SVK"), p("Jack Hendry", Position.DEF, 73, 29, "SCO"),
                    p("Abdullah Madu", Position.DEF, 71, 31), p("Georginio Wijnaldum", Position.MID, 78, 34, "NED"),
                    p("Seko Fofana", Position.MID, 78, 29, "CIV"), p("Alvaro Medran", Position.MID, 73, 30, "ESP"),
                    p("Moussa Dembele", Position.FWD, 77, 28, "FRA"), p("Karl Toko Ekambi", Position.FWD, 75, 32, "CMR"),
                ),
            ),
            RealClubSeed(
                id = "al-taawoun", name = "Al-Taawoun", nameAr = "التعاون", shortName = "TAA",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Buraidah", stadium = "Al Taawoun Stadium",
                primaryHex = "#0A3871", secondaryHex = "#FFE600",
                crestSpec = defaultSpec(
                    shape = CrestShape.CLASSIC_SHIELD,
                    pattern = CrestPattern.SOLID,
                    symbol = CrestSymbol.STAR,
                    accentHex = "#FFE600",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><path d='M18 14 H82 V54 C82 74 50 88 50 88 C50 88 18 74 18 54 Z' fill='#0A3871' stroke='#FFE600' stroke-width='5'/><polygon points='50,26 68,50 50,74 32,50' fill='#FFE600'/></svg>",
                ),
                budgetM = 18.0, clubOvr = 72, formation = Formation.F4231,
                players = listOf(
                    p("Mailson", Position.GK, 73, 28, "BRA"), p("Andrei Girotto", Position.DEF, 74, 32, "BRA"),
                    p("Aschraf El Mahdioui", Position.MID, 73, 28, "NED"), p("Faycal Fajr", Position.MID, 73, 36, "MAR"),
                    p("Flavio", Position.MID, 72, 28, "BRA"), p("Musa Barrow", Position.FWD, 75, 26, "GAM"),
                    p("Roger Martinez", Position.FWD, 74, 30, "COL"),
                ),
            ),
            RealClubSeed(
                id = "neom", name = "NEOM", nameAr = "نيوم", shortName = "NEO",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Tabuk", stadium = "King Khalid Sport City",
                primaryHex = "#0085CA", secondaryHex = "#00843D",
                crestSpec = defaultSpec(
                    shape = CrestShape.MODERN_CREST,
                    pattern = CrestPattern.SOLID,
                    symbol = CrestSymbol.BOLT,
                    accentHex = "#FFFFFF",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' rx='18' fill='#111111'/><polygon points='50,16 82,40 70,78 30,78 18,40' fill='none' stroke='#0085CA' stroke-width='5'/><circle cx='50' cy='48' r='13' fill='#00843D'/></svg>",
                ),
                budgetM = 45.0, clubOvr = 74, formation = Formation.F4231,
                players = listOf(
                    p("Mustafa Malayekah", Position.GK, 70, 38), p("Ahmed Hegazi", Position.DEF, 76, 33, "EGY"),
                    p("Salman Al-Faraj", Position.MID, 74, 35), p("Said Benrahma", Position.FWD, 78, 29, "ALG"),
                    p("Romarinho", Position.FWD, 76, 33, "BRA"), p("Abdullah Doucoure", Position.MID, 76, 31, "MLI"),
                ),
            ),
            RealClubSeed(
                id = "al-fateh", name = "Al-Fateh", nameAr = "الفتح", shortName = "FAT",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Al-Hasa", stadium = "Al-Fateh Club Stadium",
                primaryHex = "#E60000", secondaryHex = "#111111",
                crestSpec = defaultSpec(
                    shape = CrestShape.MODERN_CREST,
                    pattern = CrestPattern.SOLID,
                    symbol = CrestSymbol.STAR,
                    accentHex = "#FFFFFF",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' rx='18' fill='#111111'/><polygon points='28,24 72,12 72,62 28,84' fill='none' stroke='#E60000' stroke-width='6'/><circle cx='50' cy='48' r='12' fill='#FFFFFF'/></svg>",
                ),
                budgetM = 14.0, clubOvr = 71, formation = Formation.F4231,
                players = listOf(
                    p("Peter Szappanos", Position.GK, 71, 33, "HUN"), p("Marwane Saadane", Position.DEF, 71, 32, "MAR"),
                    p("Jason Denayer", Position.DEF, 73, 29, "BEL"), p("Sofiane Bendebka", Position.MID, 72, 32, "ALG"),
                    p("Matias Vargas", Position.MID, 74, 27, "ARG"), p("Mourad Batna", Position.FWD, 75, 34, "MAR"),
                    p("Djaniny", Position.FWD, 73, 33, "CPV"),
                ),
            ),
            RealClubSeed(
                id = "al-fayha", name = "Al-Fayha", nameAr = "الفيحاء", shortName = "FAY",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Al-Majma'ah", stadium = "Al Majma'ah Sports City",
                primaryHex = "#F27D00", secondaryHex = "#1E3A8A",
                crestSpec = defaultSpec(
                    shape = CrestShape.CLASSIC_SHIELD,
                    pattern = CrestPattern.HOOPS,
                    symbol = CrestSymbol.STAR,
                    accentHex = "#F27D00",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><path d='M22 14 H78 V54 C78 74 50 88 50 88 C50 88 22 74 22 54 Z' fill='#FFFFFF' stroke='#1E3A8A' stroke-width='5'/><rect x='22' y='26' width='56' height='14' fill='#1E3A8A'/><circle cx='50' cy='58' r='14' fill='#F27D00'/></svg>",
                ),
                budgetM = 12.0, clubOvr = 70, formation = Formation.F4231,
                players = listOf(
                    p("Orlando Mosquera", Position.GK, 72, 29, "PAN"), p("Chris Smalling", Position.DEF, 76, 35, "ENG"),
                    p("Gojko Cimirot", Position.MID, 72, 31, "BIH"), p("Alejandro Pozuelo", Position.MID, 74, 33, "ESP"),
                    p("Fashion Sakala", Position.FWD, 74, 27, "ZAM"), p("Renzo Lopez", Position.FWD, 71, 30, "URU"),
                ),
            ),
            RealClubSeed(
                id = "al-khaleej", name = "Al-Khaleej", nameAr = "الخليج", shortName = "KHA",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Saihat", stadium = "Prince Mohamed bin Fahd",
                primaryHex = "#007A3D", secondaryHex = "#FFD100",
                crestSpec = defaultSpec(
                    shape = CrestShape.ITALIAN_OVAL,
                    pattern = CrestPattern.SOLID,
                    symbol = CrestSymbol.EAGLE,
                    accentHex = "#FFD100",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><path d='M50 8 C74 8 88 28 88 52 C88 74 62 92 50 92 C38 92 12 74 12 52 C12 28 26 8 50 8 Z' fill='#007A3D' stroke='#FFD100' stroke-width='4'/><polygon points='50,20 66,56 34,56' fill='#FFD100'/></svg>",
                ),
                budgetM = 12.0, clubOvr = 70, formation = Formation.F433,
                players = listOf(
                    p("Ibrahim Sehic", Position.GK, 72, 36, "BIH"), p("Marcel Tisserand", Position.DEF, 72, 31, "COD"),
                    p("Pedro Rebocho", Position.DEF, 71, 29, "POR"), p("Kostas Fortounis", Position.MID, 75, 32, "GRE"),
                    p("Fabio Martins", Position.FWD, 73, 31, "POR"), p("Abdullah Al-Salem", Position.FWD, 71, 31),
                ),
            ),
            RealClubSeed(
                id = "al-kholood", name = "Al-Kholood", nameAr = "الخلود", shortName = "KHO",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Ar Rass", stadium = "Al-Hazem Club Stadium",
                primaryHex = "#8B1538", secondaryHex = "#F3D9B1",
                crestSpec = defaultSpec(
                    shape = CrestShape.ROUNDEL,
                    pattern = CrestPattern.SOLID,
                    symbol = CrestSymbol.LAUREL,
                    accentHex = "#F3D9B1",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><circle cx='50' cy='50' r='44' fill='#102619' stroke='#F3D9B1' stroke-width='4'/><circle cx='50' cy='50' r='33' fill='none' stroke='#8B1538' stroke-width='4'/><circle cx='50' cy='50' r='14' fill='#F3D9B1'/></svg>",
                ),
                budgetM = 10.5, clubOvr = 69, formation = Formation.F4231,
                players = listOf(
                    p("Marcelo Grohe", Position.GK, 73, 37, "BRA"), p("Norbert Gyomber", Position.DEF, 71, 32, "SVK"),
                    p("William Troost-Ekong", Position.DEF, 72, 31, "NGA"), p("Aliou Dieng", Position.MID, 74, 27, "MLI"),
                    p("Alex Collado", Position.MID, 72, 25, "ESP"), p("Jackson Muleka", Position.FWD, 72, 25, "COD"),
                    p("Myziane Maolida", Position.FWD, 72, 25, "COM"),
                ),
            ),
            RealClubSeed(
                id = "al-riyadh", name = "Al-Riyadh", nameAr = "الرياض", shortName = "RIY",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Riyadh", stadium = "Prince Faisal bin Fahd",
                primaryHex = "#E61C1C", secondaryHex = "#22252A",
                crestSpec = defaultSpec(
                    shape = CrestShape.CLASSIC_SHIELD,
                    pattern = CrestPattern.VERTICAL_STRIPES,
                    symbol = CrestSymbol.NONE,
                    accentHex = "#FFFFFF",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><path d='M20 18 H80 V54 C80 74 50 88 50 88 C50 88 20 74 20 54 Z' fill='#22252A' stroke='#FFFFFF' stroke-width='3'/><rect x='33' y='30' width='9' height='42' fill='#E61C1C'/><rect x='46' y='30' width='9' height='42' fill='#E61C1C'/><rect x='59' y='30' width='9' height='42' fill='#E61C1C'/></svg>",
                ),
                budgetM = 11.0, clubOvr = 69, formation = Formation.F4231,
                players = listOf(
                    p("Milan Borjan", Position.GK, 73, 37, "CAN"), p("Lucas Kal", Position.DEF, 71, 28, "BRA"),
                    p("Yoann Barbet", Position.DEF, 72, 31, "FRA"), p("Bernard Mensah", Position.MID, 72, 30, "GHA"),
                    p("Ibrahim Bayesh", Position.MID, 71, 24, "IRQ"), p("Mohamed Konate", Position.FWD, 72, 26, "BFA"),
                ),
            ),
            RealClubSeed(
                id = "al-hazem", name = "Al-Hazem", nameAr = "الحزم", shortName = "HAZ",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Ar Rass", stadium = "Al-Hazem Club Stadium",
                primaryHex = "#F5B800", secondaryHex = "#D4A373",
                crestSpec = defaultSpec(
                    shape = CrestShape.CLASSIC_SHIELD,
                    pattern = CrestPattern.HALF_AND_HALF,
                    symbol = CrestSymbol.STAR,
                    accentHex = "#F5B800",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' rx='18' fill='#111111'/><path d='M24 20 H76 V52 C76 72 50 84 50 84 C50 84 24 72 24 52 Z' fill='#1E293B' stroke='#F5B800' stroke-width='5'/><polygon points='50,32 62,48 50,64 38,48' fill='#F5B800'/></svg>",
                ),
                budgetM = 9.5, clubOvr = 68, formation = Formation.F433,
                players = listOf(
                    p("Aymen Dahmen", Position.GK, 70, 27, "TUN"), p("Paulo Ricardo", Position.DEF, 69, 30, "BRA"),
                    p("Toze", Position.MID, 70, 31, "POR"), p("Yousef Al-Shammari", Position.FWD, 68, 26),
                ),
            ),
            RealClubSeed(
                id = "al-akhdood", name = "Al-Akhdood", nameAr = "الأخدود", shortName = "AKH",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Najran", stadium = "Prince Hathloul bin Abdul Aziz",
                primaryHex = "#0EA5E9", secondaryHex = "#FFFFFF",
                crestSpec = defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"),
                budgetM = 9.0, clubOvr = 68, formation = Formation.F433,
                players = listOf(
                    p("Paulo Vitor", Position.GK, 70, 35, "BRA"), p("Damion Lowe", Position.DEF, 70, 31, "JAM"),
                    p("Juan Sebastian Pedroza", Position.MID, 70, 25, "COL"), p("Christian Bassogog", Position.FWD, 71, 29, "CMR"),
                ),
            ),
            RealClubSeed(
                id = "dam", name = "Damac", nameAr = "ضمك", shortName = "DAM",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Khamis Mushait", stadium = "Damac Club Stadium",
                primaryHex = "#D97706", secondaryHex = "#DC2626",
                crestSpec = defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FACC15"),
                budgetM = 11.5, clubOvr = 70, formation = Formation.F4231,
                players = listOf(
                    p("Florin Nita", Position.GK, 73, 37, "ROU"), p("Abdelkader Bedrane", Position.DEF, 71, 32, "ALG"),
                    p("Nicolae Stanciu", Position.MID, 75, 31, "ROU"), p("Georges-Kevin N'Koudou", Position.FWD, 76, 29, "CMR"),
                    p("Habib Diallo", Position.FWD, 74, 29, "SEN"),
                ),
            ),
            RealClubSeed(
                id = "weh", name = "Al-Wehda", nameAr = "الوحدة", shortName = "WEH",
                country = CountryCode.KSA, divisionId = "ksa_t1", tier = 1,
                city = "Mecca", stadium = "King Abdul Aziz Stadium",
                primaryHex = "#B91C1C", secondaryHex = "#FFFFFF",
                crestSpec = defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CRESCENT, "#FFFFFF"),
                budgetM = 10.0, clubOvr = 69, formation = Formation.F4231,
                players = listOf(
                    p("Ignacio de Arruabarrena", Position.GK, 71, 27, "URU"), p("Jawad El Yamiq", Position.DEF, 72, 32, "MAR"),
                    p("Juninho Bacuna", Position.MID, 72, 27, "CUW"), p("Craig Goodwin", Position.FWD, 74, 32, "AUS"),
                    p("Odion Ighalo", Position.FWD, 73, 35, "NGA"),
                ),
            ),

            // ==================== TIER 2: YELO LEAGUE (18 CLUBS) ====================
            RealClubSeed(
                id = "abha", name = "Abha", nameAr = "أبها", shortName = "ABH",
                country = CountryCode.KSA, divisionId = "ksa_t2", tier = 2,
                city = "Abha", stadium = "Prince Sultan bin Abdul Aziz",
                primaryHex = "#0B3C6D", secondaryHex = "#D91B24",
                crestSpec = defaultSpec(
                    shape = CrestShape.CLASSIC_SHIELD,
                    pattern = CrestPattern.VERTICAL_STRIPES,
                    symbol = CrestSymbol.EAGLE,
                    accentHex = "#0B3C6D",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><path d='M24 14 H76 V54 C76 72 50 84 50 84 C50 84 24 72 24 54 Z' fill='#FFFFFF' stroke='#0B3C6D' stroke-width='5'/><polygon points='50,32 62,50 50,68 38,50' fill='#D91B24'/></svg>",
                ),
                budgetM = 4.5, clubOvr = 66, formation = Formation.F4231,
                players = listOf(
                    p("Ciprian Tatarusanu", Position.GK, 69, 38, "ROU"), p("Fabian Noguera", Position.DEF, 68, 31, "ARG"),
                    p("Hassan Al-Ali", Position.FWD, 66, 22),
                ),
            ),
            RealClubSeed(
                id = "al-faisaly-fc", name = "Al-Faisaly", nameAr = "الفيصلي", shortName = "FAI",
                country = CountryCode.KSA, divisionId = "ksa_t2", tier = 2,
                city = "Harmah", stadium = "Al Majma'ah Sports City",
                primaryHex = "#6D121D", secondaryHex = "#FFFFFF",
                crestSpec = defaultSpec(
                    shape = CrestShape.ROUNDEL,
                    pattern = CrestPattern.SOLID,
                    symbol = CrestSymbol.CROWN,
                    accentHex = "#FFFFFF",
                    customSvg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><circle cx='50' cy='50' r='44' fill='#6D121D' stroke='#FFFFFF' stroke-width='4'/><circle cx='50' cy='50' r='26' fill='none' stroke='#FFFFFF' stroke-width='3'/><polygon points='50,34 62,46 58,62 42,62 38,46' fill='#FFFFFF'/></svg>",
                ),
                budgetM = 4.8, clubOvr = 67, formation = Formation.F433,
                players = listOf(
                    p("Vladimir Stojkovic", Position.GK, 69, 41, "SRB"), p("Raphael Silva", Position.DEF, 68, 32, "BRA"),
                    p("Lucas Souza", Position.MID, 69, 34, "BRA"), p("Morato", Position.FWD, 68, 32, "BRA"),
                ),
            ),
            RealClubSeed("naj", "Al-Najma", "NAJ", CountryCode.KSA, "ksa_t2", 2, "Unaizah", "Dept of Education Stadium", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 4.2, 66, Formation.F442, listOf(p("Ali Al-Amri", Position.GK, 65, 28), p("Sylla Sow", Position.FWD, 67, 28, "NED"))),
            RealClubSeed("ada", "Al-Adalah", "ADA", CountryCode.KSA, "ksa_t2", 2, "Al-Hasa", "Prince Abdullah bin Jalawi", "#DC2626", "#1E3A8A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.CHEVRON, CrestSymbol.CRESCENT, "#FFFFFF"), 3.8, 66, Formation.F4231, listOf(p("Milan Mijatovic", Position.GK, 67, 37, "MNE"), p("Rakan Al-Shamlan", Position.FWD, 65, 26))),
            RealClubSeed("tai", "Al-Tai", "TAI", CountryCode.KSA, "ksa_t2", 2, "Ha'il", "Prince Abdul Aziz bin Musa'ed", "#94A3B8", "#111827", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 4.2, 66, Formation.F433, listOf(p("Victor Braga", Position.GK, 67, 32, "BRA"), p("Robert Bauer", Position.DEF, 67, 29, "GER"), p("Virgil Misidjan", Position.FWD, 69, 31, "SUR"))),
            RealClubSeed("oro", "Al-Orobah", "ORO", CountryCode.KSA, "ksa_t2", 2, "Sakakah", "Al-Jouf University Stadium", "#16A34A", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LAUREL, "#FACC15"), 4.0, 66, Formation.F4231, listOf(p("Gaetan Coucke", Position.GK, 69, 26, "BEL"), p("Kurt Zouma", Position.DEF, 73, 30, "FRA"), p("Cristian Tello", Position.FWD, 71, 33, "ESP"))),
            RealClubSeed("bat_ksa", "Al-Batin", "BAT", CountryCode.KSA, "ksa_t2", 2, "Hafar Al-Batin", "Al-Batin Club Stadium", "#38BDF8", "#111827", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 3.2, 65, Formation.F4231, listOf(p("Martin Campana", Position.GK, 67, 35, "URU"), p("Yousef Al-Shammari", Position.FWD, 66, 26))),
            RealClubSeed("jab_ksa", "Al-Jabalain", "JAB", CountryCode.KSA, "ksa_t2", 2, "Ha'il", "Prince Abdul Aziz bin Musa'ed", "#78350F", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FACC15"), 3.4, 66, Formation.F4231, listOf(p("Leo Lacroix", Position.DEF, 67, 32, "SUI"), p("Kaka Mendes", Position.FWD, 67, 29, "BRA"))),
            RealClubSeed("buk_ksa", "Al-Bukiryah", "BUK", CountryCode.KSA, "ksa_t2", 2, "Al-Bukayriyah", "Al-Bukiryah Stadium", "#7C3AED", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 3.0, 65, Formation.F433, listOf(p("Fernando Pinheiro", Position.MID, 66, 28, "BRA"))),
            RealClubSeed("ara_ksa", "Al-Arabi KSA", "ARK", CountryCode.KSA, "ksa_t2", 2, "Unaizah", "Dept of Education Stadium", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FFFFFF"), 3.2, 65, Formation.F442, listOf(p("Romell Quioto", Position.FWD, 68, 33, "HON"))),
            RealClubSeed("zul_ksa", "Al-Zulfi", "ZUL", CountryCode.KSA, "ksa_t2", 2, "Al-Zulfi", "Al-Zulfi Stadium", "#F97316", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CRESCENT, "#FFFFFF"), 2.8, 64, Formation.F4231, listOf(p("Sandro Manoel", Position.MID, 65, 35, "BRA"))),
            RealClubSeed("ain_ksa", "Al-Ain KSA", "AIK", CountryCode.KSA, "ksa_t2", 2, "Al-Baha", "King Saud Sport City", "#2563EB", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.EAGLE, "#FACC15"), 2.9, 64, Formation.F442, listOf(p("Mohammed Fouzair", Position.MID, 67, 32, "MAR"))),
            RealClubSeed("jan_ksa", "Al-Jandal", "JAN", CountryCode.KSA, "ksa_t2", 2, "Dumat Al-Jandal", "Al-Orobah Stadium", "#16A34A", "#111827", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 2.7, 64, Formation.F433, listOf(p("Kwame Bonsu", Position.MID, 65, 30, "GHA"))),
            RealClubSeed("jed_ksa", "Jeddah Club", "JED", CountryCode.KSA, "ksa_t2", 2, "Jeddah", "King Abdullah Annex", "#1E3A8A", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.ANCHOR, "#FFFFFF"), 2.8, 64, Formation.F4231, listOf(p("Medwin Biteghe", Position.MID, 65, 28, "GAB"))),
            RealClubSeed("oho_ksa", "Ohod", "OHO", CountryCode.KSA, "ksa_t2", 2, "Medina", "Prince Mohammed bin Abdul Aziz", "#FACC15", "#78350F", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#78350F"), 2.8, 64, Formation.F442, listOf(p("Karim Aribi", Position.FWD, 66, 30, "ALG"))),
            RealClubSeed("saf_ksa", "Al-Safa", "SAF", CountryCode.KSA, "ksa_t2", 2, "Safwa", "Al-Safa Stadium", "#F97316", "#111827", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"), 2.6, 63, Formation.F4231, listOf(p("Oussama Darfalou", Position.FWD, 66, 31, "ALG"))),
            RealClubSeed("tar_ksa", "Al-Taraji", "TAR", CountryCode.KSA, "ksa_t2", 2, "Qatif", "Prince Nayef bin Abdul Aziz", "#2563EB", "#FACC15", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FACC15"), 2.5, 63, Formation.F442, listOf(p("Ibrahima Tandia", Position.FWD, 65, 31, "MLI"))),
            RealClubSeed("qai_ksa", "Al-Qaisumah", "QAI", CountryCode.KSA, "ksa_t2", 2, "Qaisumah", "Al-Batin Club Stadium", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 2.5, 63, Formation.F433, listOf(p("Lucas Gaúcho", Position.FWD, 65, 33, "BRA"))),

            // ==================== TIER 3: SAUDI SECOND DIVISION (16 CLUBS) ====================
            RealClubSeed("dir", "Al-Diriyah", "DIR", CountryCode.KSA, "ksa_t3", 3, "Diriyah", "Diriyah Stadium", "#854D0E", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FACC15"), 2.4, 62, Formation.F433, listOf(p("Moussa Marega", Position.FWD, 70, 33, "MLI"), p("Hussain Al-Shuwaish", Position.DEF, 62, 35))),
            RealClubSeed("ula", "Al-Ula", "ULA", CountryCode.KSA, "ksa_t3", 3, "AlUla", "Al-Ula Sports City", "#D97706", "#1E293B", defaultSpec(CrestShape.DIAMOND, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 3.0, 62, Formation.F4231, listOf(p("Allan", Position.MID, 71, 33, "BRA"), p("Christian Atsu", Position.MID, 63, 30, "GHA"))),
            RealClubSeed("kaw", "Al-Kawkab", "KAW", CountryCode.KSA, "ksa_t3", 3, "Al-Kharj", "Al-Shoalah Stadium", "#2563EB", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 0.8, 59, Formation.F442, listOf(p("Fahad Al-Harbi", Position.DEF, 60, 27))),
            RealClubSeed("haj", "Hajer", "HAJ", CountryCode.KSA, "ksa_t3", 3, "Al-Hasa", "Hajer Stadium", "#FFFFFF", "#111827", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CRESCENT, "#111827"), 0.9, 60, Formation.F442, listOf(p("Mazen Al-Qahtani", Position.GK, 60, 26))),
            RealClubSeed("ans", "Al-Ansar", "ANS", CountryCode.KSA, "ksa_t3", 3, "Medina", "Prince Mohammed bin Abdul Aziz", "#F97316", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.CRESCENT, "#FFFFFF"), 0.7, 59, Formation.F433, listOf(p("Mansour Al-Muwallad", Position.FWD, 60, 27))),
            RealClubSeed("jee", "Al-Jeel", "JEE", CountryCode.KSA, "ksa_t3", 3, "Al-Hasa", "Prince Abdullah bin Jalawi", "#1D4ED8", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.BOLT, "#FACC15"), 0.7, 59, Formation.F442, listOf(p("Abdullah Al-Dawsari", Position.MID, 59, 26))),
            RealClubSeed("was_ksa", "Al-Washm", "WAS", CountryCode.KSA, "ksa_t3", 3, "Shaqra", "Al-Washm Stadium", "#7C3AED", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 0.7, 59, Formation.F433, listOf(p("Abdulrahman Al-Obaid", Position.DEF, 60, 31))),
            RealClubSeed("raw_ksa", "Al-Rawdhah", "RAW", CountryCode.KSA, "ksa_t3", 3, "Al-Jishah", "Prince Abdullah bin Jalawi", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LAUREL, "#FFFFFF"), 0.7, 58, Formation.F442, listOf(p("Sultan Al-Shammeri", Position.MID, 59, 27))),
            RealClubSeed("thu_ksa", "Al-Thuqbah", "THU", CountryCode.KSA, "ksa_t3", 3, "Khobar", "Prince Saud bin Jalawi", "#DC2626", "#111827", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.BOLT, "#FFFFFF"), 0.65, 58, Formation.F442, listOf(p("Nasser Al-Otaibi", Position.FWD, 59, 26))),
            RealClubSeed("ray_ksa", "Al-Rayyan KSA", "RYK", CountryCode.KSA, "ksa_t3", 3, "Ha'il", "Al-Rayyan Stadium", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 0.65, 58, Formation.F433, listOf(p("Fahad Al-Rashidi", Position.MID, 59, 27))),
            RealClubSeed("saq_ksa", "Al-Saqer", "SAQ", CountryCode.KSA, "ksa_t3", 3, "Buraidah", "Al-Taawoun Annex", "#16A34A", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FACC15"), 0.75, 59, Formation.F4231, listOf(p("Mohammed Al-Sahli", Position.FWD, 60, 26))),
            RealClubSeed("wej_ksa", "Wej", "WEJ", CountryCode.KSA, "ksa_t3", 3, "Taif", "King Fahd Stadium", "#F97316", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 0.7, 59, Formation.F442, listOf(p("Waleed Al-Harbi", Position.DEF, 59, 28))),
            RealClubSeed("njr_ksa", "Najran SC", "NJR", CountryCode.KSA, "ksa_t3", 3, "Najran", "Prince Hathloul Stadium", "#DC2626", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FACC15"), 0.85, 60, Formation.F4231, listOf(p("Hussain Al-Mansoor", Position.MID, 61, 27))),
            RealClubSeed("ent_ksa", "Al-Entesar", "ENT", CountryCode.KSA, "ksa_t3", 3, "Rabigh", "Al-Entesar Ground", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 0.65, 58, Formation.F442, listOf(p("Bader Al-Johani", Position.FWD, 58, 25))),
            RealClubSeed("bis_ksa", "Bisha", "BSH", CountryCode.KSA, "ksa_t3", 3, "Bisha", "Bisha Sports City", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.CRESCENT, "#FFFFFF"), 0.65, 58, Formation.F433, listOf(p("Tariq Al-Qahtani", Position.MID, 59, 26))),
            RealClubSeed("afi_ksa", "Afif", "AFI", CountryCode.KSA, "ksa_t3", 3, "Afif", "Afif Municipal Ground", "#2563EB", "#FACC15", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FACC15"), 0.6, 58, Formation.F442, listOf(p("Saad Al-Otaibi", Position.DEF, 58, 25))),
        )

        return seeds.map { RealPlayerRosterBuilder.buildClub(it, saudiPool) }
    }
}
