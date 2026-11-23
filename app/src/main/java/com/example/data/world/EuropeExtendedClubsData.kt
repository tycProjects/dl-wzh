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
 * Modular Real-World European Expansion:
 * - Portugal: Liga Portugal Betclic & Liga Portugal 2
 * - Netherlands: Eredivisie & Eerste Divisie
 * - Turkey: Super Lig
 * - Additional UEFA clubs across Scotland, Belgium, Austria, Greece, Ukraine, Croatia,
 *   Serbia, Czechia, Switzerland, Denmark, Norway, Sweden, Poland, Hungary, Azerbaijan,
 *   Romania, Slovakia, Bulgaria, Ireland, Iceland, Israel, Cyprus, Latvia, Wales, Slovenia
 *   so UCL (36), UEL (36), and UECL (36) each run with 36 distinct real clubs.
 */
object EuropeExtendedClubsData {

    val divisions: List<DivisionInfo> = listOf(
        // Portugal
        DivisionInfo("por_t1", "Liga Portugal", CountryCode.POR, 1, 0, 0, 2, uclSpots = 2, uelSpots = 1, ueclSpots = 1),
        DivisionInfo("por_t2", "Liga Portugal 2", CountryCode.POR, 2, 2, 2, 0),
        // Netherlands
        DivisionInfo("ned_t1", "Eredivisie", CountryCode.NED, 1, 0, 0, 2, uclSpots = 2, uelSpots = 1, ueclSpots = 1),
        DivisionInfo("ned_t2", "Eerste Divisie", CountryCode.NED, 2, 2, 2, 0),
        // Turkey
        DivisionInfo("tur_t1", "Super Lig", CountryCode.TUR, 1, 0, 0, 2, uclSpots = 2, uelSpots = 1, ueclSpots = 1),
    )

    private fun p(name: String, pos: Position, ovr: Int, age: Int, nat: String) =
        RealPlayerSeed(name, pos, ovr, age, nat)

    private val portugalPool = listOf(
        p("Diogo Costa", Position.GK, 84, 25, "POR"), p("Rui Silva", Position.GK, 78, 30, "POR"),
        p("Anatoliy Trubin", Position.GK, 80, 23, "UKR"), p("Lukas Hornicek", Position.GK, 74, 22, "CZE"),
        p("Bruno Varela", Position.GK, 75, 30, "CPV"), p("Fabiano", Position.GK, 68, 36, "BRA"),
        p("Goncalo Inacio", Position.DEF, 81, 23, "POR"), p("Antonio Silva", Position.DEF, 80, 21, "POR"),
        p("Nicolas Otamendi", Position.DEF, 80, 36, "ARG"), p("Tomas Araujo", Position.DEF, 78, 22, "POR"),
        p("Ousmane Diomande", Position.DEF, 79, 21, "CIV"), p("Nehuen Perez", Position.DEF, 77, 24, "ARG"),
        p("Francisco Moura", Position.DEF, 76, 25, "POR"), p("Sikou Niakate", Position.DEF, 75, 25, "MLI"),
        p("Morten Hjulmand", Position.MID, 82, 25, "DEN"), p("Orkun Kokcu", Position.MID, 80, 23, "TUR"),
        p("Florentino Luis", Position.MID, 79, 25, "POR"), p("Fredrik Aursnes", Position.MID, 80, 29, "NOR"),
        p("Nico Gonzalez", Position.MID, 79, 22, "ESP"), p("Alan Varela", Position.MID, 79, 23, "ARG"),
        p("Rodrigo Zalazar", Position.MID, 76, 25, "URU"), p("Tiago Silva", Position.MID, 75, 31, "POR"),
        p("Viktor Gyokeres", Position.FWD, 86, 26, "SWE"), p("Vangelis Pavlidis", Position.FWD, 80, 26, "GRE"),
        p("Kerem Akturkoglu", Position.FWD, 80, 26, "TUR"), p("Angel Di Maria", Position.FWD, 82, 36, "ARG"),
        p("Samu Aghehowa", Position.FWD, 80, 20, "ESP"), p("Francisco Trincao", Position.FWD, 80, 24, "POR"),
        p("Geovany Quenda", Position.FWD, 77, 17, "POR"), p("Ricardo Horta", Position.FWD, 78, 30, "POR"),
    )

    private val netherlandsPool = listOf(
        p("Walter Benitez", Position.GK, 79, 31, "ARG"), p("Remko Pasveer", Position.GK, 74, 41, "NED"),
        p("Timon Wellenreuther", Position.GK, 76, 29, "GER"), p("Rome-Jayden Owusu-Oduro", Position.GK, 72, 20, "NED"),
        p("Lars Unnerstall", Position.GK, 77, 34, "GER"), p("Nick Olij", Position.GK, 75, 29, "NED"),
        p("Olivier Boscagli", Position.DEF, 80, 27, "FRA"), p("Ryan Flamingo", Position.DEF, 77, 21, "NED"),
        p("Jorrel Hato", Position.DEF, 78, 18, "NED"), p("Josip Sutalo", Position.DEF, 78, 24, "CRO"),
        p("David Hancko", Position.DEF, 81, 27, "SVK"), p("Lutsharel Geertruida", Position.DEF, 79, 24, "NED"),
        p("Wouter Goes", Position.DEF, 74, 20, "NED"), p("Mees Hilgers", Position.DEF, 75, 23, "IDN"),
        p("Joey Veerman", Position.MID, 80, 26, "NED"), p("Jerdy Schouten", Position.MID, 80, 27, "NED"),
        p("Malik Tillman", Position.MID, 79, 22, "USA"), p("Kenneth Taylor", Position.MID, 77, 22, "NED"),
        p("Jordan Henderson", Position.MID, 78, 34, "ENG"), p("Quinten Timber", Position.MID, 79, 23, "NED"),
        p("Sem Steijn", Position.MID, 78, 23, "NED"), p("Peer Koopmeiners", Position.MID, 74, 24, "NED"),
        p("Luuk de Jong", Position.FWD, 80, 34, "NED"), p("Noa Lang", Position.FWD, 79, 25, "NED"),
        p("Johan Bakayoko", Position.FWD, 79, 21, "BEL"), p("Brian Brobbey", Position.FWD, 77, 22, "NED"),
        p("Igor Paixao", Position.FWD, 78, 24, "BRA"), p("Troy Parrott", Position.FWD, 75, 22, "IRL"),
        p("Michel Vlap", Position.MID, 74, 27, "NED"), p("Henk Veerman", Position.FWD, 70, 33, "NED"),
    )

    private val turkeyPool = listOf(
        p("Fernando Muslera", Position.GK, 78, 38, "URU"), p("Dominik Livakovic", Position.GK, 80, 29, "CRO"),
        p("Mert Gunok", Position.GK, 77, 35, "TUR"), p("Ugurcan Cakir", Position.GK, 79, 28, "TUR"),
        p("Muhammed Sengezer", Position.GK, 73, 27, "TUR"), p("Berke Ozer", Position.GK, 74, 24, "TUR"),
        p("Davinson Sanchez", Position.DEF, 81, 28, "COL"), p("Abdulkerim Bardakci", Position.DEF, 78, 30, "TUR"),
        p("Milan Skriniar", Position.DEF, 81, 29, "SVK"), p("Caglar Soyuncu", Position.DEF, 77, 28, "TUR"),
        p("Gabriel Paulista", Position.DEF, 77, 34, "BRA"), p("Stefan Savic", Position.DEF, 78, 33, "MNE"),
        p("Leo Duarte", Position.DEF, 74, 28, "BRA"), p("Rick van Drongelen", Position.DEF, 73, 25, "NED"),
        p("Lucas Torreira", Position.MID, 81, 28, "URU"), p("Gabriel Sara", Position.MID, 79, 25, "BRA"),
        p("Fred", Position.MID, 80, 31, "BRA"), p("Sebastian Szymanski", Position.MID, 79, 25, "POL"),
        p("Gedson Fernandes", Position.MID, 78, 25, "POR"), p("Rafa Silva", Position.MID, 80, 31, "POR"),
        p("Batista Mendy", Position.MID, 76, 24, "FRA"), p("Olivier Ntcham", Position.MID, 74, 28, "CMR"),
        p("Victor Osimhen", Position.FWD, 87, 25, "NGA"), p("Mauro Icardi", Position.FWD, 83, 31, "ARG"),
        p("Dries Mertens", Position.FWD, 79, 37, "BEL"), p("Youssef En-Nesyri", Position.FWD, 81, 27, "MAR"),
        p("Edin Dzeko", Position.FWD, 80, 38, "BIH"), p("Ciro Immobile", Position.FWD, 80, 34, "ITA"),
        p("Simon Banza", Position.FWD, 78, 28, "COD"), p("Krzysztof Piatek", Position.FWD, 77, 29, "POL"),
    )

    private val uefaOtherPool = listOf(
        p("Kasper Schmeichel", Position.GK, 77, 38, "DEN"), p("Jack Butland", Position.GK, 75, 31, "ENG"),
        p("Simon Mignolet", Position.GK, 77, 36, "BEL"), p("Alexander Schlager", Position.GK, 75, 28, "AUT"),
        p("Dominik Kotarski", Position.GK, 75, 24, "CRO"), p("Dmytro Riznyk", Position.GK, 75, 25, "UKR"),
        p("Cameron Carter-Vickers", Position.DEF, 77, 26, "USA"), p("James Tavernier", Position.DEF, 76, 33, "ENG"),
        p("Brandon Mechele", Position.DEF, 75, 31, "BEL"), p("Jan Vertonghen", Position.DEF, 76, 37, "BEL"),
        p("Strahinja Erakovic", Position.DEF, 74, 23, "SRB"), p("Mykola Matviyenko", Position.DEF, 77, 28, "UKR"),
        p("Kevin Mac Allister", Position.DEF, 75, 27, "ARG"), p("Rodinei", Position.DEF, 75, 32, "BRA"),
        p("Callum McGregor", Position.MID, 77, 31, "SCO"), p("Hans Vanaken", Position.MID, 78, 32, "BEL"),
        p("Oscar Gloukh", Position.MID, 78, 20, "ISR"), p("Georgiy Sudakov", Position.MID, 79, 22, "UKR"),
        p("Martin Baturina", Position.MID, 78, 22, "CRO"), p("Lukas Provod", Position.MID, 76, 28, "CZE"),
        p("Patrick Berg", Position.MID, 76, 27, "NOR"), p("Ayoub El Kaabi", Position.FWD, 78, 31, "MAR"),
        p("Kyogo Furuhashi", Position.FWD, 76, 29, "JPN"), p("Kasper Dolberg", Position.FWD, 76, 27, "DEN"),
        p("Christos Tzolis", Position.FWD, 77, 22, "GRE"), p("Bruno Petkovic", Position.FWD, 76, 30, "CRO"),
        p("Fotis Ioannidis", Position.FWD, 76, 24, "GRE"), p("Tomas Chory", Position.FWD, 75, 29, "CZE"),
        p("Xherdan Shaqiri", Position.FWD, 76, 33, "SUI"), p("Barnabas Varga", Position.FWD, 75, 30, "HUN"),
    )

    fun createClubs(): List<Club> {
        val por = createPortugalSeeds().map { RealPlayerRosterBuilder.buildClub(it, portugalPool) }
        val ned = createNetherlandsSeeds().map { RealPlayerRosterBuilder.buildClub(it, netherlandsPool) }
        val tur = createTurkeySeeds().map { RealPlayerRosterBuilder.buildClub(it, turkeyPool) }
        val euroOther = createUefaOtherSeeds().map { RealPlayerRosterBuilder.buildClub(it, uefaOtherPool) }
        return por + ned + tur + euroOther
    }

    private fun createPortugalSeeds(): List<RealClubSeed> = listOf(
        // Tier 1: Liga Portugal
        RealClubSeed(
            "scp_por", "Sporting CP", "SCP", CountryCode.POR, "por_t1", 1, "Lisbon", "Estadio Jose Alvalade",
            "#008057", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.LION, "#FACC15"),
            68.0, 82, Formation.F352,
            listOf(
                p("Rui Silva", Position.GK, 79, 30, "POR"), p("Goncalo Inacio", Position.DEF, 81, 23, "POR"),
                p("Ousmane Diomande", Position.DEF, 79, 21, "CIV"), p("Morten Hjulmand", Position.MID, 82, 25, "DEN"),
                p("Pedro Goncalves", Position.MID, 81, 26, "POR"), p("Francisco Trincao", Position.FWD, 80, 24, "POR"),
                p("Viktor Gyokeres", Position.FWD, 86, 26, "SWE"), p("Geovany Quenda", Position.FWD, 77, 17, "POR"),
            ),
        ),
        RealClubSeed(
            "slb", "Benfica", "SLB", CountryCode.POR, "por_t1", 1, "Lisbon", "Estadio da Luz",
            "#E30613", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FACC15"),
            72.0, 82, Formation.F4231,
            listOf(
                p("Anatoliy Trubin", Position.GK, 80, 23, "UKR"), p("Nicolas Otamendi", Position.DEF, 80, 36, "ARG"),
                p("Antonio Silva", Position.DEF, 80, 21, "POR"), p("Florentino Luis", Position.MID, 79, 25, "POR"),
                p("Orkun Kokcu", Position.MID, 80, 23, "TUR"), p("Fredrik Aursnes", Position.MID, 80, 29, "NOR"),
                p("Angel Di Maria", Position.FWD, 82, 36, "ARG"), p("Vangelis Pavlidis", Position.FWD, 80, 26, "GRE"),
            ),
        ),
        RealClubSeed(
            "fcp", "FC Porto", "FCP", CountryCode.POR, "por_t1", 1, "Porto", "Estadio do Dragao",
            "#00428C", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#D4AF37"),
            58.0, 81, Formation.F4231,
            listOf(
                p("Diogo Costa", Position.GK, 85, 25, "POR"), p("Nehuen Perez", Position.DEF, 77, 24, "ARG"),
                p("Francisco Moura", Position.DEF, 76, 25, "POR"), p("Alan Varela", Position.MID, 79, 23, "ARG"),
                p("Nico Gonzalez", Position.MID, 79, 22, "ESP"), p("Pepe", Position.FWD, 78, 27, "BRA"),
                p("Samu Aghehowa", Position.FWD, 80, 20, "ESP"), p("Rodrigo Mora", Position.MID, 76, 17, "POR"),
            ),
        ),
        RealClubSeed(
            "scb", "SC Braga", "BRG", CountryCode.POR, "por_t1", 1, "Braga", "Estadio Municipal de Braga",
            "#CC0000", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"),
            26.0, 77, Formation.F4231,
            listOf(
                p("Matheus", Position.GK, 76, 32, "BRA"), p("Sikou Niakate", Position.DEF, 75, 25, "MLI"),
                p("Joao Moutinho", Position.MID, 76, 38, "POR"), p("Rodrigo Zalazar", Position.MID, 76, 25, "URU"),
                p("Ricardo Horta", Position.FWD, 78, 30, "POR"), p("Bruma", Position.FWD, 77, 30, "POR"),
            ),
        ),
        RealClubSeed(
            "vsc", "Vitoria Guimaraes", "VSC", CountryCode.POR, "por_t1", 1, "Guimaraes", "D. Afonso Henriques",
            "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#111111"),
            14.0, 74, Formation.F433,
            listOf(
                p("Bruno Varela", Position.GK, 75, 30, "CPV"), p("Toni Borevkovic", Position.DEF, 73, 27, "CRO"),
                p("Tiago Silva", Position.MID, 75, 31, "POR"), p("Samu", Position.MID, 73, 28, "POR"),
                p("Nuno Santos", Position.FWD, 73, 25, "POR"),
            ),
        ),
        RealClubSeed("fam", "Famalicao", "FAM", CountryCode.POR, "por_t1", 1, "Vila Nova", "Estadio Municipal", "#003882", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 10.0, 72, Formation.F4231, listOf(p("Lazar Carevic", Position.GK, 71, 25, "MNE"), p("Gustavo Sa", Position.MID, 74, 20, "POR"))),
        RealClubSeed("scl", "Santa Clara", "SCL", CountryCode.POR, "por_t1", 1, "Ponta Delgada", "Estadio de Sao Miguel", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FACC15"), 8.5, 71, Formation.F352, listOf(p("Gabriel Batista", Position.GK, 71, 26, "BRA"), p("Vinicius Lopes", Position.FWD, 72, 25, "BRA"))),
        RealClubSeed("est_por", "Estoril Praia", "EST", CountryCode.POR, "por_t1", 1, "Estoril", "Antonia Coimbra", "#FACC15", "#00529F", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#00529F"), 8.0, 71, Formation.F352, listOf(p("Joel Robles", Position.GK, 72, 34, "ESP"), p("Alejandro Marques", Position.FWD, 71, 24, "ESP"))),
        // Tier 2: Liga Portugal 2
        RealClubSeed("viz", "Vizela", "VIZ", CountryCode.POR, "por_t2", 2, "Vizela", "Estadio do FC Vizela", "#2563EB", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 3.5, 67, Formation.F4231, listOf(p("Fabijan Buntic", Position.GK, 68, 27, "CRO"), p("Heinz Morschel", Position.MID, 68, 27, "DOM"))),
        RealClubSeed("mar_por", "Maritimo", "MAR", CountryCode.POR, "por_t2", 2, "Funchal", "Estadio do Maritimo", "#DC2626", "#16A34A", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FFFFFF"), 3.8, 67, Formation.F433, listOf(p("Romain Correia", Position.DEF, 66, 25, "POR"), p("Carlos Daniel", Position.MID, 67, 30, "POR"))),
        RealClubSeed("pac", "Pacos de Ferreira", "PDF", CountryCode.POR, "por_t2", 2, "Pacos", "Capital do Movel", "#FACC15", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#16A34A"), 3.2, 66, Formation.F4231, listOf(p("Marafona", Position.GK, 67, 37, "POR"), p("Rui Fonte", Position.FWD, 67, 34, "POR"))),
        RealClubSeed("acv", "Academico Viseu", "ACV", CountryCode.POR, "por_t2", 2, "Viseu", "Estadio do Fontelo", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 3.0, 66, Formation.F433, listOf(p("Andre Clovis", Position.FWD, 68, 27, "BRA"))),
        RealClubSeed("cha_por", "Chaves", "GDC", CountryCode.POR, "por_t2", 2, "Chaves", "Estadio Municipal", "#DC2626", "#1E3A8A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.TOWER, "#FFFFFF"), 3.2, 66, Formation.F442, listOf(p("Vozinha", Position.GK, 67, 38, "CPV"), p("Platiny", Position.FWD, 67, 34, "BRA"))),
        RealClubSeed("ton", "Tondela", "CDT", CountryCode.POR, "por_t2", 2, "Tondela", "Joao Cardoso", "#FACC15", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#111111"), 3.4, 67, Formation.F433, listOf(p("Bernardo Fontes", Position.GK, 67, 22, "BRA"), p("Roberto", Position.FWD, 67, 35, "POR"))),
        // Additional Liga Portugal clubs (8 -> 18)
        RealClubSeed("por1_boa", "Boavista", "BOA", CountryCode.POR, "por_t1", 1, "Porto", "Estadio do Bessa", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.QUARTERS, CrestSymbol.CROWN, "#FACC15"), 7.5, 71, Formation.F4231, listOf(p("Cesar", Position.GK, 70, 32, "BRA"), p("Robert Bozenik", Position.FWD, 72, 25, "SVK"))),
        RealClubSeed("por1_rio", "Rio Ave", "RIO", CountryCode.POR, "por_t1", 1, "Vila do Conde", "Estadio dos Arcos", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.ANCHOR, "#FFFFFF"), 7.5, 71, Formation.F4231, listOf(p("Cezary Miszta", Position.GK, 71, 23, "POL"), p("Clayton", Position.FWD, 72, 25, "BRA"))),
        RealClubSeed("por1_gil", "Gil Vicente", "GIL", CountryCode.POR, "por_t1", 1, "Barcelos", "Cidade de Barcelos", "#DC2626", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 7.5, 71, Formation.F433, listOf(p("Andrew", Position.GK, 72, 23, "BRA"), p("Felix Correia", Position.FWD, 72, 23, "POR"))),
        RealClubSeed("por1_mor", "Moreirense", "MOR", CountryCode.POR, "por_t1", 1, "Moreira de Conegos", "Parque Joaquim de Almeida", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.STAR, "#FFFFFF"), 7.2, 71, Formation.F4231, listOf(p("Kewin", Position.GK, 71, 29, "BRA"), p("Alan", Position.MID, 72, 24, "BRA"))),
        RealClubSeed("por1_aro", "Arouca", "ARO", CountryCode.POR, "por_t1", 1, "Arouca", "Municipal de Arouca", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#1D4ED8"), 7.0, 70, Formation.F4231, listOf(p("Nico Mantl", Position.GK, 70, 24, "GER"), p("Jason", Position.FWD, 71, 30, "ESP"))),
        RealClubSeed("por1_cas", "Casa Pia", "CAS", CountryCode.POR, "por_t1", 1, "Lisbon", "Pina Manique", "#111111", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 6.8, 70, Formation.F352, listOf(p("Patrick Sequeira", Position.GK, 71, 25, "CRC"), p("Cassiano", Position.FWD, 71, 35, "BRA"))),
        RealClubSeed("por1_est", "Estrela Amadora", "EAM", CountryCode.POR, "por_t1", 1, "Amadora", "Jose Gomes", "#FFFFFF", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#16A34A"), 6.5, 70, Formation.F352, listOf(p("Bruno Brigido", Position.GK, 70, 33, "BRA"), p("Kikas", Position.FWD, 70, 26, "POR"))),
        RealClubSeed("por1_far", "Farense", "FAR", CountryCode.POR, "por_t1", 1, "Faro", "Sao Luis", "#111111", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 6.2, 69, Formation.F4231, listOf(p("Ricardo Velho", Position.GK, 73, 26, "POR"), p("Tomane", Position.FWD, 70, 32, "POR"))),
        RealClubSeed("por1_nac", "Nacional", "NAC", CountryCode.POR, "por_t1", 1, "Funchal", "Estadio da Madeira", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 6.0, 69, Formation.F433, listOf(p("Lucas Franca", Position.GK, 70, 28, "BRA"), p("Isaac Tomich", Position.FWD, 69, 20, "BRA"))),
        RealClubSeed("por1_avs", "AVS Futebol SAD", "AVS", CountryCode.POR, "por_t1", 1, "Vila das Aves", "Estadio do CD Aves", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.BOLT, "#FFFFFF"), 5.8, 69, Formation.F4231, listOf(p("Guillermo Ochoa", Position.GK, 73, 39, "MEX"), p("Nene", Position.FWD, 69, 41, "BRA"))),
        // Additional Liga Portugal 2 clubs (6 -> 18)
        RealClubSeed("por2_alv", "Alverca", "ALV", CountryCode.POR, "por_t2", 2, "Alverca", "FC Alverca Stadium", "#1D4ED8", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 2.8, 66, Formation.F433, listOf(p("Joao Bravim", Position.GK, 66, 26, "BRA"), p("Anthony Carter", Position.FWD, 67, 30, "AUS"))),
        RealClubSeed("por2_pen", "Penafiel", "PEN", CountryCode.POR, "por_t2", 2, "Penafiel", "25 de Abril", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.STAR, "#FFFFFF"), 2.6, 66, Formation.F4231, listOf(p("Manuel Baldé", Position.GK, 66, 22, "GNB"), p("Gabriel Barbosa", Position.FWD, 67, 25, "BRA"))),
        RealClubSeed("por2_tor", "Torreense", "TOR", CountryCode.POR, "por_t2", 2, "Torres Vedras", "Manuel Marques", "#1D4ED8", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 2.6, 66, Formation.F352, listOf(p("Thiago Rodrigues", Position.GK, 66, 28, "BRA"), p("Manuel Pozo", Position.FWD, 67, 27, "ESP"))),
        RealClubSeed("por2_lei", "Leiria", "LEI", CountryCode.POR, "por_t2", 2, "Leiria", "Dr. Magalhaes Pessoa", "#FFFFFF", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#DC2626"), 2.8, 66, Formation.F4231, listOf(p("Pawel Kieszek", Position.GK, 67, 40, "POL"), p("Juan Munoz", Position.FWD, 67, 29, "ESP"))),
        RealClubSeed("por2_leix", "Leixoes", "LXS", CountryCode.POR, "por_t2", 2, "Matosinhos", "Estadio do Mar", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 2.5, 65, Formation.F433, listOf(p("Dani Figueira", Position.GK, 66, 26, "POR"), p("Werton", Position.FWD, 66, 21, "BRA"))),
        RealClubSeed("por2_fei", "Feirense", "FEI", CountryCode.POR, "por_t2", 2, "Santa Maria", "Marcolino de Castro", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 2.5, 65, Formation.F4231, listOf(p("Diego Altube", Position.GK, 66, 24, "ESP"), p("Leandro Antunes", Position.FWD, 66, 27, "POR"))),
        RealClubSeed("por2_fel", "Felgueiras", "FEL", CountryCode.POR, "por_t2", 2, "Felgueiras", "Dr. Machado de Matos", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 2.4, 65, Formation.F433, listOf(p("Bruno Pinto", Position.GK, 65, 29, "POR"), p("Carlos Eduardo", Position.FWD, 66, 22, "BRA"))),
        RealClubSeed("por2_aca", "Academica", "AAC", CountryCode.POR, "por_t2", 2, "Coimbra", "Cidade de Coimbra", "#111111", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 2.5, 65, Formation.F433, listOf(p("Bernardo Santos", Position.GK, 65, 25, "POR"), p("Juan Perea", Position.FWD, 65, 25, "COL"))),
        RealClubSeed("por2_maf", "Mafra", "MAF", CountryCode.POR, "por_t2", 2, "Mafra", "Municipal de Mafra", "#FACC15", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#16A34A"), 2.3, 65, Formation.F4231, listOf(p("Martin Fraisl", Position.GK, 66, 31, "AUT"), p("Monday Etim", Position.FWD, 65, 26, "NGA"))),
        RealClubSeed("por2_oli", "Oliveirense", "OLI", CountryCode.POR, "por_t2", 2, "Oliveira de Azemeis", "Carlos Osorio", "#DC2626", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LAUREL, "#FFFFFF"), 2.2, 64, Formation.F442, listOf(p("Arthur Augusto", Position.GK, 65, 25, "BRA"), p("Zé Leite", Position.FWD, 65, 25, "POR"))),
        RealClubSeed("por2_por", "Portimonense", "PTM", CountryCode.POR, "por_t2", 2, "Portimao", "Municipal de Portimao", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.EAGLE, "#FFFFFF"), 2.7, 66, Formation.F4231, listOf(p("Vinicius Silvestre", Position.GK, 66, 30, "BRA"), p("Tambe", Position.FWD, 66, 22, "CIV"))),
        RealClubSeed("por2_bel", "Belenenses", "BEL", CountryCode.POR, "por_t2", 2, "Lisbon", "Estadio do Restelo", "#0038A8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 2.6, 65, Formation.F433, listOf(p("David Grilo", Position.GK, 65, 28, "POR"), p("Camara", Position.FWD, 65, 24, "GNB"))),
    )

    private fun createNetherlandsSeeds(): List<RealClubSeed> = listOf(
        // Tier 1: Eredivisie
        RealClubSeed(
            "psv", "PSV Eindhoven", "PSV", CountryCode.NED, "ned_t1", 1, "Eindhoven", "Philips Stadion",
            "#ED1C24", "#FFFFFF", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.FLAME, "#FACC15"),
            65.0, 81, Formation.F433,
            listOf(
                p("Walter Benitez", Position.GK, 79, 31, "ARG"), p("Olivier Boscagli", Position.DEF, 80, 27, "FRA"),
                p("Ryan Flamingo", Position.DEF, 77, 21, "NED"), p("Joey Veerman", Position.MID, 80, 26, "NED"),
                p("Jerdy Schouten", Position.MID, 80, 27, "NED"), p("Malik Tillman", Position.MID, 79, 22, "USA"),
                p("Noa Lang", Position.FWD, 79, 25, "NED"), p("Luuk de Jong", Position.FWD, 80, 34, "NED"),
                p("Johan Bakayoko", Position.FWD, 79, 21, "BEL"),
            ),
        ),
        RealClubSeed(
            "aja", "Ajax", "AJA", CountryCode.NED, "ned_t1", 1, "Amsterdam", "Johan Cruijff Arena",
            "#FFFFFF", "#D2122E", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#D2122E"),
            62.0, 80, Formation.F433,
            listOf(
                p("Remko Pasveer", Position.GK, 75, 41, "NED"), p("Jorrel Hato", Position.DEF, 78, 18, "NED"),
                p("Josip Sutalo", Position.DEF, 78, 24, "CRO"), p("Jordan Henderson", Position.MID, 78, 34, "ENG"),
                p("Kenneth Taylor", Position.MID, 77, 22, "NED"), p("Davy Klaassen", Position.MID, 76, 31, "NED"),
                p("Mika Godts", Position.FWD, 76, 19, "BEL"), p("Brian Brobbey", Position.FWD, 77, 22, "NED"),
            ),
        ),
        RealClubSeed(
            "fey", "Feyenoord", "FEY", CountryCode.NED, "ned_t1", 1, "Rotterdam", "De Kuip",
            "#D2122E", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HALF_AND_HALF, CrestSymbol.CROWN, "#111111"),
            52.0, 79, Formation.F433,
            listOf(
                p("Timon Wellenreuther", Position.GK, 76, 29, "GER"), p("David Hancko", Position.DEF, 81, 27, "SVK"),
                p("Quinten Timber", Position.MID, 79, 23, "NED"), p("In-beom Hwang", Position.MID, 78, 28, "KOR"),
                p("Antoni Milambo", Position.MID, 75, 19, "NED"), p("Igor Paixao", Position.FWD, 78, 24, "BRA"),
                p("Ayase Ueda", Position.FWD, 76, 26, "JPN"),
            ),
        ),
        RealClubSeed(
            "az", "AZ Alkmaar", "AZ", CountryCode.NED, "ned_t1", 1, "Alkmaar", "AFAS Stadion",
            "#DB0021", "#FFFFFF", defaultSpec(CrestShape.DIAMOND, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"),
            28.0, 76, Formation.F4231,
            listOf(
                p("Rome-Jayden Owusu-Oduro", Position.GK, 73, 20, "NED"), p("Wouter Goes", Position.DEF, 74, 20, "NED"),
                p("Peer Koopmeiners", Position.MID, 74, 24, "NED"), p("Sven Mijnans", Position.MID, 75, 24, "NED"),
                p("Troy Parrott", Position.FWD, 75, 22, "IRL"),
            ),
        ),
        RealClubSeed(
            "twe", "FC Twente", "TWE", CountryCode.NED, "ned_t1", 1, "Enschede", "De Grolsch Veste",
            "#E30613", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"),
            22.0, 75, Formation.F4231,
            listOf(
                p("Lars Unnerstall", Position.GK, 77, 34, "GER"), p("Mees Hilgers", Position.DEF, 75, 23, "IDN"),
                p("Sem Steijn", Position.MID, 78, 23, "NED"), p("Michel Vlap", Position.MID, 74, 27, "NED"),
                p("Sam Lammers", Position.FWD, 75, 27, "NED"),
            ),
        ),
        RealClubSeed("utr", "FC Utrecht", "UTR", CountryCode.NED, "ned_t1", 1, "Utrecht", "Stadion Galgenwaard", "#E30613", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.TOWER, "#FFFFFF"), 18.0, 74, Formation.F4231, listOf(p("Vasilis Barkas", Position.GK, 74, 30, "GRE"), p("Paxten Aaronson", Position.MID, 74, 21, "USA"), p("Sebastien Haller", Position.FWD, 77, 30, "CIV"))),
        RealClubSeed("gae", "Go Ahead Eagles", "GAE", CountryCode.NED, "ned_t1", 1, "Deventer", "De Adelaarshorst", "#DC2626", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.EAGLE, "#FACC15"), 12.0, 73, Formation.F4231, listOf(p("Jari De Busser", Position.GK, 72, 25, "BEL"), p("Oliver Antman", Position.FWD, 73, 23, "FIN"))),
        RealClubSeed("nec", "NEC Nijmegen", "NEC", CountryCode.NED, "ned_t1", 1, "Nijmegen", "Goffertstadion", "#DC2626", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.EAGLE, "#FFFFFF"), 12.0, 72, Formation.F4231, listOf(p("Robin Roefs", Position.GK, 73, 21, "NED"), p("Koki Ogawa", Position.FWD, 74, 27, "JPN"))),
        // Tier 2: Eerste Divisie
        RealClubSeed("ado", "ADO Den Haag", "ADO", CountryCode.NED, "ned_t2", 2, "The Hague", "Bingoal Stadion", "#FACC15", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.EAGLE, "#111111"), 3.2, 67, Formation.F433, listOf(p("Kilian Nikiema", Position.GK, 66, 21, "BFA"), p("Lee Bonis", Position.FWD, 67, 25, "NIR"))),
        RealClubSeed("exc", "Excelsior", "EXC", CountryCode.NED, "ned_t2", 2, "Rotterdam", "Van Donge & De Roo", "#111111", "#DC2626", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#DC2626"), 3.4, 68, Formation.F4231, listOf(p("Calvin Raatsie", Position.GK, 67, 22, "NED"), p("Lance Duijvestijn", Position.MID, 68, 25, "NED"))),
        RealClubSeed("vol", "FC Volendam", "VOL", CountryCode.NED, "ned_t2", 2, "Volendam", "Kras Stadion", "#F97316", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 3.5, 68, Formation.F433, listOf(p("Henk Veerman", Position.FWD, 70, 33, "NED"), p("Robert Muhren", Position.FWD, 68, 35, "NED"))),
        RealClubSeed("cam_ned", "SC Cambuur", "CAM", CountryCode.NED, "ned_t2", 2, "Leeuwarden", "Kooi Stadion", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#1D4ED8"), 3.0, 67, Formation.F433, listOf(p("Thijs Jansen", Position.GK, 67, 23, "NED"), p("Remco Balk", Position.FWD, 67, 23, "NED"))),
        RealClubSeed("gra_ned", "De Graafschap", "GRA", CountryCode.NED, "ned_t2", 2, "Doetinchem", "De Vijverberg", "#2563EB", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.STAR, "#FFFFFF"), 2.8, 66, Formation.F433, listOf(p("Ties Wieggers", Position.GK, 66, 24, "NED"), p("Ralf Seuntjens", Position.FWD, 67, 35, "NED"))),
        RealClubSeed("vvv", "VVV-Venlo", "VVV", CountryCode.NED, "ned_t2", 2, "Venlo", "De Koel", "#FACC15", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#111111"), 2.6, 65, Formation.F442, listOf(p("Jan de Boer", Position.GK, 66, 24, "NED"), p("Navarone Foor", Position.MID, 67, 32, "NED"))),
        // Additional Eredivisie clubs (8 -> 18)
        RealClubSeed("ned1_hee", "SC Heerenveen", "HEE", CountryCode.NED, "ned_t1", 1, "Heerenveen", "Abe Lenstra Stadion", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#DC2626"), 10.0, 72, Formation.F4231, listOf(p("Andries Noppert", Position.GK, 72, 30, "NED"), p("Luuk Brouwers", Position.MID, 72, 26, "NED"), p("Ion Nicolaescu", Position.FWD, 72, 26, "MDA"))),
        RealClubSeed("ned1_spa", "Sparta Rotterdam", "SPA", CountryCode.NED, "ned_t1", 1, "Rotterdam", "Het Kasteel", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.TOWER, "#FFFFFF"), 10.0, 72, Formation.F4231, listOf(p("Nick Olij", Position.GK, 75, 29, "NED"), p("Tobias Lauritsen", Position.FWD, 72, 27, "NOR"))),
        RealClubSeed("ned1_gro", "FC Groningen", "GRO", CountryCode.NED, "ned_t1", 1, "Groningen", "Euroborg", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 9.5, 71, Formation.F442, listOf(p("Etienne Vaessen", Position.GK, 72, 29, "SUR"), p("Luciano Valente", Position.MID, 71, 21, "NED"))),
        RealClubSeed("ned1_for", "Fortuna Sittard", "FOR", CountryCode.NED, "ned_t1", 1, "Sittard", "Fortuna Sittard Stadion", "#FACC15", "#16A34A", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HALF_AND_HALF, CrestSymbol.STAR, "#FFFFFF"), 8.5, 71, Formation.F4231, listOf(p("Mattijs Branderhorst", Position.GK, 71, 30, "NED"), p("Alen Halilovic", Position.MID, 72, 28, "CRO"))),
        RealClubSeed("ned1_pec", "PEC Zwolle", "PEC", CountryCode.NED, "ned_t1", 1, "Zwolle", "MAC3PARK Stadion", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.CROWN, "#FFFFFF"), 8.0, 70, Formation.F4231, listOf(p("Jasper Schendelaar", Position.GK, 71, 24, "NED"), p("Dylan Vente", Position.FWD, 71, 25, "SUR"))),
        RealClubSeed("ned1_her", "Heracles Almelo", "HER", CountryCode.NED, "ned_t1", 1, "Almelo", "Asito Stadion", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FFFFFF"), 8.0, 70, Formation.F4231, listOf(p("Fabian de Keijzer", Position.GK, 70, 24, "NED"), p("Luka Kulenovic", Position.FWD, 71, 25, "BIH"))),
        RealClubSeed("ned1_nac", "NAC Breda", "NAC", CountryCode.NED, "ned_t1", 1, "Breda", "Rat Verlegh Stadion", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.STAR, "#111111"), 8.0, 70, Formation.F433, listOf(p("Daniel Bielica", Position.GK, 70, 25, "POL"), p("Elias Mar Omarsson", Position.FWD, 70, 29, "ISL"))),
        RealClubSeed("ned1_wil", "Willem II", "WIL", CountryCode.NED, "ned_t1", 1, "Tilburg", "Koning Willem II", "#DC2626", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FFFFFF"), 7.5, 70, Formation.F4231, listOf(p("Thomas Didillon-Hodl", Position.GK, 71, 29, "FRA"), p("Kyan Vaesen", Position.FWD, 70, 23, "BEL"))),
        RealClubSeed("ned1_rkc", "RKC Waalwijk", "RKC", CountryCode.NED, "ned_t1", 1, "Waalwijk", "Mandemakers Stadion", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#1D4ED8"), 6.5, 69, Formation.F4231, listOf(p("Jeroen Houwen", Position.GK, 69, 28, "NED"), p("Oskar Zawada", Position.FWD, 70, 28, "POL"))),
        RealClubSeed("ned1_alm", "Almere City", "ALM", CountryCode.NED, "ned_t1", 1, "Almere", "Yanmar Stadion", "#DC2626", "#111111", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 6.5, 69, Formation.F4231, listOf(p("Nordin Bakker", Position.GK, 70, 27, "NED"), p("Thomas Robinet", Position.FWD, 70, 28, "FRA"))),
        // Additional Eerste Divisie clubs (6 -> 20)
        RealClubSeed("ned2_vit", "Vitesse", "VIT", CountryCode.NED, "ned_t2", 2, "Arnhem", "GelreDome", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.EAGLE, "#111111"), 3.0, 67, Formation.F433, listOf(p("Tom Bramel", Position.GK, 66, 19, "NED"), p("Miliano Jonathans", Position.FWD, 68, 20, "NED"))),
        RealClubSeed("ned2_dor", "FC Dordrecht", "DOR", CountryCode.NED, "ned_t2", 2, "Dordrecht", "M-Scores Stadion", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 2.8, 67, Formation.F4231, listOf(p("Celton Biai", Position.GK, 66, 24, "GNB"), p("Devin Haen", Position.FWD, 67, 20, "NED"))),
        RealClubSeed("ned2_emm", "FC Emmen", "EMM", CountryCode.NED, "ned_t2", 2, "Emmen", "De Oude Meerdijk", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 2.9, 67, Formation.F4231, listOf(p("Luca Unbehaun", Position.GK, 67, 23, "GER"), p("Keliano Nsona", Position.FWD, 67, 22, "FRA"))),
        RealClubSeed("ned2_tel", "Telstar", "TEL", CountryCode.NED, "ned_t2", 2, "Velsen", "711 Stadion", "#FFFFFF", "#1D4ED8", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#1D4ED8"), 2.5, 66, Formation.F352, listOf(p("Ronald Koeman Jr", Position.GK, 67, 29, "NED"), p("Zakaria Eddahchouri", Position.FWD, 68, 24, "NED"))),
        RealClubSeed("ned2_den", "FC Den Bosch", "DBO", CountryCode.NED, "ned_t2", 2, "'s-Hertogenbosch", "De Vliert", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 2.6, 66, Formation.F4231, listOf(p("Pepijn van de Merbel", Position.GK, 66, 22, "NED"), p("Byron Burgering", Position.FWD, 67, 24, "NED"))),
        RealClubSeed("ned2_rod", "Roda JC", "ROD", CountryCode.NED, "ned_t2", 2, "Kerkrade", "Parkstad Limburg", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#111111"), 3.0, 67, Formation.F4231, listOf(p("Justin Treichel", Position.GK, 66, 21, "GER"), p("Thibo Baeten", Position.FWD, 67, 22, "BEL"))),
        RealClubSeed("ned2_mvv", "MVV Maastricht", "MVV", CountryCode.NED, "ned_t2", 2, "Maastricht", "De Geusselt", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 2.4, 65, Formation.F433, listOf(p("Romain Matthys", Position.GK, 66, 26, "BEL"), p("Sven Braken", Position.FWD, 66, 31, "NED"))),
        RealClubSeed("ned2_hel", "Helmond Sport", "HEL", CountryCode.NED, "ned_t2", 2, "Helmond", "GS Staalwerken", "#DC2626", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 2.5, 66, Formation.F4231, listOf(p("Wouter van der Steen", Position.GK, 66, 34, "NED"), p("Anthony van den Hurk", Position.FWD, 67, 31, "CUW"))),
        RealClubSeed("ned2_eind", "FC Eindhoven", "FCE", CountryCode.NED, "ned_t2", 2, "Eindhoven", "Jan Louwers", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"), 2.5, 66, Formation.F352, listOf(p("Jorn Brondeel", Position.GK, 66, 31, "BEL"), p("Evan Rottier", Position.FWD, 66, 22, "NED"))),
        RealClubSeed("ned2_oss", "TOP Oss", "OSS", CountryCode.NED, "ned_t2", 2, "Oss", "Frans Heesen", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 2.2, 65, Formation.F442, listOf(p("Mike Havekotte", Position.GK, 65, 29, "NED"), p("Giovanni Korte", Position.FWD, 65, 31, "NED"))),
        RealClubSeed("ned2_jaja", "Jong Ajax", "JAJ", CountryCode.NED, "ned_t2", 2, "Amsterdam", "De Toekomst", "#FFFFFF", "#DC2626", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#DC2626"), 2.8, 66, Formation.F433, listOf(p("Charlie Setford", Position.GK, 66, 20, "ENG"), p("Julian Rijkhoff", Position.FWD, 67, 19, "NED"))),
        RealClubSeed("ned2_jpsv", "Jong PSV", "JPS", CountryCode.NED, "ned_t2", 2, "Eindhoven", "De Herdgang", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.FLAME, "#FACC15"), 2.7, 66, Formation.F433, listOf(p("Niek Schiks", Position.GK, 66, 20, "NED"), p("Jesper Uneken", Position.FWD, 66, 20, "NED"))),
        RealClubSeed("ned2_jaz", "Jong AZ", "JAZ", CountryCode.NED, "ned_t2", 2, "Wijdewormer", "AFAS Training", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.DIAMOND, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 2.6, 66, Formation.F433, listOf(p("Tristan Kuijsten", Position.GK, 65, 19, "NED"), p("Ro-Zangelo Daal", Position.FWD, 66, 20, "NED"))),
        RealClubSeed("ned2_jutr", "Jong FC Utrecht", "JUT", CountryCode.NED, "ned_t2", 2, "Utrecht", "Zoudenbalch", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.TOWER, "#FFFFFF"), 2.4, 65, Formation.F4231, listOf(p("Kevin Gadellaa", Position.GK, 65, 21, "NED"), p("Jesse van de Haar", Position.FWD, 65, 19, "NED"))),
    )

    private fun createTurkeySeeds(): List<RealClubSeed> = listOf(
        RealClubSeed(
            "gal", "Galatasaray", "GAL", CountryCode.TUR, "tur_t1", 1, "Istanbul", "RAMS Park",
            "#A90432", "#FDB912", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.HALF_AND_HALF, CrestSymbol.LION, "#FDB912"),
            65.0, 81, Formation.F4231,
            listOf(
                p("Fernando Muslera", Position.GK, 78, 38, "URU"), p("Davinson Sanchez", Position.DEF, 81, 28, "COL"),
                p("Abdulkerim Bardakci", Position.DEF, 78, 30, "TUR"), p("Lucas Torreira", Position.MID, 81, 28, "URU"),
                p("Gabriel Sara", Position.MID, 79, 25, "BRA"), p("Baris Alper Yilmaz", Position.FWD, 79, 24, "TUR"),
                p("Victor Osimhen", Position.FWD, 87, 25, "NGA"), p("Mauro Icardi", Position.FWD, 83, 31, "ARG"),
            ),
        ),
        RealClubSeed(
            "fen", "Fenerbahce", "FEN", CountryCode.TUR, "tur_t1", 1, "Istanbul", "Sukru Saracoglu",
            "#00205B", "#FFED00", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LAUREL, "#FFED00"),
            60.0, 80, Formation.F4231,
            listOf(
                p("Dominik Livakovic", Position.GK, 80, 29, "CRO"), p("Milan Skriniar", Position.DEF, 81, 29, "SVK"),
                p("Caglar Soyuncu", Position.DEF, 77, 28, "TUR"), p("Fred", Position.MID, 80, 31, "BRA"),
                p("Sebastian Szymanski", Position.MID, 79, 25, "POL"), p("Dusan Tadic", Position.MID, 80, 36, "SRB"),
                p("Youssef En-Nesyri", Position.FWD, 81, 27, "MAR"), p("Edin Dzeko", Position.FWD, 80, 38, "BIH"),
            ),
        ),
        RealClubSeed(
            "bes", "Besiktas", "BJK", CountryCode.TUR, "tur_t1", 1, "Istanbul", "Tupras Stadium",
            "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.EAGLE, "#D71920"),
            36.0, 77, Formation.F4231,
            listOf(
                p("Mert Gunok", Position.GK, 77, 35, "TUR"), p("Gabriel Paulista", Position.DEF, 77, 34, "BRA"),
                p("Gedson Fernandes", Position.MID, 78, 25, "POR"), p("Rafa Silva", Position.MID, 80, 31, "POR"),
                p("Semih Kilicsoy", Position.FWD, 76, 19, "TUR"), p("Ciro Immobile", Position.FWD, 80, 34, "ITA"),
            ),
        ),
        RealClubSeed(
            "tra", "Trabzonspor", "TS", CountryCode.TUR, "tur_t1", 1, "Trabzon", "Papara Park",
            "#7B102E", "#38BDF8", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HALF_AND_HALF, CrestSymbol.STAR, "#FFFFFF"),
            28.0, 75, Formation.F4231,
            listOf(
                p("Ugurcan Cakir", Position.GK, 79, 28, "TUR"), p("Stefan Savic", Position.DEF, 78, 33, "MNE"),
                p("Batista Mendy", Position.MID, 76, 24, "FRA"), p("Edin Visca", Position.FWD, 76, 34, "BIH"),
                p("Simon Banza", Position.FWD, 78, 28, "COD"),
            ),
        ),
        RealClubSeed("bas_tur", "Istanbul Basaksehir", "IBFK", CountryCode.TUR, "tur_t1", 1, "Istanbul", "Basaksehir Fatih Terim", "#F97316", "#1E3A8A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.BOLT, "#FFFFFF"), 20.0, 74, Formation.F433, listOf(p("Muhammed Sengezer", Position.GK, 73, 27, "TUR"), p("Krzysztof Piatek", Position.FWD, 77, 29, "POL"))),
        RealClubSeed("sam_tur", "Samsunspor", "SAM", CountryCode.TUR, "tur_t1", 1, "Samsun", "Samsun 19 Mayis", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 14.0, 73, Formation.F4231, listOf(p("Okan Kocuk", Position.GK, 73, 29, "TUR"), p("Olivier Ntcham", Position.MID, 74, 28, "CMR"), p("Marius Mouandilmadji", Position.FWD, 73, 26, "CHA"))),
        RealClubSeed("goz", "Goztepe", "GOZ", CountryCode.TUR, "tur_t1", 1, "Izmir", "Gursel Aksel", "#FACC15", "#DC2626", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HALF_AND_HALF, CrestSymbol.STAR, "#FFFFFF"), 13.0, 72, Formation.F352, listOf(p("Mateusz Lis", Position.GK, 72, 27, "POL"), p("Romulo Cardoso", Position.FWD, 74, 22, "BRA"))),
        RealClubSeed("eyu", "Eyupspor", "EYP", CountryCode.TUR, "tur_t1", 1, "Istanbul", "Recep Tayyip Erdogan", "#7C3AED", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.STAR, "#FACC15"), 12.0, 72, Formation.F4231, listOf(p("Berke Ozer", Position.GK, 74, 24, "TUR"), p("Emre Akbaba", Position.MID, 73, 32, "TUR"))),
        // Additional Super Lig clubs (8 -> 18)
        RealClubSeed("tur1_kas", "Kasimpasa", "KAS", CountryCode.TUR, "tur_t1", 1, "Istanbul", "Recep Tayyip Erdogan", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 11.0, 72, Formation.F4231, listOf(p("Andreas Gianniotis", Position.GK, 72, 31, "GRE"), p("Nuno Da Costa", Position.FWD, 72, 33, "CPV"))),
        RealClubSeed("tur1_riz", "Caykur Rizespor", "RIZ", CountryCode.TUR, "tur_t1", 1, "Rize", "Caykur Didi", "#16A34A", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LAUREL, "#FFFFFF"), 10.5, 72, Formation.F4231, listOf(p("Ivo Grbic", Position.GK, 73, 28, "CRO"), p("Ali Sowe", Position.FWD, 72, 30, "GAM"))),
        RealClubSeed("tur1_gaz", "Gaziantep FK", "GAZ", CountryCode.TUR, "tur_t1", 1, "Gaziantep", "Gaziantep Stadium", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 10.0, 71, Formation.F4231, listOf(p("Sokratis Dioudis", Position.GK, 71, 31, "GRE"), p("Alexandru Maxim", Position.MID, 73, 34, "ROU"))),
        RealClubSeed("tur1_ant", "Antalyaspor", "ANT", CountryCode.TUR, "tur_t1", 1, "Antalya", "Corendon Airlines Park", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.FLAME, "#FFFFFF"), 10.0, 71, Formation.F4231, listOf(p("Kenan Piric", Position.GK, 71, 30, "BIH"), p("Sam Larsson", Position.FWD, 72, 31, "SWE"))),
        RealClubSeed("tur1_ala", "Alanyaspor", "ALA", CountryCode.TUR, "tur_t1", 1, "Alanya", "GAİN Park", "#F97316", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.TOWER, "#FFFFFF"), 9.5, 71, Formation.F4231, listOf(p("Ertugrul Taskiran", Position.GK, 71, 35, "TUR"), p("Sergio Cordova", Position.FWD, 71, 27, "VEN"))),
        RealClubSeed("tur1_kon", "Konyaspor", "KON", CountryCode.TUR, "tur_t1", 1, "Konya", "Medas Konya", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 10.0, 71, Formation.F4231, listOf(p("Jakub Slowik", Position.GK, 71, 33, "POL"), p("Blaz Kramer", Position.FWD, 71, 28, "SVN"))),
        RealClubSeed("tur1_kay", "Kayserispor", "KAY", CountryCode.TUR, "tur_t1", 1, "Kayseri", "RHG Enerturk", "#FACC15", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#111111"), 9.5, 70, Formation.F4231, listOf(p("Bilal Bayazit", Position.GK, 72, 25, "TUR"), p("Duckens Nazon", Position.FWD, 71, 30, "HAI"))),
        RealClubSeed("tur1_siv", "Sivasspor", "SIV", CountryCode.TUR, "tur_t1", 1, "Sivas", "4 Eylul", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 9.5, 71, Formation.F433, listOf(p("Djordje Nikolic", Position.GK, 71, 27, "SRB"), p("Rey Manaj", Position.FWD, 74, 27, "ALB"))),
        RealClubSeed("tur1_bod", "Bodrum FK", "BOD", CountryCode.TUR, "tur_t1", 1, "Bodrum", "Bodrum Ilce", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 8.0, 69, Formation.F4231, listOf(p("Diogo Sousa", Position.GK, 70, 26, "POR"), p("George Puscas", Position.FWD, 71, 28, "ROU"))),
        RealClubSeed("tur1_hat", "Hatayspor", "HAT", CountryCode.TUR, "tur_t1", 1, "Antakya", "Mersin Stadium", "#7F1D1D", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LAUREL, "#FFFFFF"), 8.5, 70, Formation.F4231, listOf(p("Erce Kardesler", Position.GK, 71, 30, "TUR"), p("Vincent Aboubakar", Position.FWD, 75, 32, "CMR"))),
    )

    private fun createUefaOtherSeeds(): List<RealClubSeed> = listOf(
        // Scotland, Belgium, Austria, Greece, Ukraine, Croatia, Serbia, Czechia, Switzerland, Denmark, Norway, Sweden, Poland, Hungary, Azerbaijan, Romania, Slovakia, Bulgaria, Ireland, Iceland, Israel, Cyprus, Slovenia, Latvia, Wales
        RealClubSeed("cel", "Celtic", "CEL", CountryCode.EUR_OTHER, "uefa_pool", 1, "Glasgow", "Celtic Park", "#018749", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HOOPS, CrestSymbol.LAUREL, "#FFFFFF"), 38.0, 77, Formation.F433, listOf(p("Kasper Schmeichel", Position.GK, 77, 38, "DEN"), p("Cameron Carter-Vickers", Position.DEF, 77, 26, "USA"), p("Callum McGregor", Position.MID, 77, 31, "SCO"), p("Daizen Maeda", Position.FWD, 77, 27, "JPN"))),
        RealClubSeed("ran_sco", "Rangers", "RFC", CountryCode.EUR_OTHER, "uefa_pool", 1, "Glasgow", "Ibrox Stadium", "#0033A0", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 32.0, 75, Formation.F4231, listOf(p("Jack Butland", Position.GK, 75, 31, "ENG"), p("James Tavernier", Position.DEF, 76, 33, "ENG"), p("Vaclav Cerny", Position.FWD, 76, 27, "CZE"))),
        RealClubSeed("hea", "Hearts", "HEA", CountryCode.EUR_OTHER, "uefa_pool", 1, "Edinburgh", "Tynecastle Park", "#7A003C", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 10.0, 70, Formation.F4231, listOf(p("Craig Gordon", Position.GK, 71, 41, "SCO"), p("Lawrence Shankland", Position.FWD, 73, 29, "SCO"))),
        RealClubSeed("clb_bel", "Club Brugge", "CLU", CountryCode.EUR_OTHER, "uefa_pool", 1, "Bruges", "Jan Breydel", "#0055A5", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FACC15"), 36.0, 78, Formation.F4231, listOf(p("Simon Mignolet", Position.GK, 77, 36, "BEL"), p("Brandon Mechele", Position.DEF, 75, 31, "BEL"), p("Hans Vanaken", Position.MID, 78, 32, "BEL"), p("Christos Tzolis", Position.FWD, 77, 22, "GRE"))),
        RealClubSeed("and_bel", "Anderlecht", "AND", CountryCode.EUR_OTHER, "uefa_pool", 1, "Brussels", "Lotto Park", "#512B85", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#D4AF37"), 28.0, 75, Formation.F433, listOf(p("Colin Coosemans", Position.GK, 73, 32, "BEL"), p("Jan Vertonghen", Position.DEF, 76, 37, "BEL"), p("Kasper Dolberg", Position.FWD, 76, 27, "DEN"))),
        RealClubSeed("usg", "Union Saint-Gilloise", "USG", CountryCode.EUR_OTHER, "uefa_pool", 1, "Brussels", "Joseph Marien", "#FFDD00", "#00338D", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.STAR, "#00338D"), 24.0, 75, Formation.F352, listOf(p("Anthony Moris", Position.GK, 74, 34, "LUX"), p("Kevin Mac Allister", Position.DEF, 75, 27, "ARG"), p("Franjo Ivanovic", Position.FWD, 75, 21, "CRO"))),
        RealClubSeed("gen_bel", "KAA Gent", "GNT", CountryCode.EUR_OTHER, "uefa_pool", 1, "Ghent", "Planet Group Arena", "#00479C", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 20.0, 74, Formation.F4231, listOf(p("Davy Roef", Position.GK, 73, 30, "BEL"), p("Tsuyoshi Watanabe", Position.DEF, 74, 27, "JPN"))),
        RealClubSeed("rbs", "RB Salzburg", "RBS", CountryCode.EUR_OTHER, "uefa_pool", 1, "Salzburg", "Red Bull Arena", "#DD0741", "#FFFFFF", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.BOLT, "#FACC15"), 36.0, 76, Formation.F433, listOf(p("Alexander Schlager", Position.GK, 75, 28, "AUT"), p("Oscar Gloukh", Position.MID, 78, 20, "ISR"), p("Karim Konate", Position.FWD, 76, 20, "CIV"))),
        RealClubSeed("stu", "Sturm Graz", "STU", CountryCode.EUR_OTHER, "uefa_pool", 1, "Graz", "Merkur Arena", "#111111", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"), 20.0, 74, Formation.F442, listOf(p("Kjell Scherpen", Position.GK, 73, 24, "NED"), p("Otar Kiteishvili", Position.MID, 76, 28, "GEO"))),
        RealClubSeed("rap", "Rapid Wien", "RAP", CountryCode.EUR_OTHER, "uefa_pool", 1, "Vienna", "Allianz Stadion", "#00853E", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 14.0, 72, Formation.F4231, listOf(p("Niklas Hedl", Position.GK, 72, 23, "AUT"), p("Guido Burgstaller", Position.FWD, 73, 35, "AUT"))),
        RealClubSeed("oly", "Olympiacos", "OLY", CountryCode.EUR_OTHER, "uefa_pool", 1, "Piraeus", "Karaiskakis Stadium", "#E30613", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LAUREL, "#FFFFFF"), 32.0, 77, Formation.F4231, listOf(p("Konstantinos Tzolakis", Position.GK, 75, 22, "GRE"), p("Rodinei", Position.DEF, 75, 32, "BRA"), p("Ayoub El Kaabi", Position.FWD, 78, 31, "MAR"))),
        RealClubSeed("pao", "PAOK", "PAOK", CountryCode.EUR_OTHER, "uefa_pool", 1, "Thessaloniki", "Toumba Stadium", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.EAGLE, "#FFFFFF"), 24.0, 75, Formation.F4231, listOf(p("Dominik Kotarski", Position.GK, 75, 24, "CRO"), p("Andrija Zivkovic", Position.FWD, 77, 28, "SRB"))),
        RealClubSeed("aek", "AEK Athens", "AEK", CountryCode.EUR_OTHER, "uefa_pool", 1, "Athens", "OPAP Arena", "#FFCC00", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#111111"), 22.0, 74, Formation.F442, listOf(p("Levi Garcia", Position.FWD, 76, 27, "TRI"), p("Erik Lamela", Position.MID, 75, 32, "ARG"))),
        RealClubSeed("pana", "Panathinaikos", "PAO", CountryCode.EUR_OTHER, "uefa_pool", 1, "Athens", "OAKA Stadium", "#007A33", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LAUREL, "#FFFFFF"), 22.0, 74, Formation.F4231, listOf(p("Bartlomiej Dragowski", Position.GK, 74, 27, "POL"), p("Fotis Ioannidis", Position.FWD, 76, 24, "GRE"))),
        RealClubSeed("shk", "Shakhtar Donetsk", "SHK", CountryCode.EUR_OTHER, "uefa_pool", 1, "Donetsk", "Donbas Arena", "#FF5A00", "#111111", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.HALF_AND_HALF, CrestSymbol.FLAME, "#FFFFFF"), 38.0, 76, Formation.F4231, listOf(p("Dmytro Riznyk", Position.GK, 75, 25, "UKR"), p("Mykola Matviyenko", Position.DEF, 77, 28, "UKR"), p("Georgiy Sudakov", Position.MID, 79, 22, "UKR"))),
        RealClubSeed("dyn_k", "Dynamo Kyiv", "DKY", CountryCode.EUR_OTHER, "uefa_pool", 1, "Kyiv", "NSC Olimpiyskiy", "#005BBB", "#FFFFFF", defaultSpec(CrestShape.DIAMOND, CrestPattern.SOLID, CrestSymbol.STAR, "#FACC15"), 22.0, 74, Formation.F4231, listOf(p("Heorhiy Bushchan", Position.GK, 75, 30, "UKR"), p("Vladyslav Vanat", Position.FWD, 75, 22, "UKR"))),
        RealClubSeed("din_z", "Dinamo Zagreb", "DZG", CountryCode.EUR_OTHER, "uefa_pool", 1, "Zagreb", "Stadion Maksimir", "#0033A0", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 26.0, 75, Formation.F4231, listOf(p("Ivan Nevistic", Position.GK, 73, 26, "CRO"), p("Martin Baturina", Position.MID, 78, 22, "CRO"), p("Bruno Petkovic", Position.FWD, 76, 30, "CRO"))),
        RealClubSeed("haj_s", "Hajduk Split", "HAJ", CountryCode.EUR_OTHER, "uefa_pool", 1, "Split", "Poljud Stadium", "#FFFFFF", "#0033A0", defaultSpec(CrestShape.ROUNDEL, CrestPattern.QUARTERS, CrestSymbol.STAR, "#DC2626"), 14.0, 72, Formation.F4231, listOf(p("Ivan Rakitic", Position.MID, 76, 36, "CRO"), p("Marko Livaja", Position.FWD, 75, 31, "CRO"))),
        RealClubSeed("czv", "Red Star Belgrade", "CZV", CountryCode.EUR_OTHER, "uefa_pool", 1, "Belgrade", "Rajko Mitic", "#D71920", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FACC15"), 24.0, 75, Formation.F4231, listOf(p("Omri Glazer", Position.GK, 74, 28, "ISR"), p("Rade Krunic", Position.MID, 75, 31, "BIH"), p("Cherif Ndiaye", Position.FWD, 75, 28, "SEN"))),
        RealClubSeed("par_srb", "Partizan", "PAR", CountryCode.EUR_OTHER, "uefa_pool", 1, "Belgrade", "Partizan Stadium", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.FLAME, "#FFFFFF"), 12.0, 71, Formation.F4231, listOf(p("Aleksandar Jovanovic", Position.GK, 72, 32, "SRB"), p("Bibras Natcho", Position.MID, 72, 36, "ISR"))),
        RealClubSeed("spa_p", "Sparta Prague", "ACS", CountryCode.EUR_OTHER, "uefa_pool", 1, "Prague", "epet ARENA", "#9E1B32", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FACC15"), 22.0, 75, Formation.F352, listOf(p("Peter Vindahl", Position.GK, 73, 26, "DEN"), p("Veljko Birmancevic", Position.FWD, 76, 26, "SRB"))),
        RealClubSeed("sla_p", "Slavia Prague", "SLA", CountryCode.EUR_OTHER, "uefa_pool", 1, "Prague", "Fortuna Arena", "#E30613", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HALF_AND_HALF, CrestSymbol.STAR, "#E30613"), 24.0, 76, Formation.F352, listOf(p("Antonin Kinsky", Position.GK, 74, 21, "CZE"), p("Lukas Provod", Position.MID, 76, 28, "CZE"), p("Tomas Chory", Position.FWD, 75, 29, "CZE"))),
        RealClubSeed("plz", "Viktoria Plzen", "PLZ", CountryCode.EUR_OTHER, "uefa_pool", 1, "Plzen", "Doosan Arena", "#003DA5", "#D71920", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FFFFFF"), 16.0, 73, Formation.F352, listOf(p("Pavel Sulc", Position.MID, 75, 23, "CZE"), p("Matej Vydra", Position.FWD, 72, 32, "CZE"))),
        RealClubSeed("yb", "Young Boys", "YB", CountryCode.EUR_OTHER, "uefa_pool", 1, "Bern", "Wankdorf", "#FFCC00", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HALF_AND_HALF, CrestSymbol.STAR, "#111111"), 20.0, 74, Formation.F442, listOf(p("David von Ballmoos", Position.GK, 73, 29, "SUI"), p("Joël Monteiro", Position.FWD, 74, 25, "SUI"))),
        RealClubSeed("bas_sui", "FC Basel", "FCB", CountryCode.EUR_OTHER, "uefa_pool", 1, "Basel", "St. Jakob-Park", "#D71920", "#0038A8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.STAR, "#FACC15"), 18.0, 73, Formation.F4231, listOf(p("Marwin Hitz", Position.GK, 73, 37, "SUI"), p("Xherdan Shaqiri", Position.MID, 76, 33, "SUI"))),
        RealClubSeed("lug", "FC Lugano", "LUG", CountryCode.EUR_OTHER, "uefa_pool", 1, "Lugano", "Stadio Cornaredo", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 14.0, 72, Formation.F4231, listOf(p("Amir Saipi", Position.GK, 72, 24, "SUI"), p("Renato Steffen", Position.FWD, 74, 33, "SUI"))),
        RealClubSeed("fck_den", "FC Copenhagen", "FCK", CountryCode.EUR_OTHER, "uefa_pool", 1, "Copenhagen", "Parken Stadium", "#FFFFFF", "#004B93", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#004B93"), 24.0, 75, Formation.F433, listOf(p("Kevin Diks", Position.DEF, 75, 28, "IDN"), p("Mohamed Elyounoussi", Position.FWD, 76, 30, "NOR"))),
        RealClubSeed("fcm", "Midtjylland", "FCM", CountryCode.EUR_OTHER, "uefa_pool", 1, "Herning", "MCH Arena", "#111111", "#D71920", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#D71920"), 18.0, 74, Formation.F442, listOf(p("Elias Olafsson", Position.GK, 72, 24, "ISL"), p("Adam Buksa", Position.FWD, 74, 28, "POL"))),
        RealClubSeed("bod", "Bodo/Glimt", "BOD", CountryCode.EUR_OTHER, "uefa_pool", 1, "Bodo", "Aspmyra Stadion", "#FFE600", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.BOLT, "#111111"), 18.0, 75, Formation.F433, listOf(p("Nikita Haikin", Position.GK, 73, 29, "RUS"), p("Patrick Berg", Position.MID, 76, 27, "NOR"), p("Jens Petter Hauge", Position.FWD, 74, 25, "NOR"))),
        RealClubSeed("mol", "Molde", "MOL", CountryCode.EUR_OTHER, "uefa_pool", 1, "Molde", "Aker Stadion", "#0055A5", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 12.0, 72, Formation.F352, listOf(p("Magnus Wolff Eikrem", Position.MID, 73, 34, "NOR"), p("Fredrik Gulbrandsen", Position.FWD, 72, 32, "NOR"))),
        RealClubSeed("mff", "Malmo FF", "MFF", CountryCode.EUR_OTHER, "uefa_pool", 1, "Malmo", "Eleda Stadion", "#6CABDD", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 15.0, 73, Formation.F442, listOf(p("Johan Dahlin", Position.GK, 72, 38, "SWE"), p("Isaac Kiese Thelin", Position.FWD, 73, 32, "SWE"))),
        RealClubSeed("dju", "Djurgarden", "DIF", CountryCode.EUR_OTHER, "uefa_pool", 1, "Stockholm", "3Arena", "#005CA9", "#6CABDD", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FACC15"), 12.0, 71, Formation.F4231, listOf(p("Jacob Rinne", Position.GK, 72, 31, "SWE"), p("Tokmac Nguen", Position.FWD, 72, 31, "NOR"))),
        RealClubSeed("leg", "Legia Warsaw", "LEG", CountryCode.EUR_OTHER, "uefa_pool", 1, "Warsaw", "Polish Army Stadium", "#007A33", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.CROWN, "#FFFFFF"), 14.0, 72, Formation.F433, listOf(p("Kacper Tobiasz", Position.GK, 71, 22, "POL"), p("Luquinhas", Position.MID, 72, 28, "BRA"))),
        RealClubSeed("jag", "Jagiellonia", "JAG", CountryCode.EUR_OTHER, "uefa_pool", 1, "Bialystok", "Chorten Arena", "#FACC15", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.CROWN, "#111111"), 11.0, 71, Formation.F4231, listOf(p("Jesus Imaz", Position.MID, 73, 34, "ESP"), p("Afimico Pululu", Position.FWD, 73, 25, "COD"))),
        RealClubSeed("ftc", "Ferencvaros", "FTC", CountryCode.EUR_OTHER, "uefa_pool", 1, "Budapest", "Groupama Arena", "#00853E", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.EAGLE, "#FACC15"), 18.0, 73, Formation.F4231, listOf(p("Denes Dibusz", Position.GK, 75, 34, "HUN"), p("Barnabas Varga", Position.FWD, 75, 30, "HUN"))),
        RealClubSeed("qar", "Qarabag", "QAR", CountryCode.EUR_OTHER, "uefa_pool", 1, "Baku", "Tofiq Bahramov", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FACC15"), 16.0, 73, Formation.F4231, listOf(p("Juninho", Position.FWD, 74, 28, "BRA"), p("Yassine Benzia", Position.MID, 73, 30, "ALG"))),
        RealClubSeed("lud", "Ludogorets", "LUD", CountryCode.EUR_OTHER, "uefa_pool", 1, "Razgrad", "Huvepharma Arena", "#00853E", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 14.0, 72, Formation.F4231, listOf(p("Sergio Padt", Position.GK, 72, 34, "NED"), p("Kwadwo Duah", Position.FWD, 73, 27, "SUI"))),
        RealClubSeed("slo_b", "Slovan Bratislava", "SLO", CountryCode.EUR_OTHER, "uefa_pool", 1, "Bratislava", "Tehelne pole", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 14.0, 72, Formation.F4231, listOf(p("Milan Borjan", Position.GK, 73, 37, "CAN"), p("Tigran Barseghyan", Position.FWD, 74, 31, "ARM"))),
        RealClubSeed("fcsb", "FCSB", "FCSB", CountryCode.EUR_OTHER, "uefa_pool", 1, "Bucharest", "Arena Nationala", "#0033A0", "#D71920", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FACC15"), 14.0, 72, Formation.F4231, listOf(p("Stefan Tarnovanu", Position.GK, 73, 24, "ROU"), p("Darius Olaru", Position.MID, 75, 26, "ROU"))),
        RealClubSeed("mta", "Maccabi Tel Aviv", "MTA", CountryCode.EUR_OTHER, "uefa_pool", 1, "Tel Aviv", "Bloomfield Stadium", "#FACC15", "#0038A8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#0038A8"), 14.0, 72, Formation.F433, listOf(p("Dor Peretz", Position.MID, 73, 29, "ISR"), p("Eran Zahavi", Position.FWD, 74, 37, "ISR"))),
        RealClubSeed("apo", "APOEL Nicosia", "APO", CountryCode.EUR_OTHER, "uefa_pool", 1, "Nicosia", "GSP Stadium", "#0038A8", "#FFCC00", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFCC00"), 10.0, 70, Formation.F4231, listOf(p("Vid Belec", Position.GK, 72, 34, "SVN"), p("Youssef El-Arabi", Position.FWD, 73, 37, "MAR"))),
        RealClubSeed("oli", "Olimpija Ljubljana", "OLI", CountryCode.EUR_OTHER, "uefa_pool", 1, "Ljubljana", "Stozice Stadium", "#00853E", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 8.0, 69, Formation.F4231, listOf(p("Matevz Vidovsek", Position.GK, 71, 25, "SVN"), p("Raul Florucz", Position.FWD, 71, 23, "AUT"))),
        RealClubSeed("sha_r", "Shamrock Rovers", "SHM", CountryCode.EUR_OTHER, "uefa_pool", 1, "Dublin", "Tallaght Stadium", "#00853E", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.STAR, "#FFFFFF"), 4.0, 66, Formation.F352, listOf(p("Jack Byrne", Position.MID, 69, 28, "IRL"), p("Johnny Kenny", Position.FWD, 68, 21, "IRL"))),
        RealClubSeed("vik", "Vikingur Reykjavik", "VIK", CountryCode.EUR_OTHER, "uefa_pool", 1, "Reykjavik", "Vikingsvollur", "#D71920", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 3.0, 65, Formation.F433, listOf(p("Ingvar Jonsson", Position.GK, 66, 35, "ISL"), p("Ari Sigurpalsson", Position.FWD, 67, 21, "ISL"))),
        RealClubSeed("celje", "NK Celje", "CEL", CountryCode.EUR_OTHER, "uefa_pool", 1, "Celje", "Stadion Z'dezele", "#38BDF8", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 6.0, 68, Formation.F433, listOf(p("Zan Karnicnik", Position.DEF, 71, 30, "SVN"), p("Armandas Kucys", Position.FWD, 69, 21, "LTU"))),
        RealClubSeed("tns", "The New Saints", "TNS", CountryCode.EUR_OTHER, "uefa_pool", 1, "Oswestry", "Park Hall", "#00853E", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.STAR, "#FFFFFF"), 2.5, 64, Formation.F433, listOf(p("Connor Roberts", Position.GK, 65, 31, "WAL"), p("Declan McManus", Position.FWD, 66, 30, "SCO"))),
    )
}
