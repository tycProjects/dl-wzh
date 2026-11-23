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
 * Modular 3-Tier Pyramids for Spain, Italy, Germany, and France
 * with 100% real-world clubs, real players, FC-style ratings, and custom SVG crest specs.
 */
object EuropeMajorClubsData {

    val divisions: List<DivisionInfo> = listOf(
        // Spain
        DivisionInfo("esp_t1", "LaLiga EA Sports", CountryCode.ESP, 1, 0, 0, 2, uclSpots = 4, uelSpots = 1, ueclSpots = 1),
        DivisionInfo("esp_t2", "LaLiga Hypermotion", CountryCode.ESP, 2, 2, 2, 2),
        DivisionInfo("esp_t3", "Primera Federacion", CountryCode.ESP, 3, 2, 2, 0),
        // Italy
        DivisionInfo("ita_t1", "Serie A Enilive", CountryCode.ITA, 1, 0, 0, 2, uclSpots = 4, uelSpots = 1, ueclSpots = 1),
        DivisionInfo("ita_t2", "Serie BKT", CountryCode.ITA, 2, 2, 2, 2),
        DivisionInfo("ita_t3", "Serie C", CountryCode.ITA, 3, 2, 2, 0),
        // Germany
        DivisionInfo("ger_t1", "Bundesliga", CountryCode.GER, 1, 0, 0, 2, uclSpots = 4, uelSpots = 1, ueclSpots = 1),
        DivisionInfo("ger_t2", "2. Bundesliga", CountryCode.GER, 2, 2, 2, 2),
        DivisionInfo("ger_t3", "3. Liga", CountryCode.GER, 3, 2, 2, 0),
        // France
        DivisionInfo("fra_t1", "Ligue 1 McDonald's", CountryCode.FRA, 1, 0, 0, 2, uclSpots = 3, uelSpots = 1, ueclSpots = 1),
        DivisionInfo("fra_t2", "Ligue 2 BKT", CountryCode.FRA, 2, 2, 2, 2),
        DivisionInfo("fra_t3", "Championnat National", CountryCode.FRA, 3, 2, 2, 0),
    )

    private fun p(name: String, pos: Position, ovr: Int, age: Int, nat: String) =
        RealPlayerSeed(name, pos, ovr, age, nat)

    private val spainPool = listOf(
        p("Fernando Pacheco", Position.GK, 73, 32, "ESP"), p("Andres Fernandez", Position.GK, 72, 37, "ESP"),
        p("Dani Cardenas", Position.GK, 71, 27, "ESP"), p("Jokin Ezkieta", Position.GK, 69, 28, "ESP"),
        p("Edgar Badia", Position.GK, 72, 32, "ESP"), p("Ander Cantero", Position.GK, 68, 29, "ESP"),
        p("Sergio Postigo", Position.DEF, 70, 36, "ESP"), p("Jorge Cuenca", Position.DEF, 74, 25, "ESP"),
        p("Carlos Neva", Position.DEF, 72, 28, "ESP"), p("Miguel Rubio", Position.DEF, 70, 26, "ESP"),
        p("Unai Elgezabal", Position.DEF, 69, 31, "ESP"), p("Ruben Duarte", Position.DEF, 74, 29, "ESP"),
        p("Dani Calvo", Position.DEF, 71, 30, "ESP"), p("Oier Luengo", Position.DEF, 69, 27, "ESP"),
        p("Manu Hernando", Position.DEF, 69, 26, "ESP"), p("Javi Hernandez", Position.DEF, 72, 26, "ESP"),
        p("Oriol Rey", Position.MID, 71, 26, "ESP"), p("Pablo Martinez", Position.MID, 72, 26, "ESP"),
        p("Iñigo Vicente", Position.MID, 73, 26, "ESP"), p("Aritz Aldasoro", Position.MID, 70, 25, "ESP"),
        p("Santi Colombatto", Position.MID, 73, 27, "ARG"), p("Jaime Seoane", Position.MID, 72, 27, "ESP"),
        p("Martin Hongla", Position.MID, 73, 26, "CMR"), p("Gonzalo Villar", Position.MID, 73, 26, "ESP"),
        p("Dion Lopy", Position.MID, 73, 22, "SEN"), p("Lucas Robertone", Position.MID, 75, 27, "ARG"),
        p("Alemao", Position.FWD, 72, 26, "BRA"), p("Myrto Uzuni", Position.FWD, 75, 29, "ALB"),
        p("Luis Suarez", Position.FWD, 75, 26, "COL"), p("Leo Baptistao", Position.FWD, 73, 32, "BRA"),
        p("Roger Bruge", Position.FWD, 71, 28, "ESP"), p("Andres Martin", Position.FWD, 71, 25, "ESP"),
        p("Borja Baston", Position.FWD, 71, 32, "ESP"), p("Juanto Ortuno", Position.FWD, 65, 32, "ESP"),
    )

    private val italyPool = listOf(
        p("Stefano Turati", Position.GK, 73, 23, "ITA"), p("Nicolas", Position.GK, 71, 36, "BRA"),
        p("Sebastiano Desplanches", Position.GK, 69, 21, "ITA"), p("Andrea Fulignati", Position.GK, 70, 30, "ITA"),
        p("Samuele Perisan", Position.GK, 69, 27, "ITA"), p("Simone Scuffet", Position.GK, 73, 28, "ITA"),
        p("Matteo Bianchetti", Position.DEF, 71, 31, "ITA"), p("Luca Ravanelli", Position.DEF, 71, 27, "ITA"),
        p("Arturo Calabresi", Position.DEF, 70, 28, "ITA"), p("Antonio Caracciolo", Position.DEF, 70, 34, "ITA"),
        p("Pietro Ceccaroni", Position.DEF, 70, 28, "ITA"), p("Ionut Nedelcearu", Position.DEF, 70, 28, "ROU"),
        p("Petko Hristov", Position.DEF, 71, 25, "BUL"), p("Przemyslaw Wisniewski", Position.DEF, 71, 26, "POL"),
        p("Filippo Romagna", Position.DEF, 71, 27, "ITA"), p("Josh Doig", Position.DEF, 72, 22, "SCO"),
        p("Michele Castagnetti", Position.MID, 71, 35, "ITA"), p("Jari Vandeputte", Position.MID, 73, 28, "BEL"),
        p("Marius Marin", Position.MID, 72, 26, "ROU"), p("Matteo Tramoni", Position.MID, 73, 24, "FRA"),
        p("Salvatore Esposito", Position.MID, 73, 24, "ITA"), p("Filippo Ranocchia", Position.MID, 72, 23, "ITA"),
        p("Jacopo Segre", Position.MID, 71, 27, "ITA"), p("Daniel Boloca", Position.MID, 73, 25, "ROU"),
        p("Kristian Thorstvedt", Position.MID, 74, 25, "NOR"), p("Leonardo Sernicola", Position.MID, 71, 27, "ITA"),
        p("Matteo Brunori", Position.FWD, 73, 30, "ITA"), p("Joel Pohjanpalo", Position.FWD, 74, 30, "FIN"),
        p("Massimo Coda", Position.FWD, 72, 35, "ITA"), p("Gennaro Tutino", Position.FWD, 73, 28, "ITA"),
        p("Francesco Pio Esposito", Position.FWD, 72, 19, "ITA"), p("Nicholas Bonfanti", Position.FWD, 70, 22, "ITA"),
        p("Roberto Inglese", Position.FWD, 67, 33, "ITA"), p("Franco Ferrari", Position.FWD, 65, 29, "ARG"),
    )

    private val germanyPool = listOf(
        p("Florian Kastenmeier", Position.GK, 73, 27, "GER"), p("Ron-Robert Zieler", Position.GK, 73, 35, "GER"),
        p("Daniel Heuer Fernandes", Position.GK, 74, 32, "POR"), p("Marvin Schwabe", Position.GK, 75, 29, "GER"),
        p("Justin Heekeren", Position.GK, 69, 24, "GER"), p("Tim Schreiber", Position.GK, 68, 22, "GER"),
        p("Timo Hubers", Position.DEF, 74, 28, "GER"), p("Dominique Heintz", Position.DEF, 71, 31, "GER"),
        p("Sebastian Schonlau", Position.DEF, 73, 30, "GER"), p("Miro Muheim", Position.DEF, 73, 26, "SUI"),
        p("Andre Hoffmann", Position.DEF, 71, 31, "GER"), p("Tim Oberdorf", Position.DEF, 71, 28, "GER"),
        p("Marcel Halstenberg", Position.DEF, 74, 33, "GER"), p("Phil Neumann", Position.DEF, 72, 27, "GER"),
        p("Marcin Kaminski", Position.DEF, 70, 32, "POL"), p("Boris Tomiak", Position.DEF, 71, 26, "GER"),
        p("Eric Martel", Position.MID, 75, 22, "GER"), p("Dejan Ljubicic", Position.MID, 74, 27, "AUT"),
        p("Jonas Meffert", Position.MID, 73, 30, "GER"), p("Ludovit Reis", Position.MID, 74, 24, "NED"),
        p("Ao Tanaka", Position.MID, 75, 26, "JPN"), p("Isak Johannesson", Position.MID, 73, 21, "ISL"),
        p("Enzo Leopold", Position.MID, 72, 24, "GER"), p("Marlon Ritter", Position.MID, 72, 30, "GER"),
        p("Paul Seguin", Position.MID, 71, 29, "GER"), p("Niklas Hauptmann", Position.MID, 69, 28, "GER"),
        p("Ragnar Ache", Position.FWD, 73, 26, "GER"), p("Davie Selke", Position.FWD, 73, 29, "GER"),
        p("Robert Glatzel", Position.FWD, 75, 30, "GER"), p("Kenan Karaman", Position.FWD, 73, 30, "TUR"),
        p("Moussa Sylla", Position.FWD, 73, 25, "MLI"), p("Dawid Kownacki", Position.FWD, 72, 27, "POL"),
        p("Christoph Daferner", Position.FWD, 68, 26, "GER"), p("Thore Jacobsen", Position.MID, 66, 27, "GER"),
    )

    private val francePool = listOf(
        p("Yvon Mvogo", Position.GK, 74, 30, "SUI"), p("Obed Nkambadio", Position.GK, 70, 21, "FRA"),
        p("Alexandre Oukidja", Position.GK, 72, 36, "ALG"), p("Enzo Basilio", Position.GK, 69, 30, "FRA"),
        p("Anthony Mandrea", Position.GK, 71, 27, "ALG"), p("Ewen Jaouen", Position.GK, 67, 18, "FRA"),
        p("Montassar Talbi", Position.DEF, 74, 26, "TUN"), p("Laurent Abergel", Position.MID, 74, 31, "FRA"),
        p("Moustapha Mbow", Position.DEF, 71, 24, "SEN"), p("Timothee Kolodziejczak", Position.DEF, 71, 33, "FRA"),
        p("Sadibou Sane", Position.DEF, 71, 20, "SEN"), p("Matthieu Udol", Position.DEF, 73, 28, "FRA"),
        p("Opa Sangante", Position.DEF, 70, 33, "GNB"), p("Romain Thomas", Position.DEF, 70, 36, "FRA"),
        p("Vincent Sasso", Position.DEF, 69, 33, "FRA"), p("Alexandre Coeff", Position.DEF, 68, 32, "FRA"),
        p("Maxime Lopez", Position.MID, 74, 26, "FRA"), p("Vincent Marchetti", Position.MID, 71, 27, "FRA"),
        p("Gauthier Hein", Position.MID, 73, 28, "FRA"), p("Benjamin Stambouli", Position.MID, 72, 34, "FRA"),
        p("Dylan Louiserre", Position.MID, 70, 29, "FRA"), p("Kalidou Sidibe", Position.MID, 70, 25, "FRA"),
        p("Julien Ponceau", Position.MID, 72, 24, "FRA"), p("Bilal Brahimi", Position.FWD, 72, 24, "ALG"),
        p("Eli Junior Kroupi", Position.FWD, 73, 18, "FRA"), p("Jean-Philippe Krasso", Position.FWD, 73, 27, "CIV"),
        p("Cheikh Sabaly", Position.FWD, 72, 25, "SEN"), p("Idrissa Gueye", Position.FWD, 69, 18, "SEN"),
        p("Alexandre Mendy", Position.FWD, 73, 30, "GNB"), p("Gaetan Courtet", Position.FWD, 68, 35, "FRA"),
        p("Sambou Soumano", Position.FWD, 71, 23, "SEN"), p("Arnold Vula", Position.FWD, 65, 25, "FRA"),
    )

    fun createClubs(): List<Club> {
        val spainClubs = createSpainSeeds().map { RealPlayerRosterBuilder.buildClub(it, spainPool) }
        val italyClubs = createItalySeeds().map { RealPlayerRosterBuilder.buildClub(it, italyPool) }
        val germanyClubs = createGermanySeeds().map { RealPlayerRosterBuilder.buildClub(it, germanyPool) }
        val franceClubs = createFranceSeeds().map { RealPlayerRosterBuilder.buildClub(it, francePool) }
        return spainClubs + italyClubs + germanyClubs + franceClubs
    }

    private fun createSpainSeeds(): List<RealClubSeed> = listOf(
        // Tier 1: LaLiga EA Sports
        RealClubSeed(
            "rma", "Real Madrid", "RMA", CountryCode.ESP, "esp_t1", 1, "Madrid", "Santiago Bernabeu",
            "#FFFFFF", "#FEBE10", defaultSpec(CrestShape.SPANISH_CROWN, CrestPattern.DIAGONAL_SASH, CrestSymbol.CROWN, "#00529F"),
            210.0, 89, Formation.F433,
            listOf(
                p("Thibaut Courtois", Position.GK, 89, 32, "BEL"), p("Andriy Lunin", Position.GK, 81, 25, "UKR"),
                p("Ferland Mendy", Position.DEF, 82, 29, "FRA"), p("Antonio Rudiger", Position.DEF, 88, 31, "GER"),
                p("Eder Militao", Position.DEF, 85, 26, "BRA"), p("Dani Carvajal", Position.DEF, 86, 32, "ESP"),
                p("Raul Asencio", Position.DEF, 78, 21, "ESP"),
                p("Federico Valverde", Position.MID, 89, 26, "URU"), p("Jude Bellingham", Position.MID, 90, 21, "ENG"),
                p("Aurelien Tchouameni", Position.MID, 85, 24, "FRA"), p("Eduardo Camavinga", Position.MID, 84, 22, "FRA"),
                p("Luka Modric", Position.MID, 84, 39, "CRO"),
                p("Vinicius Junior", Position.FWD, 90, 24, "BRA"), p("Kylian Mbappe", Position.FWD, 91, 25, "FRA"),
                p("Rodrygo", Position.FWD, 86, 23, "BRA"), p("Endrick", Position.FWD, 79, 18, "BRA"),
            ),
        ),
        RealClubSeed(
            "fcb", "FC Barcelona", "BAR", CountryCode.ESP, "esp_t1", 1, "Barcelona", "Spotify Camp Nou",
            "#A50044", "#004D98", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.CROSS, CrestSymbol.STAR, "#EDBB00"),
            140.0, 87, Formation.F4231,
            listOf(
                p("Marc-Andre ter Stegen", Position.GK, 87, 32, "GER"), p("Inaki Pena", Position.GK, 78, 25, "ESP"),
                p("Alejandro Balde", Position.DEF, 82, 21, "ESP"), p("Pau Cubarsi", Position.DEF, 82, 17, "ESP"),
                p("Ronald Araujo", Position.DEF, 85, 25, "URU"), p("Jules Kounde", Position.DEF, 85, 26, "FRA"),
                p("Inigo Martinez", Position.DEF, 81, 33, "ESP"),
                p("Pedri", Position.MID, 88, 22, "ESP"), p("Gavi", Position.MID, 84, 20, "ESP"),
                p("Dani Olmo", Position.MID, 85, 26, "ESP"), p("Frenkie de Jong", Position.MID, 85, 27, "NED"),
                p("Marc Casado", Position.MID, 79, 21, "ESP"),
                p("Raphinha", Position.FWD, 87, 27, "BRA"), p("Robert Lewandowski", Position.FWD, 88, 36, "POL"),
                p("Lamine Yamal", Position.FWD, 87, 17, "ESP"), p("Ferran Torres", Position.FWD, 81, 24, "ESP"),
            ),
        ),
        RealClubSeed(
            "atm", "Atletico Madrid", "ATM", CountryCode.ESP, "esp_t1", 1, "Madrid", "Riyadh Air Metropolitano",
            "#CB3524", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#1E3A8A"),
            95.0, 85, Formation.F442,
            listOf(
                p("Jan Oblak", Position.GK, 87, 31, "SVN"), p("Juan Musso", Position.GK, 78, 30, "ARG"),
                p("Javi Galan", Position.DEF, 79, 30, "ESP"), p("Jose Maria Gimenez", Position.DEF, 83, 29, "URU"),
                p("Robin Le Normand", Position.DEF, 83, 28, "ESP"), p("Marcos Llorente", Position.DEF, 83, 29, "ESP"),
                p("Rodrigo De Paul", Position.MID, 84, 30, "ARG"), p("Pablo Barrios", Position.MID, 80, 21, "ESP"),
                p("Conor Gallagher", Position.MID, 81, 24, "ENG"), p("Giuliano Simeone", Position.MID, 78, 21, "ARG"),
                p("Antoine Griezmann", Position.FWD, 86, 33, "FRA"), p("Julian Alvarez", Position.FWD, 86, 24, "ARG"),
                p("Alexander Sorloth", Position.FWD, 82, 28, "NOR"),
            ),
        ),
        RealClubSeed(
            "ath", "Athletic Club", "ATH", CountryCode.ESP, "esp_t1", 1, "Bilbao", "San Mames",
            "#EE2523", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#111111"),
            45.0, 81, Formation.F4231,
            listOf(
                p("Unai Simon", Position.GK, 84, 27, "ESP"), p("Yuri Berchiche", Position.DEF, 79, 34, "ESP"),
                p("Dani Vivian", Position.DEF, 82, 25, "ESP"), p("Aitor Paredes", Position.DEF, 79, 24, "ESP"),
                p("Oscar de Marcos", Position.DEF, 78, 35, "ESP"), p("Mikel Jauregizar", Position.MID, 77, 21, "ESP"),
                p("Inigo Ruiz de Galarreta", Position.MID, 80, 31, "ESP"), p("Oihan Sancet", Position.MID, 83, 24, "ESP"),
                p("Nico Williams", Position.FWD, 85, 22, "ESP"), p("Inaki Williams", Position.FWD, 82, 30, "GHA"),
                p("Gorka Guruzeta", Position.FWD, 79, 28, "ESP"),
            ),
        ),
        RealClubSeed(
            "vil", "Villarreal", "VIL", CountryCode.ESP, "esp_t1", 1, "Villarreal", "Estadio de la Ceramica",
            "#FFE667", "#005187", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.CROWN, "#005187"),
            42.0, 80, Formation.F442,
            listOf(
                p("Diego Conde", Position.GK, 77, 26, "ESP"), p("Sergi Cardona", Position.DEF, 78, 25, "ESP"),
                p("Logan Costa", Position.DEF, 78, 23, "CPV"), p("Raul Albiol", Position.DEF, 78, 39, "ESP"),
                p("Dani Parejo", Position.MID, 81, 35, "ESP"), p("Santi Comesana", Position.MID, 78, 28, "ESP"),
                p("Alex Baena", Position.MID, 83, 23, "ESP"), p("Yeremy Pino", Position.FWD, 79, 22, "ESP"),
                p("Ayoze Perez", Position.FWD, 81, 31, "ESP"), p("Thierno Barry", Position.FWD, 77, 22, "FRA"),
            ),
        ),
        RealClubSeed(
            "bet", "Real Betis", "BET", CountryCode.ESP, "esp_t1", 1, "Seville", "Benito Villamarin",
            "#0BB363", "#FFFFFF", defaultSpec(CrestShape.SPANISH_CROWN, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FACC15"),
            35.0, 79, Formation.F4231,
            listOf(
                p("Rui Silva", Position.GK, 79, 30, "POR"), p("Diego Llorente", Position.DEF, 79, 31, "ESP"),
                p("Marc Bartra", Position.DEF, 78, 33, "ESP"), p("Johnny Cardoso", Position.MID, 80, 23, "USA"),
                p("Isco", Position.MID, 84, 32, "ESP"), p("Giovani Lo Celso", Position.MID, 82, 28, "ARG"),
                p("Antony", Position.FWD, 80, 24, "BRA"), p("Abde Ezzalzouli", Position.FWD, 78, 22, "MAR"),
                p("Cucho Hernandez", Position.FWD, 79, 25, "COL"),
            ),
        ),
        RealClubSeed(
            "rso", "Real Sociedad", "RSO", CountryCode.ESP, "esp_t1", 1, "San Sebastian", "Reale Arena",
            "#0067B1", "#FFFFFF", defaultSpec(CrestShape.SPANISH_CROWN, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FACC15"),
            40.0, 80, Formation.F433,
            listOf(
                p("Alex Remiro", Position.GK, 83, 29, "ESP"), p("Nayef Aguerd", Position.DEF, 80, 28, "MAR"),
                p("Igor Zubeldia", Position.DEF, 80, 27, "ESP"), p("Martin Zubimendi", Position.MID, 84, 25, "ESP"),
                p("Brais Mendez", Position.MID, 81, 27, "ESP"), p("Luka Sucic", Position.MID, 79, 22, "CRO"),
                p("Takefusa Kubo", Position.FWD, 82, 23, "JPN"), p("Mikel Oyarzabal", Position.FWD, 82, 27, "ESP"),
            ),
        ),
        RealClubSeed(
            "gir", "Girona", "GIR", CountryCode.ESP, "esp_t1", 1, "Girona", "Montilivi",
            "#CD2534", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FACC15"),
            32.0, 78, Formation.F4231,
            listOf(
                p("Paulo Gazzaniga", Position.GK, 79, 32, "ARG"), p("Miguel Gutierrez", Position.DEF, 81, 23, "ESP"),
                p("Daley Blind", Position.DEF, 79, 34, "NED"), p("Yangel Herrera", Position.MID, 80, 26, "VEN"),
                p("Ivan Martin", Position.MID, 78, 25, "ESP"), p("Viktor Tsygankov", Position.FWD, 81, 27, "UKR"),
                p("Bryan Gil", Position.FWD, 78, 23, "ESP"), p("Abel Ruiz", Position.FWD, 77, 24, "ESP"),
            ),
        ),
        // Tier 2: LaLiga Hypermotion
        RealClubSeed("lev", "Levante", "LEV", CountryCode.ESP, "esp_t2", 2, "Valencia", "Ciutat de Valencia", "#004D98", "#A50044", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.BOLT, "#FFFFFF"), 6.5, 72, Formation.F442, listOf(p("Andres Fernandez", Position.GK, 72, 37, "ESP"), p("Carlos Alvarez", Position.MID, 73, 21, "ESP"), p("Jose Luis Morales", Position.FWD, 73, 37, "ESP"))),
        RealClubSeed("elc", "Elche", "ELC", CountryCode.ESP, "esp_t2", 2, "Elche", "Martinez Valero", "#FFFFFF", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.CROWN, "#16A34A"), 6.0, 72, Formation.F4231, listOf(p("Matias Dituro", Position.GK, 72, 37, "ARG"), p("Aleix Febas", Position.MID, 72, 28, "ESP"), p("Nico Fernandez", Position.MID, 73, 24, "ARG"))),
        RealClubSeed("rac", "Racing Santander", "RAC", CountryCode.ESP, "esp_t2", 2, "Santander", "El Sardinero", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 5.2, 71, Formation.F4231, listOf(p("Jokin Ezkieta", Position.GK, 71, 28, "ESP"), p("Inigo Vicente", Position.MID, 74, 26, "ESP"), p("Juan Carlos Arana", Position.FWD, 72, 24, "ESP"))),
        RealClubSeed("ovi", "Real Oviedo", "OVI", CountryCode.ESP, "esp_t2", 2, "Oviedo", "Carlos Tartiere", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.SPANISH_CROWN, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 5.5, 71, Formation.F433, listOf(p("Aaron Escandell", Position.GK, 71, 29, "ESP"), p("Santi Cazorla", Position.MID, 74, 39, "ESP"), p("Alemao", Position.FWD, 72, 26, "BRA"))),
        RealClubSeed("gra", "Granada", "GRA", CountryCode.ESP, "esp_t2", 2, "Granada", "Nuevo Los Carmenes", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.CROWN, "#1E3A8A"), 7.0, 72, Formation.F442, listOf(p("Luca Zidane", Position.GK, 71, 26, "FRA"), p("Myrto Uzuni", Position.FWD, 75, 29, "ALB"), p("Lucas Boye", Position.FWD, 74, 28, "ARG"))),
        RealClubSeed("alm", "Almeria", "ALM", CountryCode.ESP, "esp_t2", 2, "Almeria", "UD Almeria Stadium", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FFFFFF"), 8.5, 73, Formation.F4231, listOf(p("Luis Maximiano", Position.GK, 74, 25, "POR"), p("Sergio Arribas", Position.MID, 75, 23, "ESP"), p("Luis Suarez", Position.FWD, 75, 26, "COL"))),
        // Tier 3: Primera Federacion
        RealClubSeed("mur", "Real Murcia", "MUR", CountryCode.ESP, "esp_t3", 3, "Murcia", "Enrique Roca", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.SPANISH_CROWN, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 1.8, 65, Formation.F433, listOf(p("Gianfranco Gazzaniga", Position.GK, 65, 30, "ARG"), p("Pedro Leon", Position.MID, 68, 37, "ESP"))),
        RealClubSeed("gim", "Gimnastic Tarragona", "GIM", CountryCode.ESP, "esp_t3", 3, "Tarragona", "Nou Estadi", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 1.5, 65, Formation.F442, listOf(p("Alberto Varo", Position.GK, 65, 31, "ESP"), p("Pablo Fernandez", Position.FWD, 66, 28, "ESP"))),
        RealClubSeed("cul", "Cultural Leonesa", "CUL", CountryCode.ESP, "esp_t3", 3, "Leon", "Reino de Leon", "#FFFFFF", "#DC2626", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#DC2626"), 1.6, 65, Formation.F4231, listOf(p("Miguel Banuz", Position.GK, 65, 31, "ESP"), p("Manu Justo", Position.FWD, 66, 28, "ESP"))),
        RealClubSeed("her", "Hercules", "HER", CountryCode.ESP, "esp_t3", 3, "Alicante", "Rico Perez", "#2563EB", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FFFFFF"), 1.4, 64, Formation.F433, listOf(p("Carlos Abad", Position.GK, 65, 29, "ESP"), p("Oriol Soldevila", Position.MID, 65, 23, "ESP"))),
        RealClubSeed("ibi", "UD Ibiza", "IBI", CountryCode.ESP, "esp_t3", 3, "Ibiza", "Can Misses", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 1.5, 64, Formation.F4231, listOf(p("Ramon Juan", Position.GK, 64, 24, "ESP"), p("Mo Dauda", Position.FWD, 66, 26, "GHA"))),
        RealClubSeed("pon", "Ponferradina", "PON", CountryCode.ESP, "esp_t3", 3, "Ponferrada", "El Toralin", "#2563EB", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.TOWER, "#FFFFFF"), 1.4, 64, Formation.F442, listOf(p("Andres Prieto", Position.GK, 65, 31, "ESP"), p("Jose Luis Cortes", Position.FWD, 65, 25, "ESP"))),
        // Additional LaLiga EA Sports (+12 -> 20)
        RealClubSeed("val", "Valencia", "VAL", CountryCode.ESP, "esp_t1", 1, "Valencia", "Mestalla", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#F97316"), 28.0, 77, Formation.F4231, listOf(p("Giorgi Mamardashvili", Position.GK, 84, 24, "GEO"), p("Jose Gaya", Position.DEF, 80, 29, "ESP"), p("Javi Guerra", Position.MID, 78, 21, "ESP"), p("Hugo Duro", Position.FWD, 78, 25, "ESP"))),
        RealClubSeed("sev", "Sevilla", "SEV", CountryCode.ESP, "esp_t1", 1, "Seville", "Ramon Sanchez-Pizjuan", "#FFFFFF", "#D71920", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#D71920"), 32.0, 78, Formation.F433, listOf(p("Orjan Nyland", Position.GK, 77, 34, "NOR"), p("Loic Bade", Position.DEF, 79, 24, "FRA"), p("Dodi Lukebakio", Position.FWD, 80, 27, "BEL"))),
        RealClubSeed("cel_esp", "Celta Vigo", "CEL", CountryCode.ESP, "esp_t1", 1, "Vigo", "Balaidos", "#7CC5E4", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#D4AF37"), 22.0, 76, Formation.F352, listOf(p("Vicente Guaita", Position.GK, 76, 37, "ESP"), p("Oscar Mingueza", Position.DEF, 78, 25, "ESP"), p("Iago Aspas", Position.FWD, 82, 37, "ESP"))),
        RealClubSeed("osa", "Osasuna", "OSA", CountryCode.ESP, "esp_t1", 1, "Pamplona", "El Sadar", "#D71920", "#1E3A8A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 20.0, 76, Formation.F433, listOf(p("Sergio Herrera", Position.GK, 78, 31, "ESP"), p("Aimar Oroz", Position.MID, 78, 22, "ESP"), p("Ante Budimir", Position.FWD, 80, 33, "CRO"))),
        RealClubSeed("get", "Getafe", "GET", CountryCode.ESP, "esp_t1", 1, "Getafe", "Coliseum", "#005999", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 18.0, 75, Formation.F442, listOf(p("David Soria", Position.GK, 79, 31, "ESP"), p("Mauro Arambarri", Position.MID, 77, 29, "URU"))),
        RealClubSeed("mal_esp", "Mallorca", "MLL", CountryCode.ESP, "esp_t1", 1, "Palma", "Son Moix", "#E30613", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 19.0, 76, Formation.F4231, listOf(p("Antonio Raillo", Position.DEF, 77, 33, "ESP"), p("Sergi Darder", Position.MID, 78, 30, "ESP"), p("Vedat Muriqi", Position.FWD, 78, 30, "KOS"))),
        RealClubSeed("ray_esp", "Rayo Vallecano", "RAY", CountryCode.ESP, "esp_t1", 1, "Madrid", "Vallecas", "#FFFFFF", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.BOLT, "#DC2626"), 17.0, 75, Formation.F4231, listOf(p("Augusto Batalla", Position.GK, 76, 28, "ARG"), p("Isi Palazon", Position.FWD, 78, 29, "ESP"))),
        RealClubSeed("ala", "Deportivo Alaves", "ALA", CountryCode.ESP, "esp_t1", 1, "Vitoria-Gasteiz", "Mendizorrotza", "#0055A5", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 15.0, 74, Formation.F4231, listOf(p("Antonio Sivera", Position.GK, 75, 28, "ESP"), p("Jon Guridi", Position.MID, 75, 29, "ESP"))),
        RealClubSeed("esp_esp", "Espanyol", "ESP", CountryCode.ESP, "esp_t1", 1, "Barcelona", "RCDE Stadium", "#007FC8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FACC15"), 16.0, 74, Formation.F442, listOf(p("Joan Garcia", Position.GK, 78, 23, "ESP"), p("Javi Puado", Position.FWD, 77, 26, "ESP"))),
        RealClubSeed("lpa", "Las Palmas", "LPA", CountryCode.ESP, "esp_t1", 1, "Las Palmas", "Estadio Gran Canaria", "#FFE600", "#00529F", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.CROWN, "#00529F"), 16.0, 74, Formation.F4231, listOf(p("Jasper Cillessen", Position.GK, 76, 35, "NED"), p("Alberto Moleiro", Position.MID, 77, 21, "ESP"))),
        RealClubSeed("leg_esp", "CD Leganes", "LEG", CountryCode.ESP, "esp_t1", 1, "Leganes", "Butarque", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LAUREL, "#FACC15"), 14.0, 73, Formation.F4231, listOf(p("Marko Dmitrovic", Position.GK, 75, 32, "SRB"), p("Juan Cruz", Position.FWD, 74, 24, "ESP"))),
        RealClubSeed("vall", "Real Valladolid", "VLL", CountryCode.ESP, "esp_t1", 1, "Valladolid", "Jose Zorrilla", "#6B21A8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FACC15"), 14.0, 73, Formation.F433, listOf(p("Karl Hein", Position.GK, 74, 22, "EST"), p("Raul Moro", Position.FWD, 75, 21, "ESP"))),
        // Additional LaLiga Hypermotion (+16 -> 22)
        RealClubSeed("esp2_mir", "Mirandes", "MIR", CountryCode.ESP, "esp_t2", 2, "Miranda", "Anduva", "#DC2626", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 4.2, 70, Formation.F352, listOf(p("Joaquin Panichelli", Position.FWD, 71, 22, "ARG"))),
        RealClubSeed("esp2_hue", "Huesca", "HUE", CountryCode.ESP, "esp_t2", 2, "Huesca", "El Alcoraz", "#003882", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROSS, "#FFFFFF"), 4.5, 70, Formation.F352),
        RealClubSeed("esp2_spo", "Sporting Gijon", "RSG", CountryCode.ESP, "esp_t2", 2, "Gijon", "El Molinon", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FACC15"), 5.5, 71, Formation.F433, listOf(p("Juan Otero", Position.FWD, 71, 29, "COL"))),
        RealClubSeed("esp2_zar", "Real Zaragoza", "ZAR", CountryCode.ESP, "esp_t2", 2, "Zaragoza", "La Romareda", "#FFFFFF", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#1D4ED8"), 6.0, 71, Formation.F442, listOf(p("Mario Soberon", Position.FWD, 71, 27, "ESP"))),
        RealClubSeed("esp2_cad", "Cadiz", "CAD", CountryCode.ESP, "esp_t2", 2, "Cadiz", "Nuevo Mirandilla", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#1D4ED8"), 6.5, 71, Formation.F442, listOf(p("Javi Ontiveros", Position.FWD, 73, 27, "ESP"))),
        RealClubSeed("esp2_mal", "Malaga", "MAL", CountryCode.ESP, "esp_t2", 2, "Malaga", "La Rosaleda", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 5.2, 70, Formation.F4231),
        RealClubSeed("esp2_dep", "Deportivo La Coruna", "DEP", CountryCode.ESP, "esp_t2", 2, "A Coruna", "Riazor", "#0055A5", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FACC15"), 5.8, 70, Formation.F4231, listOf(p("Yeremay Hernandez", Position.FWD, 74, 21, "ESP"))),
        RealClubSeed("esp2_eib", "Eibar", "EIB", CountryCode.ESP, "esp_t2", 2, "Eibar", "Ipurua", "#A50044", "#004D98", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 5.0, 70, Formation.F4231),
        RealClubSeed("esp2_cor", "Cordoba", "CCF", CountryCode.ESP, "esp_t2", 2, "Cordoba", "Nuevo Arcangel", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FFFFFF"), 4.2, 69, Formation.F433),
        RealClubSeed("esp2_alb", "Albacete", "ALB", CountryCode.ESP, "esp_t2", 2, "Albacete", "Carlos Belmonte", "#FFFFFF", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#DC2626"), 4.4, 69, Formation.F442),
        RealClubSeed("esp2_cas", "Castellon", "CAS", CountryCode.ESP, "esp_t2", 2, "Castellon", "Castalia", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.TOWER, "#FFFFFF"), 4.5, 69, Formation.F352),
        RealClubSeed("esp2_bur", "Burgos", "BUR", CountryCode.ESP, "esp_t2", 2, "Burgos", "El Plantio", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.CROWN, "#111111"), 4.2, 69, Formation.F442),
        RealClubSeed("esp2_eld", "Eldense", "ELD", CountryCode.ESP, "esp_t2", 2, "Elda", "Nuevo Pepico Amat", "#004D98", "#A50044", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 3.8, 68, Formation.F442),
        RealClubSeed("esp2_fer", "Racing Ferrol", "RFE", CountryCode.ESP, "esp_t2", 2, "Ferrol", "A Malata", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 3.6, 68, Formation.F4231),
        RealClubSeed("esp2_ten", "CD Tenerife", "TEN", CountryCode.ESP, "esp_t2", 2, "Santa Cruz", "Heliodoro Rodriguez", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 4.5, 69, Formation.F4231),
        RealClubSeed("esp2_car", "FC Cartagena", "CAR", CountryCode.ESP, "esp_t2", 2, "Cartagena", "Cartagonova", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.ANCHOR, "#FFFFFF"), 3.5, 68, Formation.F4231),
        // Additional Primera Federacion (+14 -> 20)
        RealClubSeed("esp3_and", "FC Andorra", "AND", CountryCode.ESP, "esp_t3", 3, "Andorra", "Estadi Nacional", "#1D4ED8", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#DC2626"), 1.9, 65, Formation.F433),
        RealClubSeed("esp3_ceu", "AD Ceuta", "CEU", CountryCode.ESP, "esp_t3", 3, "Ceuta", "Alfonso Murube", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#111111"), 1.5, 64, Formation.F433),
        RealClubSeed("esp3_ant", "Antequera", "ANT", CountryCode.ESP, "esp_t3", 3, "Antequera", "El Mauli", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 1.4, 64, Formation.F4231),
        RealClubSeed("esp3_mer", "Merida AD", "MER", CountryCode.ESP, "esp_t3", 3, "Merida", "Romano Jose Fouto", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#111111"), 1.3, 64, Formation.F4231),
        RealClubSeed("esp3_alc", "Alcorcon", "ALC", CountryCode.ESP, "esp_t3", 3, "Alcorcon", "Santo Domingo", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#1D4ED8"), 1.5, 64, Formation.F4231),
        RealClubSeed("esp3_rec", "Recreativo Huelva", "REC", CountryCode.ESP, "esp_t3", 3, "Huelva", "Nuevo Colombino", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FACC15"), 1.4, 64, Formation.F4231),
        RealClubSeed("esp3_alge", "Algeciras", "ALG", CountryCode.ESP, "esp_t3", 3, "Algeciras", "Nuevo Mirador", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 1.2, 63, Formation.F442),
        RealClubSeed("esp3_fuen", "Fuenlabrada", "FUE", CountryCode.ESP, "esp_t3", 3, "Fuenlabrada", "Fernando Torres", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 1.3, 63, Formation.F4231),
        RealClubSeed("esp3_marb", "Marbella", "MAR", CountryCode.ESP, "esp_t3", 3, "Marbella", "Banus Football Center", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 1.4, 64, Formation.F433),
        RealClubSeed("esp3_zam", "Zamora CF", "ZAM", CountryCode.ESP, "esp_t3", 3, "Zamora", "Ruta de la Plata", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FFFFFF"), 1.2, 63, Formation.F4231),
        RealClubSeed("esp3_bar", "Barakaldo", "BAK", CountryCode.ESP, "esp_t3", 3, "Barakaldo", "Lasesarre", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#111111"), 1.3, 64, Formation.F4231),
        RealClubSeed("esp3_tar", "SD Tarazona", "TAR", CountryCode.ESP, "esp_t3", 3, "Tarazona", "Municipal de Tarazona", "#DC2626", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 1.1, 63, Formation.F442),
        RealClubSeed("esp3_lug", "CD Lugo", "LUG", CountryCode.ESP, "esp_t3", 3, "Lugo", "Anxo Carro", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 1.4, 64, Formation.F4231),
        RealClubSeed("esp3_sab", "CE Sabadell", "SAB", CountryCode.ESP, "esp_t3", 3, "Sabadell", "Nova Creu Alta", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.QUARTERS, CrestSymbol.STAR, "#FFFFFF"), 1.3, 63, Formation.F433),
    )

    private fun createItalySeeds(): List<RealClubSeed> = listOf(
        // Tier 1: Serie A
        RealClubSeed(
            "int", "Inter Milan", "INT", CountryCode.ITA, "ita_t1", 1, "Milan", "San Siro",
            "#010E80", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#D4AF37"),
            135.0, 86, Formation.F352,
            listOf(
                p("Yann Sommer", Position.GK, 86, 35, "SUI"), p("Josep Martinez", Position.GK, 78, 26, "ESP"),
                p("Alessandro Bastoni", Position.DEF, 87, 25, "ITA"), p("Francesco Acerbi", Position.DEF, 83, 36, "ITA"),
                p("Benjamin Pavard", Position.DEF, 84, 28, "FRA"), p("Federico Dimarco", Position.MID, 85, 27, "ITA"),
                p("Nicolo Barella", Position.MID, 87, 27, "ITA"), p("Hakan Calhanoglu", Position.MID, 86, 30, "TUR"),
                p("Henrikh Mkhitaryan", Position.MID, 82, 35, "ARM"), p("Denzel Dumfries", Position.MID, 83, 28, "NED"),
                p("Lautaro Martinez", Position.FWD, 89, 27, "ARG"), p("Marcus Thuram", Position.FWD, 85, 27, "FRA"),
                p("Mehdi Taremi", Position.FWD, 79, 32, "IRN"),
            ),
        ),
        RealClubSeed(
            "nap", "Napoli", "NAP", CountryCode.ITA, "ita_t1", 1, "Naples", "Stadio Diego Armando Maradona",
            "#008FD7", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"),
            95.0, 83, Formation.F433,
            listOf(
                p("Alex Meret", Position.GK, 81, 27, "ITA"), p("Mathias Olivera", Position.DEF, 80, 27, "URU"),
                p("Alessandro Buongiorno", Position.DEF, 83, 25, "ITA"), p("Amir Rrahmani", Position.DEF, 81, 30, "KOS"),
                p("Giovanni Di Lorenzo", Position.DEF, 82, 31, "ITA"), p("Stanislav Lobotka", Position.MID, 83, 29, "SVK"),
                p("Scott McTominay", Position.MID, 82, 27, "SCO"), p("Frank Anguissa", Position.MID, 82, 29, "CMR"),
                p("David Neres", Position.FWD, 81, 27, "BRA"), p("Romelu Lukaku", Position.FWD, 83, 31, "BEL"),
                p("Matteo Politano", Position.FWD, 80, 31, "ITA"),
            ),
        ),
        RealClubSeed(
            "acm", "AC Milan", "MIL", CountryCode.ITA, "ita_t1", 1, "Milan", "San Siro",
            "#FB090B", "#111111", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.CROSS, CrestSymbol.STAR, "#FFFFFF"),
            105.0, 83, Formation.F4231,
            listOf(
                p("Mike Maignan", Position.GK, 87, 29, "FRA"), p("Theo Hernandez", Position.DEF, 86, 27, "FRA"),
                p("Fikayo Tomori", Position.DEF, 82, 26, "ENG"), p("Malick Thiaw", Position.DEF, 80, 23, "GER"),
                p("Kyle Walker", Position.DEF, 81, 34, "ENG"), p("Tijjani Reijnders", Position.MID, 84, 26, "NED"),
                p("Youssouf Fofana", Position.MID, 81, 25, "FRA"), p("Christian Pulisic", Position.MID, 84, 26, "USA"),
                p("Rafael Leao", Position.FWD, 86, 25, "POR"), p("Santiago Gimenez", Position.FWD, 81, 23, "MEX"),
                p("Joao Felix", Position.FWD, 81, 25, "POR"),
            ),
        ),
        RealClubSeed(
            "juv", "Juventus", "JUV", CountryCode.ITA, "ita_t1", 1, "Turin", "Allianz Stadium",
            "#111111", "#FFFFFF", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#D4AF37"),
            100.0, 83, Formation.F4231,
            listOf(
                p("Michele Di Gregorio", Position.GK, 81, 27, "ITA"), p("Andrea Cambiaso", Position.DEF, 82, 24, "ITA"),
                p("Gleison Bremer", Position.DEF, 86, 27, "BRA"), p("Federico Gatti", Position.DEF, 80, 26, "ITA"),
                p("Pierre Kalulu", Position.DEF, 80, 24, "FRA"), p("Manuel Locatelli", Position.MID, 82, 26, "ITA"),
                p("Khephren Thuram", Position.MID, 81, 23, "FRA"), p("Teun Koopmeiners", Position.MID, 83, 26, "NED"),
                p("Kenan Yildiz", Position.FWD, 80, 19, "TUR"), p("Dusan Vlahovic", Position.FWD, 83, 24, "SRB"),
                p("Francisco Conceicao", Position.FWD, 80, 21, "POR"),
            ),
        ),
        RealClubSeed(
            "ata", "Atalanta", "ATA", CountryCode.ITA, "ita_t1", 1, "Bergamo", "Gewiss Stadium",
            "#1E71B8", "#111111", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.HALF_AND_HALF, CrestSymbol.FLAME, "#FFFFFF"),
            62.0, 82, Formation.F352,
            listOf(
                p("Marco Carnesecchi", Position.GK, 81, 24, "ITA"), p("Isak Hien", Position.DEF, 80, 25, "SWE"),
                p("Berat Djimsiti", Position.DEF, 80, 31, "ALB"), p("Sead Kolasinac", Position.DEF, 79, 31, "BIH"),
                p("Ederson", Position.MID, 83, 25, "BRA"), p("Marten de Roon", Position.MID, 81, 33, "NED"),
                p("Raoul Bellanova", Position.MID, 79, 24, "ITA"), p("Charles De Ketelaere", Position.FWD, 82, 23, "BEL"),
                p("Ademola Lookman", Position.FWD, 85, 27, "NGA"), p("Mateo Retegui", Position.FWD, 82, 25, "ITA"),
            ),
        ),
        RealClubSeed(
            "rom", "AS Roma", "ROM", CountryCode.ITA, "ita_t1", 1, "Rome", "Stadio Olimpico",
            "#8E1F2F", "#F0BC42", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#F0BC42"),
            55.0, 80, Formation.F352,
            listOf(
                p("Mile Svilar", Position.GK, 81, 25, "SRB"), p("Evan Ndicka", Position.DEF, 80, 25, "CIV"),
                p("Gianluca Mancini", Position.DEF, 81, 28, "ITA"), p("Angelino", Position.DEF, 79, 27, "ESP"),
                p("Manu Kone", Position.MID, 80, 23, "FRA"), p("Leandro Paredes", Position.MID, 80, 30, "ARG"),
                p("Lorenzo Pellegrini", Position.MID, 81, 28, "ITA"), p("Paulo Dybala", Position.FWD, 86, 31, "ARG"),
                p("Artem Dovbyk", Position.FWD, 82, 27, "UKR"),
            ),
        ),
        RealClubSeed(
            "laz", "Lazio", "LAZ", CountryCode.ITA, "ita_t1", 1, "Rome", "Stadio Olimpico",
            "#87D8F7", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#1E3A8A"),
            45.0, 80, Formation.F4231,
            listOf(
                p("Ivan Provedel", Position.GK, 81, 30, "ITA"), p("Mario Gila", Position.DEF, 80, 24, "ESP"),
                p("Alessio Romagnoli", Position.DEF, 80, 29, "ITA"), p("Nuno Tavares", Position.DEF, 79, 24, "POR"),
                p("Nicolo Rovella", Position.MID, 80, 22, "ITA"), p("Matteo Guendouzi", Position.MID, 81, 25, "FRA"),
                p("Mattia Zaccagni", Position.FWD, 82, 29, "ITA"), p("Taty Castellanos", Position.FWD, 79, 26, "ARG"),
            ),
        ),
        RealClubSeed(
            "bol_ita", "Bologna", "BFC", CountryCode.ITA, "ita_t1", 1, "Bologna", "Renato Dall'Ara",
            "#A21C26", "#1A2F48", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.HALF_AND_HALF, CrestSymbol.TOWER, "#FFFFFF"),
            36.0, 78, Formation.F4231,
            listOf(
                p("Lukasz Skorupski", Position.GK, 79, 33, "POL"), p("Sam Beukema", Position.DEF, 79, 26, "NED"),
                p("Jhon Lucumi", Position.DEF, 79, 26, "COL"), p("Remo Freuler", Position.MID, 79, 32, "SUI"),
                p("Lewis Ferguson", Position.MID, 80, 25, "SCO"), p("Riccardo Orsolini", Position.FWD, 81, 27, "ITA"),
                p("Santiago Castro", Position.FWD, 77, 20, "ARG"),
            ),
        ),
        // Tier 2: Serie BKT
        RealClubSeed("sas", "Sassuolo", "SAS", CountryCode.ITA, "ita_t2", 2, "Sassuolo", "Mapei Stadium", "#16A34A", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.FLAME, "#FFFFFF"), 9.5, 74, Formation.F433, listOf(p("Horatiu Moldovan", Position.GK, 73, 26, "ROU"), p("Domenico Berardi", Position.FWD, 79, 30, "ITA"), p("Armand Lauriente", Position.FWD, 75, 25, "FRA"))),
        RealClubSeed("pis", "Pisa", "PIS", CountryCode.ITA, "ita_t2", 2, "Pisa", "Arena Garibaldi", "#1D4ED8", "#111111", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.CROSS, CrestSymbol.TOWER, "#FFFFFF"), 5.0, 71, Formation.F352, listOf(p("Adrian Semper", Position.GK, 71, 26, "CRO"), p("Matteo Tramoni", Position.MID, 73, 24, "FRA"))),
        RealClubSeed("spe", "Spezia", "SPE", CountryCode.ITA, "ita_t2", 2, "La Spezia", "Alberto Picco", "#FFFFFF", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.EAGLE, "#111111"), 4.8, 71, Formation.F352, listOf(p("Salvatore Esposito", Position.MID, 73, 24, "ITA"), p("Francesco Pio Esposito", Position.FWD, 72, 19, "ITA"))),
        RealClubSeed("cre", "Cremonese", "CRE", CountryCode.ITA, "ita_t2", 2, "Cremona", "Giovanni Zini", "#DC2626", "#94A3B8", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.TOWER, "#FFFFFF"), 5.5, 71, Formation.F352, listOf(p("Franco Vazquez", Position.MID, 73, 35, "ARG"), p("Jari Vandeputte", Position.MID, 73, 28, "BEL"))),
        RealClubSeed("pal", "Palermo", "PAL", CountryCode.ITA, "ita_t2", 2, "Palermo", "Renzo Barbera", "#F472B6", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#111111"), 7.0, 71, Formation.F433, listOf(p("Emil Audero", Position.GK, 75, 27, "IDN"), p("Matteo Brunori", Position.FWD, 73, 30, "ITA"))),
        RealClubSeed("sam", "Sampdoria", "SAM", CountryCode.ITA, "ita_t2", 2, "Genoa", "Luigi Ferraris", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.ANCHOR, "#DC2626"), 6.0, 71, Formation.F352, listOf(p("Massimo Coda", Position.FWD, 72, 35, "ITA"), p("Gennaro Tutino", Position.FWD, 73, 28, "ITA"))),
        // Tier 3: Serie C
        RealClubSeed("pad", "Padova", "PAD", CountryCode.ITA, "ita_t3", 3, "Padua", "Stadio Euganeo", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.CROSS, CrestSymbol.STAR, "#FFFFFF"), 1.6, 65, Formation.F352, listOf(p("Mattia Fortin", Position.GK, 65, 21, "ITA"), p("Mattia Bortolussi", Position.FWD, 66, 28, "ITA"))),
        RealClubSeed("vic", "Vicenza", "VIC", CountryCode.ITA, "ita_t3", 3, "Vicenza", "Romeo Menti", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 1.7, 65, Formation.F352, listOf(p("Alessandro Confente", Position.GK, 66, 26, "ITA"), p("Franco Ferrari", Position.FWD, 66, 29, "ARG"))),
        RealClubSeed("ter", "Ternana", "TER", CountryCode.ITA, "ita_t3", 3, "Terni", "Libero Liberati", "#DC2626", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FFFFFF"), 1.4, 64, Formation.F4231, listOf(p("Gianmarco Vannucchi", Position.GK, 65, 29, "ITA"), p("Pietro Cianci", Position.FWD, 65, 28, "ITA"))),
        RealClubSeed("pes", "Pescara", "PES", CountryCode.ITA, "ita_t3", 3, "Pescara", "Stadio Adriatico", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.ANCHOR, "#FFFFFF"), 1.4, 64, Formation.F433, listOf(p("Alessandro Plizzari", Position.GK, 66, 24, "ITA"), p("Davide Merola", Position.FWD, 66, 24, "ITA"))),
        RealClubSeed("cat", "Catania", "CAT", CountryCode.ITA, "ita_t3", 3, "Catania", "Angelo Massimino", "#DC2626", "#38BDF8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.FLAME, "#FFFFFF"), 1.8, 65, Formation.F352, listOf(p("Roberto Inglese", Position.FWD, 67, 33, "ITA"), p("Stefano Sturaro", Position.MID, 66, 31, "ITA"))),
        RealClubSeed("ben", "Benevento", "BEN", CountryCode.ITA, "ita_t3", 3, "Benevento", "Ciro Vigorito", "#FACC15", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#111111"), 1.5, 64, Formation.F433, listOf(p("Filippo Berra", Position.DEF, 65, 29, "ITA"), p("Jacopo Manconi", Position.FWD, 65, 30, "ITA"))),
        // Additional Serie A (+12 -> 20)
        RealClubSeed("fio", "Fiorentina", "FIO", CountryCode.ITA, "ita_t1", 1, "Florence", "Artemio Franchi", "#5E2B97", "#FFFFFF", defaultSpec(CrestShape.DIAMOND, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"), 48.0, 79, Formation.F4231, listOf(p("David de Gea", Position.GK, 82, 34, "ESP"), p("Dodo", Position.DEF, 79, 26, "BRA"), p("Moise Kean", Position.FWD, 80, 24, "ITA"))),
        RealClubSeed("tor_ita", "Torino", "TOR", CountryCode.ITA, "ita_t1", 1, "Turin", "Olimpico Grande Torino", "#7B102E", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 24.0, 76, Formation.F352, listOf(p("Vanja Milinkovic-Savic", Position.GK, 78, 27, "SRB"), p("Samuele Ricci", Position.MID, 79, 23, "ITA"), p("Che Adams", Position.FWD, 76, 28, "SCO"))),
        RealClubSeed("udi", "Udinese", "UDI", CountryCode.ITA, "ita_t1", 1, "Udine", "Bluenergy Stadium", "#111111", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#D4AF37"), 22.0, 75, Formation.F352, listOf(p("Jaka Bijol", Position.DEF, 77, 25, "SVN"), p("Florian Thauvin", Position.FWD, 78, 31, "FRA"), p("Lorenzo Lucca", Position.FWD, 76, 24, "ITA"))),
        RealClubSeed("gen", "Genoa", "GEN", CountryCode.ITA, "ita_t1", 1, "Genoa", "Luigi Ferraris", "#A50021", "#003366", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.EAGLE, "#FACC15"), 20.0, 75, Formation.F433, listOf(p("Johan Vasquez", Position.DEF, 76, 26, "MEX"), p("Morten Frendrup", Position.MID, 77, 23, "DEN"), p("Andrea Pinamonti", Position.FWD, 76, 25, "ITA"))),
        RealClubSeed("com", "Como 1907", "COM", CountryCode.ITA, "ita_t1", 1, "Como", "Giuseppe Sinigaglia", "#004691", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 32.0, 75, Formation.F4231, listOf(p("Nico Paz", Position.MID, 78, 20, "ARG"), p("Assane Diao", Position.FWD, 76, 19, "ESP"))),
        RealClubSeed("cag", "Cagliari", "CAG", CountryCode.ITA, "ita_t1", 1, "Cagliari", "Unipol Domus", "#9E1B32", "#002350", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.CROWN, "#FFFFFF"), 14.0, 74, Formation.F4231, listOf(p("Yerry Mina", Position.DEF, 76, 30, "COL"), p("Roberto Piccoli", Position.FWD, 74, 23, "ITA"))),
        RealClubSeed("par_ita", "Parma", "PAR", CountryCode.ITA, "ita_t1", 1, "Parma", "Ennio Tardini", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.CROSS, CrestSymbol.CROWN, "#FACC15"), 18.0, 74, Formation.F4231, listOf(p("Zion Suzuki", Position.GK, 75, 22, "JPN"), p("Dennis Man", Position.FWD, 77, 26, "ROU"), p("Ange-Yoan Bonny", Position.FWD, 74, 21, "FRA"))),
        RealClubSeed("ver", "Hellas Verona", "VER", CountryCode.ITA, "ita_t1", 1, "Verona", "Marcantonio Bentegodi", "#00338D", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FACC15"), 14.0, 74, Formation.F4231, listOf(p("Lorenzo Montipo", Position.GK, 75, 28, "ITA"), p("Casper Tengstedt", Position.FWD, 74, 24, "DEN"))),
        RealClubSeed("emp", "Empoli", "EMP", CountryCode.ITA, "ita_t1", 1, "Empoli", "Carlo Castellani", "#0055A5", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 13.0, 73, Formation.F352, listOf(p("Ardian Ismajli", Position.DEF, 75, 28, "ALB"), p("Sebastiano Esposito", Position.FWD, 75, 22, "ITA"))),
        RealClubSeed("lec", "Lecce", "LEC", CountryCode.ITA, "ita_t1", 1, "Lecce", "Via del Mare", "#FACC15", "#D71920", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FFFFFF"), 12.0, 73, Formation.F433, listOf(p("Wladimiro Falcone", Position.GK, 76, 29, "ITA"), p("Nikola Krstovic", Position.FWD, 75, 24, "MNE"))),
        RealClubSeed("ven", "Venezia", "VEN", CountryCode.ITA, "ita_t1", 1, "Venezia", "Pier Luigi Penzo", "#F97316", "#16A34A", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.LION, "#FACC15"), 12.0, 73, Formation.F352, listOf(p("Filip Stankovic", Position.GK, 74, 22, "SRB"), p("Hans Nicolussi Caviglia", Position.MID, 74, 24, "ITA"))),
        RealClubSeed("mon_ita", "Monza", "MON", CountryCode.ITA, "ita_t1", 1, "Monza", "U-Power Stadium", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 15.0, 73, Formation.F352, listOf(p("Matteo Pessina", Position.MID, 76, 27, "ITA"), p("Dany Mota", Position.FWD, 74, 26, "POR"))),
        // Additional Serie BKT (+14 -> 20)
        RealClubSeed("ita2_juvs", "Juve Stabia", "JST", CountryCode.ITA, "ita_t2", 2, "Castellammare", "Romeo Menti", "#FACC15", "#1E3A8A", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 3.8, 69, Formation.F352),
        RealClubSeed("ita2_cat", "Catanzaro", "CTZ", CountryCode.ITA, "ita_t2", 2, "Catanzaro", "Nicola Ceravolo", "#DC2626", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.EAGLE, "#FFFFFF"), 4.2, 70, Formation.F352, listOf(p("Pietro Iemmello", Position.FWD, 71, 32, "ITA"))),
        RealClubSeed("ita2_bar", "Bari", "BAR", CountryCode.ITA, "ita_t2", 2, "Bari", "San Nicola", "#FFFFFF", "#DC2626", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.SOLID, CrestSymbol.STAR, "#DC2626"), 5.0, 70, Formation.F352),
        RealClubSeed("ita2_ces", "Cesena", "CES", CountryCode.ITA, "ita_t2", 2, "Cesena", "Dino Manuzzi", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.EAGLE, "#111111"), 4.5, 70, Formation.F352, listOf(p("Cristian Shpendi", Position.FWD, 71, 21, "ALB"))),
        RealClubSeed("ita2_mod", "Modena", "MOD", CountryCode.ITA, "ita_t2", 2, "Modena", "Alberto Braglia", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#1D4ED8"), 4.4, 69, Formation.F4231),
        RealClubSeed("ita2_reg", "Reggiana", "REG", CountryCode.ITA, "ita_t2", 2, "Reggio Emilia", "Mapei Stadium", "#7F1D1D", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 4.0, 69, Formation.F4231),
        RealClubSeed("ita2_bre", "Brescia", "BRE", CountryCode.ITA, "ita_t2", 2, "Brescia", "Mario Rigamonti", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.CHEVRON, CrestSymbol.LION, "#FFFFFF"), 4.6, 69, Formation.F433),
        RealClubSeed("ita2_car", "Carrarese", "CAR", CountryCode.ITA, "ita_t2", 2, "Carrara", "Dei Marmi", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#1D4ED8"), 3.4, 68, Formation.F352),
        RealClubSeed("ita2_man", "Mantova", "MAN", CountryCode.ITA, "ita_t2", 2, "Mantua", "Danilo Martelli", "#FFFFFF", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.STAR, "#DC2626"), 3.6, 68, Formation.F4231),
        RealClubSeed("ita2_cit", "Cittadella", "CIT", CountryCode.ITA, "ita_t2", 2, "Cittadella", "Pier Cesare Tombolato", "#7F1D1D", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 3.8, 69, Formation.F352),
        RealClubSeed("ita2_fro", "Frosinone", "FRO", CountryCode.ITA, "ita_t2", 2, "Frosinone", "Benito Stirpe", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#1D4ED8"), 5.5, 70, Formation.F433),
        RealClubSeed("ita2_sal", "Salernitana", "SAL", CountryCode.ITA, "ita_t2", 2, "Salerno", "Arechi", "#7F1D1D", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 5.8, 70, Formation.F352),
        RealClubSeed("ita2_sud", "Sudtirol", "SUD", CountryCode.ITA, "ita_t2", 2, "Bolzano", "Druso", "#FFFFFF", "#DC2626", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.EAGLE, "#DC2626"), 3.8, 69, Formation.F352),
        RealClubSeed("ita2_cos", "Cosenza", "COS", CountryCode.ITA, "ita_t2", 2, "Cosenza", "San Vito-Gigi Marulla", "#DC2626", "#1E3A8A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FFFFFF"), 3.5, 68, Formation.F352),
        // Additional Serie C (+14 -> 20)
        RealClubSeed("ita3_ent", "Virtus Entella", "ENT", CountryCode.ITA, "ita_t3", 3, "Chiavari", "Comunale", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 1.6, 65, Formation.F352),
        RealClubSeed("ita3_aud", "Audace Cerignola", "CER", CountryCode.ITA, "ita_t3", 3, "Cerignola", "Domenico Monterisi", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.EAGLE, "#FFFFFF"), 1.4, 64, Formation.F352),
        RealClubSeed("ita3_ave", "Avellino", "AVE", CountryCode.ITA, "ita_t3", 3, "Avellino", "Partenio-Lombardi", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 1.8, 65, Formation.F433),
        RealClubSeed("ita3_mon", "Monopoli", "MNP", CountryCode.ITA, "ita_t3", 3, "Monopoli", "Vito Simone Veneziani", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.ANCHOR, "#FFFFFF"), 1.3, 64, Formation.F352),
        RealClubSeed("ita3_cro", "Crotone", "CRO", CountryCode.ITA, "ita_t3", 3, "Crotone", "Ezio Scida", "#DC2626", "#1E3A8A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 1.5, 64, Formation.F4231),
        RealClubSeed("ita3_fer", "Feralpisalo", "FER", CountryCode.ITA, "ita_t3", 3, "Salo", "Lino Turina", "#16A34A", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.LION, "#FFFFFF"), 1.4, 64, Formation.F352),
        RealClubSeed("ita3_tre", "Trento", "TRE", CountryCode.ITA, "ita_t3", 3, "Trento", "Briamasco", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#1D4ED8"), 1.3, 64, Formation.F433),
        RealClubSeed("ita3_tor", "Torres", "TOR", CountryCode.ITA, "ita_t3", 3, "Sassari", "Vanni Sanna", "#DC2626", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.TOWER, "#FFFFFF"), 1.4, 64, Formation.F352),
        RealClubSeed("ita3_rim", "Rimini", "RIM", CountryCode.ITA, "ita_t3", 3, "Rimini", "Romeo Neri", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HOOPS, CrestSymbol.STAR, "#FFFFFF"), 1.3, 64, Formation.F352),
        RealClubSeed("ita3_spa", "SPAL", "SPA", CountryCode.ITA, "ita_t3", 3, "Ferrara", "Paolo Mazza", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.ITALIAN_OVAL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FFFFFF"), 1.5, 64, Formation.F433),
        RealClubSeed("ita3_per", "Perugia", "PER", CountryCode.ITA, "ita_t3", 3, "Perugia", "Renato Curi", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 1.5, 64, Formation.F4231),
        RealClubSeed("ita3_are", "Arezzo", "ARE", CountryCode.ITA, "ita_t3", 3, "Arezzo", "Citta di Arezzo", "#7F1D1D", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 1.4, 64, Formation.F433),
        RealClubSeed("ita3_tri", "Triestina", "TRI", CountryCode.ITA, "ita_t3", 3, "Trieste", "Nereo Rocco", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 1.5, 64, Formation.F4231),
        RealClubSeed("ita3_nov", "Novara", "NOV", CountryCode.ITA, "ita_t3", 3, "Novara", "Silvio Piola", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 1.3, 63, Formation.F352),
    )

    private fun createGermanySeeds(): List<RealClubSeed> = listOf(
        // Tier 1: Bundesliga
        RealClubSeed(
            "bay", "Bayern Munich", "FCB", CountryCode.GER, "ger_t1", 1, "Munich", "Allianz Arena",
            "#DC052D", "#0066B2", defaultSpec(CrestShape.ROUNDEL, CrestPattern.QUARTERS, CrestSymbol.STAR, "#FFFFFF"),
            165.0, 87, Formation.F4231,
            listOf(
                p("Manuel Neuer", Position.GK, 86, 38, "GER"), p("Alphonso Davies", Position.DEF, 84, 24, "CAN"),
                p("Kim Min-jae", Position.DEF, 84, 28, "KOR"), p("Dayot Upamecano", Position.DEF, 84, 26, "FRA"),
                p("Konrad Laimer", Position.DEF, 81, 27, "AUT"), p("Joshua Kimmich", Position.MID, 88, 29, "GER"),
                p("Aleksandar Pavlovic", Position.MID, 80, 20, "GER"), p("Jamal Musiala", Position.MID, 88, 21, "GER"),
                p("Michael Olise", Position.FWD, 85, 22, "FRA"), p("Harry Kane", Position.FWD, 90, 31, "ENG"),
                p("Kingsley Coman", Position.FWD, 83, 28, "FRA"), p("Leroy Sane", Position.FWD, 83, 28, "GER"),
            ),
        ),
        RealClubSeed(
            "lev_ger", "Bayer Leverkusen", "B04", CountryCode.GER, "ger_t1", 1, "Leverkusen", "BayArena",
            "#E32221", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"),
            92.0, 85, Formation.F352,
            listOf(
                p("Lukas Hradecky", Position.GK, 83, 34, "FIN"), p("Piero Hincapie", Position.DEF, 83, 22, "ECU"),
                p("Jonathan Tah", Position.DEF, 86, 28, "GER"), p("Edmond Tapsoba", Position.DEF, 84, 25, "BFA"),
                p("Alejandro Grimaldo", Position.MID, 86, 29, "ESP"), p("Granit Xhaka", Position.MID, 86, 32, "SUI"),
                p("Exequiel Palacios", Position.MID, 84, 26, "ARG"), p("Jeremie Frimpong", Position.MID, 85, 23, "NED"),
                p("Florian Wirtz", Position.MID, 88, 21, "GER"), p("Victor Boniface", Position.FWD, 83, 23, "NGA"),
                p("Patrik Schick", Position.FWD, 83, 28, "CZE"),
            ),
        ),
        RealClubSeed(
            "bvb", "Borussia Dortmund", "BVB", CountryCode.GER, "ger_t1", 1, "Dortmund", "Signal Iduna Park",
            "#FDE100", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.BOLT, "#111111"),
            85.0, 82, Formation.F4231,
            listOf(
                p("Gregor Kobel", Position.GK, 87, 26, "SUI"), p("Nico Schlotterbeck", Position.DEF, 84, 24, "GER"),
                p("Waldemar Anton", Position.DEF, 81, 28, "GER"), p("Julian Ryerson", Position.DEF, 79, 27, "NOR"),
                p("Felix Nmecha", Position.MID, 79, 24, "GER"), p("Pascal Gross", Position.MID, 81, 33, "GER"),
                p("Julian Brandt", Position.MID, 83, 28, "GER"), p("Karim Adeyemi", Position.FWD, 80, 22, "GER"),
                p("Serhou Guirassy", Position.FWD, 85, 28, "GUI"), p("Jamie Gittens", Position.FWD, 80, 20, "ENG"),
            ),
        ),
        RealClubSeed(
            "rbl", "RB Leipzig", "RBL", CountryCode.GER, "ger_t1", 1, "Leipzig", "Red Bull Arena",
            "#DD0741", "#FFFFFF", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.BOLT, "#FACC15"),
            80.0, 82, Formation.F442,
            listOf(
                p("Peter Gulacsi", Position.GK, 82, 34, "HUN"), p("Willi Orban", Position.DEF, 83, 32, "HUN"),
                p("Castello Lukeba", Position.DEF, 81, 21, "FRA"), p("David Raum", Position.DEF, 81, 26, "GER"),
                p("Amadou Haidara", Position.MID, 80, 26, "MLI"), p("Xavi Simons", Position.MID, 84, 21, "NED"),
                p("Christoph Baumgartner", Position.MID, 79, 25, "AUT"), p("Lois Openda", Position.FWD, 85, 24, "BEL"),
                p("Benjamin Sesko", Position.FWD, 82, 21, "SVN"),
            ),
        ),
        RealClubSeed(
            "sge", "Eintracht Frankfurt", "SGE", CountryCode.GER, "ger_t1", 1, "Frankfurt", "Deutsche Bank Park",
            "#E1000F", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"),
            55.0, 80, Formation.F442,
            listOf(
                p("Kevin Trapp", Position.GK, 81, 34, "GER"), p("Robin Koch", Position.DEF, 80, 28, "GER"),
                p("Arthur Theate", Position.DEF, 79, 24, "BEL"), p("Ellyes Skhiri", Position.MID, 80, 29, "TUN"),
                p("Hugo Larsson", Position.MID, 79, 20, "SWE"), p("Mario Gotze", Position.MID, 79, 32, "GER"),
                p("Hugo Ekitike", Position.FWD, 81, 22, "FRA"), p("Ansgar Knauff", Position.FWD, 77, 22, "GER"),
            ),
        ),
        RealClubSeed(
            "vfb", "VfB Stuttgart", "VFB", CountryCode.GER, "ger_t1", 1, "Stuttgart", "MHP Arena",
            "#FFFFFF", "#E32219", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.CROWN, "#E32219"),
            48.0, 80, Formation.F4231,
            listOf(
                p("Alexander Nubel", Position.GK, 81, 28, "GER"), p("Maximilian Mittelstadt", Position.DEF, 80, 27, "GER"),
                p("Jeff Chabot", Position.DEF, 78, 26, "GER"), p("Angelo Stiller", Position.MID, 82, 23, "GER"),
                p("Atakan Karazor", Position.MID, 79, 28, "TUR"), p("Enzo Millot", Position.MID, 80, 22, "FRA"),
                p("Deniz Undav", Position.FWD, 81, 28, "GER"), p("Ermedin Demirovic", Position.FWD, 80, 26, "BIH"),
            ),
        ),
        RealClubSeed(
            "scf", "SC Freiburg", "SCF", CountryCode.GER, "ger_t1", 1, "Freiburg", "Europa-Park Stadion",
            "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#DC2626"),
            32.0, 77, Formation.F4231,
            listOf(
                p("Noah Atubolu", Position.GK, 76, 22, "GER"), p("Matthias Ginter", Position.DEF, 80, 30, "GER"),
                p("Philipp Lienhart", Position.DEF, 79, 28, "AUT"), p("Maximilian Eggestein", Position.MID, 77, 27, "GER"),
                p("Ritsu Doan", Position.MID, 79, 26, "JPN"), p("Vincenzo Grifo", Position.FWD, 80, 31, "ITA"),
                p("Lucas Holer", Position.FWD, 76, 30, "GER"),
            ),
        ),
        RealClubSeed(
            "svw", "Werder Bremen", "SVW", CountryCode.GER, "ger_t1", 1, "Bremen", "Weserstadion",
            "#1D9053", "#FFFFFF", defaultSpec(CrestShape.DIAMOND, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"),
            24.0, 76, Formation.F352,
            listOf(
                p("Michael Zetterer", Position.GK, 77, 29, "GER"), p("Marco Friedl", Position.DEF, 77, 26, "AUT"),
                p("Mitchell Weiser", Position.MID, 78, 30, "GER"), p("Jens Stage", Position.MID, 77, 28, "DEN"),
                p("Romano Schmid", Position.MID, 78, 24, "AUT"), p("Marvin Ducksch", Position.FWD, 78, 30, "GER"),
            ),
        ),
        // Tier 2: 2. Bundesliga
        RealClubSeed("koe", "1. FC Koln", "KOE", CountryCode.GER, "ger_t2", 2, "Cologne", "RheinEnergieStadion", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 9.0, 73, Formation.F4231, listOf(p("Marvin Schwabe", Position.GK, 75, 29, "GER"), p("Eric Martel", Position.MID, 75, 22, "GER"), p("Tim Lemperle", Position.FWD, 73, 22, "GER"))),
        RealClubSeed("hsv", "Hamburger SV", "HSV", CountryCode.GER, "ger_t2", 2, "Hamburg", "Volksparkstadion", "#0055A5", "#FFFFFF", defaultSpec(CrestShape.DIAMOND, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 9.5, 73, Formation.F433, listOf(p("Daniel Heuer Fernandes", Position.GK, 74, 32, "POR"), p("Ludovit Reis", Position.MID, 74, 24, "NED"), p("Robert Glatzel", Position.FWD, 75, 30, "GER"))),
        RealClubSeed("f95", "Fortuna Dusseldorf", "F95", CountryCode.GER, "ger_t2", 2, "Dusseldorf", "Merkur Spiel-Arena", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 6.0, 72, Formation.F4231, listOf(p("Florian Kastenmeier", Position.GK, 73, 27, "GER"), p("Isak Johannesson", Position.MID, 73, 21, "ISL"), p("Dawid Kownacki", Position.FWD, 72, 27, "POL"))),
        RealClubSeed("h96", "Hannover 96", "H96", CountryCode.GER, "ger_t2", 2, "Hanover", "Heinz von Heiden Arena", "#16A34A", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 5.5, 71, Formation.F352, listOf(p("Ron-Robert Zieler", Position.GK, 73, 35, "GER"), p("Marcel Halstenberg", Position.DEF, 74, 33, "GER"), p("Nicolo Tresoldi", Position.FWD, 72, 20, "GER"))),
        RealClubSeed("fck", "1. FC Kaiserslautern", "FCK", CountryCode.GER, "ger_t2", 2, "Kaiserslautern", "Fritz-Walter-Stadion", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"), 5.0, 71, Formation.F352, listOf(p("Julian Krahl", Position.GK, 71, 24, "GER"), p("Marlon Ritter", Position.MID, 72, 30, "GER"), p("Ragnar Ache", Position.FWD, 73, 26, "GER"))),
        RealClubSeed("s04", "Schalke 04", "S04", CountryCode.GER, "ger_t2", 2, "Gelsenkirchen", "Veltins-Arena", "#004D9D", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 6.5, 71, Formation.F4231, listOf(p("Paul Seguin", Position.MID, 71, 29, "GER"), p("Kenan Karaman", Position.FWD, 73, 30, "TUR"), p("Moussa Sylla", Position.FWD, 73, 25, "MLI"))),
        // Tier 3: 3. Liga
        RealClubSeed("sgd", "Dynamo Dresden", "SGD", CountryCode.GER, "ger_t3", 3, "Dresden", "Rudolf-Harbig-Stadion", "#FACC15", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.CROWN, "#111111"), 2.2, 66, Formation.F433, listOf(p("Tim Schreiber", Position.GK, 68, 22, "GER"), p("Christoph Daferner", Position.FWD, 68, 26, "GER"))),
        RealClubSeed("dsc", "Arminia Bielefeld", "DSC", CountryCode.GER, "ger_t3", 3, "Bielefeld", "SchucoArena", "#004E9E", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HALF_AND_HALF, CrestSymbol.STAR, "#FFFFFF"), 2.0, 65, Formation.F433, listOf(p("Jonas Kersken", Position.GK, 67, 24, "GER"), p("Mael Corboz", Position.MID, 66, 30, "USA"))),
        RealClubSeed("m60", "1860 Munich", "M60", CountryCode.GER, "ger_t3", 3, "Munich", "Grunwalder Stadion", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 1.6, 64, Formation.F4231, listOf(p("Marco Hiller", Position.GK, 65, 27, "GER"), p("Thore Jacobsen", Position.MID, 66, 27, "GER"))),
        RealClubSeed("rwe", "Rot-Weiss Essen", "RWE", CountryCode.GER, "ger_t3", 3, "Essen", "Stadion an der Hafenstrasse", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 1.5, 64, Formation.F4231, listOf(p("Jakob Golz", Position.GK, 66, 26, "GER"), p("Ahmet Arslan", Position.MID, 66, 30, "GER"))),
        RealClubSeed("fch_ger", "Hansa Rostock", "FCH", CountryCode.GER, "ger_t3", 3, "Rostock", "Ostseestadion", "#0055A5", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 1.8, 65, Formation.F352, listOf(p("Benjamin Uphoff", Position.GK, 66, 31, "GER"), p("Nils Froling", Position.FWD, 66, 24, "SWE"))),
        RealClubSeed("fce", "Energie Cottbus", "FCE", CountryCode.GER, "ger_t3", 3, "Cottbus", "LEAG Energie Stadion", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"), 1.4, 64, Formation.F433, listOf(p("Elias Bethke", Position.GK, 65, 21, "GER"), p("Timmy Thiele", Position.FWD, 66, 33, "GER"))),
        // Additional Bundesliga (+10 -> 18)
        RealClubSeed("bmg", "Borussia M'gladbach", "BMG", CountryCode.GER, "ger_t1", 1, "Monchengladbach", "Borussia-Park", "#FFFFFF", "#111111", defaultSpec(CrestShape.DIAMOND, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#16A34A"), 36.0, 77, Formation.F4231, listOf(p("Tim Kleindienst", Position.FWD, 79, 29, "GER"), p("Robin Hack", Position.FWD, 77, 26, "GER"), p("Julian Weigl", Position.MID, 78, 29, "GER"))),
        RealClubSeed("m05", "Mainz 05", "M05", CountryCode.GER, "ger_t1", 1, "Mainz", "MEWA Arena", "#C3141E", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 28.0, 77, Formation.F352, listOf(p("Robin Zentner", Position.GK, 78, 30, "GER"), p("Nadiem Amiri", Position.MID, 79, 28, "GER"), p("Jonathan Burkardt", Position.FWD, 79, 24, "GER"))),
        RealClubSeed("wob", "VfL Wolfsburg", "WOB", CountryCode.GER, "ger_t1", 1, "Wolfsburg", "Volkswagen Arena", "#65B32E", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 38.0, 77, Formation.F4231, listOf(p("Kamil Grabara", Position.GK, 78, 25, "POL"), p("Maximilian Arnold", Position.MID, 79, 30, "GER"), p("Mohamed Amoura", Position.FWD, 78, 24, "ALG"))),
        RealClubSeed("fca", "FC Augsburg", "FCA", CountryCode.GER, "ger_t1", 1, "Augsburg", "WWK Arena", "#BA3733", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LAUREL, "#FFFFFF"), 22.0, 75, Formation.F352, listOf(p("Finn Dahmen", Position.GK, 76, 26, "GER"), p("Alexis Claude-Maurice", Position.MID, 76, 26, "FRA"))),
        RealClubSeed("fcu", "Union Berlin", "FCU", CountryCode.GER, "ger_t1", 1, "Berlin", "Alte Forsterei", "#D4011D", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FACC15"), 26.0, 75, Formation.F352, listOf(p("Frederik Ronnow", Position.GK, 78, 32, "DEN"), p("Danilho Doekhi", Position.DEF, 77, 26, "NED"))),
        RealClubSeed("tsg", "TSG Hoffenheim", "TSG", CountryCode.GER, "ger_t1", 1, "Sinsheim", "PreZero Arena", "#1C63B7", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 30.0, 76, Formation.F4231, listOf(p("Oliver Baumann", Position.GK, 80, 34, "GER"), p("Andrej Kramaric", Position.FWD, 80, 33, "CRO"))),
        RealClubSeed("stp", "FC St. Pauli", "STP", CountryCode.GER, "ger_t1", 1, "Hamburg", "Millerntor-Stadion", "#624339", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 16.0, 73, Formation.F352, listOf(p("Nikola Vasilj", Position.GK, 74, 28, "BIH"), p("Jackson Irvine", Position.MID, 75, 31, "AUS"))),
        RealClubSeed("hdh", "1. FC Heidenheim", "HDH", CountryCode.GER, "ger_t1", 1, "Heidenheim", "Voith-Arena", "#E2001A", "#003D7C", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 16.0, 73, Formation.F4231, listOf(p("Kevin Muller", Position.GK, 75, 33, "GER"), p("Paul Wanner", Position.MID, 74, 18, "GER"))),
        RealClubSeed("ksv", "Holstein Kiel", "KSV", CountryCode.GER, "ger_t1", 1, "Kiel", "Holstein-Stadion", "#0055A5", "#DC2626", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 13.0, 72, Formation.F352, listOf(p("Timon Weiner", Position.GK, 72, 25, "GER"), p("Shuto Machino", Position.FWD, 73, 25, "JPN"))),
        RealClubSeed("boc_ger", "VfL Bochum", "BOC", CountryCode.GER, "ger_t1", 1, "Bochum", "Ruhrstadion", "#005CA9", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 14.0, 72, Formation.F433, listOf(p("Patrick Drewes", Position.GK, 73, 31, "GER"), p("Myron Boadu", Position.FWD, 74, 23, "NED"))),
        // Additional 2. Bundesliga (+12 -> 18)
        RealClubSeed("ger2_bsc", "Hertha BSC", "BSC", CountryCode.GER, "ger_t2", 2, "Berlin", "Olympiastadion", "#0055A5", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FFFFFF"), 8.5, 72, Formation.F4231, listOf(p("Fabian Reese", Position.FWD, 74, 26, "GER"))),
        RealClubSeed("ger2_ksc", "Karlsruher SC", "KSC", CountryCode.GER, "ger_t2", 2, "Karlsruhe", "BBBank Wildpark", "#0055A5", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 6.0, 71, Formation.F442, listOf(p("Marvin Wanitzek", Position.MID, 74, 31, "GER"))),
        RealClubSeed("ger2_scp", "SC Paderborn", "SCP", CountryCode.GER, "ger_t2", 2, "Paderborn", "Home Deluxe Arena", "#0055A5", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 5.5, 71, Formation.F352),
        RealClubSeed("ger2_fcm", "1. FC Magdeburg", "FCM", CountryCode.GER, "ger_t2", 2, "Magdeburg", "Avnet Arena", "#0055A5", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 5.2, 71, Formation.F433, listOf(p("Martijn Kaars", Position.FWD, 73, 25, "NED"))),
        RealClubSeed("ger2_sve", "SV Elversberg", "ELV", CountryCode.GER, "ger_t2", 2, "Spiesen-Elversberg", "Waldstadion", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#111111"), 4.8, 70, Formation.F4231, listOf(p("Fisnik Asllani", Position.FWD, 72, 22, "KOS"))),
        RealClubSeed("ger2_fcn", "1. FC Nurnberg", "FCN", CountryCode.GER, "ger_t2", 2, "Nuremberg", "Max-Morlock-Stadion", "#7F1D1D", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 6.0, 70, Formation.F352, listOf(p("Stefanos Tzimas", Position.FWD, 73, 18, "GRE"))),
        RealClubSeed("ger2_svd", "Darmstadt 98", "D98", CountryCode.GER, "ger_t2", 2, "Darmstadt", "Bollenfalltor", "#0055A5", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LAUREL, "#FFFFFF"), 5.5, 70, Formation.F442),
        RealClubSeed("ger2_sgf", "Greuther Furth", "SGF", CountryCode.GER, "ger_t2", 2, "Furth", "Sportpark Ronhof", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LAUREL, "#FFFFFF"), 5.0, 69, Formation.F352),
        RealClubSeed("ger2_eb", "Eintracht Braunschweig", "EBS", CountryCode.GER, "ger_t2", 2, "Braunschweig", "Eintracht-Stadion", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#1D4ED8"), 4.6, 69, Formation.F352),
        RealClubSeed("ger2_ssv", "SSV Ulm", "ULM", CountryCode.GER, "ger_t2", 2, "Ulm", "Donaustadion", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.EAGLE, "#111111"), 4.2, 68, Formation.F352),
        RealClubSeed("ger2_prm", "Preussen Munster", "SCP", CountryCode.GER, "ger_t2", 2, "Munster", "Preussenstadion", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 4.0, 68, Formation.F4231),
        RealClubSeed("ger2_jahn", "Jahn Regensburg", "SSV", CountryCode.GER, "ger_t2", 2, "Regensburg", "Jahnstadion", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 3.8, 68, Formation.F4231),
        // Additional 3. Liga (+14 -> 20)
        RealClubSeed("ger3_fcs", "1. FC Saarbrucken", "FCS", CountryCode.GER, "ger_t3", 3, "Saarbrucken", "Ludwigspark", "#1D4ED8", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FFFFFF"), 1.8, 65, Formation.F352),
        RealClubSeed("ger3_fci", "FC Ingolstadt", "FCI", CountryCode.GER, "ger_t3", 3, "Ingolstadt", "Audi Sportpark", "#111111", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.LION, "#FFFFFF"), 1.9, 65, Formation.F442),
        RealClubSeed("ger3_vfl", "VfL Osnabruck", "OSN", CountryCode.GER, "ger_t3", 3, "Osnabruck", "Bremer Brucke", "#7C3AED", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 1.6, 64, Formation.F4231),
        RealClubSeed("ger3_svw", "Wehen Wiesbaden", "WEH", CountryCode.GER, "ger_t3", 3, "Wiesbaden", "BRITA-Arena", "#DC2626", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#FACC15"), 1.7, 65, Formation.F352),
        RealClubSeed("ger3_wal", "Waldhof Mannheim", "WAL", CountryCode.GER, "ger_t3", 3, "Mannheim", "Carl-Benz-Stadion", "#1D4ED8", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FFFFFF"), 1.5, 64, Formation.F4231),
        RealClubSeed("ger3_vic", "Viktoria Koln", "VIC", CountryCode.GER, "ger_t3", 3, "Cologne", "Sportpark Hohenberg", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 1.4, 64, Formation.F4231),
        RealClubSeed("ger3_ale", "Alemannia Aachen", "AAC", CountryCode.GER, "ger_t3", 3, "Aachen", "Tivoli", "#FACC15", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.EAGLE, "#111111"), 1.5, 64, Formation.F352),
        RealClubSeed("ger3_ext", "SC Verl", "VER", CountryCode.GER, "ger_t3", 3, "Verl", "Sportclub Arena", "#FFFFFF", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#111111"), 1.3, 64, Formation.F433),
        RealClubSeed("ger3_erc", "Erzgebirge Aue", "AUE", CountryCode.GER, "ger_t3", 3, "Aue", "Erzgebirgsstadion", "#7C3AED", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 1.6, 64, Formation.F4231),
        RealClubSeed("ger3_svs", "SV Sandhausen", "SVS", CountryCode.GER, "ger_t3", 3, "Sandhausen", "GP Stadion am Hardtwald", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 1.5, 64, Formation.F352),
        RealClubSeed("ger3_unt", "SpVgg Unterhaching", "HCH", CountryCode.GER, "ger_t3", 3, "Unterhaching", "Sportpark", "#DC2626", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.STAR, "#FFFFFF"), 1.3, 63, Formation.F442),
        RealClubSeed("ger3_bvb2", "Borussia Dortmund II", "BVB2", CountryCode.GER, "ger_t3", 3, "Dortmund", "Rote Erde", "#FDE100", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.BOLT, "#111111"), 1.5, 64, Formation.F4231),
        RealClubSeed("ger3_h96b", "Hannover 96 II", "H96B", CountryCode.GER, "ger_t3", 3, "Hanover", "Eilenriedestadion", "#16A34A", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 1.2, 63, Formation.F433),
        RealClubSeed("ger3_vfb2", "VfB Stuttgart II", "VFB2", CountryCode.GER, "ger_t3", 3, "Stuttgart", "WIRmachenDRUCK Arena", "#FFFFFF", "#E32219", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.CROWN, "#E32219"), 1.4, 63, Formation.F4231),
    )

    private fun createFranceSeeds(): List<RealClubSeed> = listOf(
        // Tier 1: Ligue 1
        RealClubSeed(
            "psg", "Paris Saint-Germain", "PSG", CountryCode.FRA, "fra_t1", 1, "Paris", "Parc des Princes",
            "#004170", "#DA291C", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"),
            180.0, 86, Formation.F433,
            listOf(
                p("Gianluigi Donnarumma", Position.GK, 87, 25, "ITA"), p("Nuno Mendes", Position.DEF, 84, 22, "POR"),
                p("Willian Pacho", Position.DEF, 82, 23, "ECU"), p("Marquinhos", Position.DEF, 86, 30, "BRA"),
                p("Achraf Hakimi", Position.DEF, 86, 26, "MAR"), p("Vitinha", Position.MID, 86, 24, "POR"),
                p("Joao Neves", Position.MID, 84, 20, "POR"), p("Warren Zaire-Emery", Position.MID, 82, 18, "FRA"),
                p("Bradley Barcola", Position.FWD, 84, 22, "FRA"), p("Ousmane Dembele", Position.FWD, 88, 27, "FRA"),
                p("Khvicha Kvaratskhelia", Position.FWD, 86, 23, "GEO"), p("Desire Doue", Position.FWD, 81, 19, "FRA"),
            ),
        ),
        RealClubSeed(
            "om", "Olympique de Marseille", "OM", CountryCode.FRA, "fra_t1", 1, "Marseille", "Orange Velodrome",
            "#2FAEE0", "#FFFFFF", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.STAR, "#D4AF37"),
            55.0, 80, Formation.F4231,
            listOf(
                p("Geronimo Rulli", Position.GK, 80, 32, "ARG"), p("Leonardo Balerdi", Position.DEF, 80, 25, "ARG"),
                p("Pierre-Emile Hojbjerg", Position.MID, 82, 29, "DEN"), p("Adrien Rabiot", Position.MID, 83, 29, "FRA"),
                p("Mason Greenwood", Position.FWD, 82, 23, "ENG"), p("Amine Gouiri", Position.FWD, 79, 24, "ALG"),
            ),
        ),
        RealClubSeed(
            "asm", "AS Monaco", "ASM", CountryCode.FRA, "fra_t1", 1, "Monaco", "Stade Louis II",
            "#E2001A", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.CROWN, "#D4AF37"),
            60.0, 80, Formation.F4231,
            listOf(
                p("Radoslaw Majecki", Position.GK, 77, 25, "POL"), p("Thilo Kehrer", Position.DEF, 79, 28, "GER"),
                p("Denis Zakaria", Position.MID, 81, 28, "SUI"), p("Aleksandr Golovin", Position.MID, 81, 28, "RUS"),
                p("Maghnes Akliouche", Position.MID, 79, 22, "FRA"), p("Breel Embolo", Position.FWD, 79, 27, "SUI"),
                p("Mika Biereth", Position.FWD, 78, 21, "DEN"),
            ),
        ),
        RealClubSeed(
            "losc", "Lille OSC", "LIL", CountryCode.FRA, "fra_t1", 1, "Lille", "Decathlon Arena",
            "#E01E13", "#1A2238", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"),
            45.0, 79, Formation.F4231,
            listOf(
                p("Lucas Chevalier", Position.GK, 81, 23, "FRA"), p("Bafode Diakite", Position.DEF, 79, 23, "FRA"),
                p("Benjamin Andre", Position.MID, 79, 34, "FRA"), p("Ayyoub Bouaddi", Position.MID, 76, 17, "FRA"),
                p("Edon Zhegrova", Position.FWD, 81, 25, "KOS"), p("Jonathan David", Position.FWD, 83, 24, "CAN"),
            ),
        ),
        RealClubSeed(
            "nice", "OGC Nice", "NIC", CountryCode.FRA, "fra_t1", 1, "Nice", "Allianz Riviera",
            "#CC0000", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.EAGLE, "#FACC15"),
            38.0, 78, Formation.F352,
            listOf(
                p("Marcin Bulka", Position.GK, 80, 25, "POL"), p("Dante", Position.DEF, 77, 41, "BRA"),
                p("Jonathan Clauss", Position.DEF, 79, 32, "FRA"), p("Evann Guessand", Position.FWD, 78, 23, "CIV"),
                p("Mohamed-Ali Cho", Position.FWD, 77, 20, "FRA"),
            ),
        ),
        RealClubSeed(
            "lyon", "Olympique Lyonnais", "OL", CountryCode.FRA, "fra_t1", 1, "Lyon", "Groupama Stadium",
            "#1E4595", "#D71920", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FACC15"),
            48.0, 79, Formation.F433,
            listOf(
                p("Lucas Perri", Position.GK, 79, 26, "BRA"), p("Moussa Niakhate", Position.DEF, 78, 28, "SEN"),
                p("Corentin Tolisso", Position.MID, 80, 30, "FRA"), p("Rayan Cherki", Position.MID, 81, 21, "FRA"),
                p("Alexandre Lacazette", Position.FWD, 81, 33, "FRA"), p("Georges Mikautadze", Position.FWD, 79, 24, "GEO"),
            ),
        ),
        RealClubSeed(
            "rcl", "RC Lens", "LEN", CountryCode.FRA, "fra_t1", 1, "Lens", "Stade Bollaert-Delelis",
            "#FFD100", "#E30613", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.TOWER, "#111111"),
            28.0, 77, Formation.F352,
            listOf(
                p("Mathew Ryan", Position.GK, 76, 32, "AUS"), p("Facundo Medina", Position.DEF, 80, 25, "ARG"),
                p("Adrien Thomasson", Position.MID, 76, 30, "FRA"), p("M'Bala Nzola", Position.FWD, 76, 28, "ANG"),
            ),
        ),
        RealClubSeed(
            "ren", "Stade Rennais", "REN", CountryCode.FRA, "fra_t1", 1, "Rennes", "Roazhon Park",
            "#E4002B", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.CROWN, "#FFFFFF"),
            34.0, 77, Formation.F352,
            listOf(
                p("Brice Samba", Position.GK, 80, 30, "FRA"), p("Seko Fofana", Position.MID, 79, 29, "CIV"),
                p("Ludovic Blas", Position.MID, 78, 26, "FRA"), p("Arnaud Kalimuendo", Position.FWD, 78, 22, "FRA"),
            ),
        ),
        // Tier 2: Ligue 2 BKT
        RealClubSeed("lor", "FC Lorient", "LOR", CountryCode.FRA, "fra_t2", 2, "Lorient", "Stade du Moustoir", "#F97316", "#111111", defaultSpec(CrestShape.DIAMOND, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 6.5, 72, Formation.F4231, listOf(p("Yvon Mvogo", Position.GK, 74, 30, "SUI"), p("Eli Junior Kroupi", Position.FWD, 73, 18, "FRA"))),
        RealClubSeed("pfc", "Paris FC", "PFC", CountryCode.FRA, "fra_t2", 2, "Paris", "Stade Charlety", "#1E3A8A", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 9.0, 71, Formation.F4231, listOf(p("Maxime Lopez", Position.MID, 74, 26, "FRA"), p("Jean-Philippe Krasso", Position.FWD, 73, 27, "CIV"))),
        RealClubSeed("met", "FC Metz", "MET", CountryCode.FRA, "fra_t2", 2, "Metz", "Stade Saint-Symphorien", "#7F1D1D", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.CROSS, CrestSymbol.STAR, "#FFFFFF"), 5.5, 71, Formation.F4231, listOf(p("Matthieu Udol", Position.DEF, 73, 28, "FRA"), p("Gauthier Hein", Position.MID, 73, 28, "FRA"))),
        RealClubSeed("dun", "USL Dunkerque", "DUN", CountryCode.FRA, "fra_t2", 2, "Dunkerque", "Stade Marcel-Tribut", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 3.5, 69, Formation.F433, listOf(p("Opa Sangante", Position.DEF, 70, 33, "GNB"), p("Enzo Bardeli", Position.MID, 71, 23, "FRA"))),
        RealClubSeed("eag", "EA Guingamp", "EAG", CountryCode.FRA, "fra_t2", 2, "Guingamp", "Stade du Roudourou", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 3.8, 69, Formation.F442, listOf(p("Enzo Basilio", Position.GK, 69, 30, "FRA"), p("Jacques Siwe", Position.FWD, 70, 23, "FRA"))),
        RealClubSeed("cae", "SM Caen", "CAE", CountryCode.FRA, "fra_t2", 2, "Caen", "Michel d'Ornano", "#1D4ED8", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FFFFFF"), 4.2, 69, Formation.F4231, listOf(p("Anthony Mandrea", Position.GK, 71, 27, "ALG"), p("Alexandre Mendy", Position.FWD, 73, 30, "GNB"))),
        // Tier 3: Championnat National
        RealClubSeed("nan_fra", "AS Nancy-Lorraine", "NAN", CountryCode.FRA, "fra_t3", 3, "Nancy", "Marcel Picot", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"), 1.5, 64, Formation.F352, listOf(p("Martin Sourzac", Position.GK, 64, 32, "FRA"), p("Cheikh Toure", Position.FWD, 65, 23, "SEN"))),
        RealClubSeed("lem", "Le Mans FC", "LEM", CountryCode.FRA, "fra_t3", 3, "Le Mans", "Stade Marie-Marvingt", "#DC2626", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.BOLT, "#FACC15"), 1.4, 64, Formation.F352, listOf(p("Nicolas Kocik", Position.GK, 64, 26, "FRA"), p("Dame Gueye", Position.FWD, 65, 29, "SEN"))),
        RealClubSeed("bou", "US Boulogne", "USB", CountryCode.FRA, "fra_t3", 3, "Boulogne", "Stade de la Liberation", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.ANCHOR, "#FFFFFF"), 1.1, 63, Formation.F442, listOf(p("Yannick Pandor", Position.GK, 64, 23, "COM"), p("Thibault Rambaud", Position.FWD, 64, 27, "FRA"))),
        RealClubSeed("dij", "Dijon FCO", "DIJ", CountryCode.FRA, "fra_t3", 3, "Dijon", "Gaston Gerard", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 1.5, 64, Formation.F433, listOf(p("Paul Delecroix", Position.GK, 64, 36, "FRA"), p("Alexandre Parsemain", Position.FWD, 64, 21, "FRA"))),
        RealClubSeed("soc", "FC Sochaux", "SOC", CountryCode.FRA, "fra_t3", 3, "Montbeliard", "Auguste Bonal", "#FACC15", "#1E3A8A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#1E3A8A"), 1.6, 64, Formation.F442, listOf(p("Mathieu Patouillet", Position.GK, 65, 20, "FRA"), p("Armand Gnanduillet", Position.FWD, 65, 32, "CIV"))),
        RealClubSeed("orl", "US Orleans", "ORL", CountryCode.FRA, "fra_t3", 3, "Orleans", "Stade de la Source", "#FACC15", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.STAR, "#DC2626"), 1.2, 63, Formation.F433, listOf(p("Vincent Viot", Position.GK, 64, 30, "FRA"), p("Fahd El Khoumisti", Position.FWD, 65, 31, "FRA"))),
        // Additional Ligue 1 (+10 -> 18)
        RealClubSeed("str_fra", "RC Strasbourg", "RCS", CountryCode.FRA, "fra_t1", 1, "Strasbourg", "La Meinau", "#009EE0", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FFFFFF"), 32.0, 77, Formation.F352, listOf(p("Djordje Petrovic", Position.GK, 78, 25, "SRB"), p("Andrey Santos", Position.MID, 79, 20, "BRA"), p("Emanuel Emegha", Position.FWD, 77, 21, "NED"))),
        RealClubSeed("bre_fra", "Stade Brestois", "SB29", CountryCode.FRA, "fra_t1", 1, "Brest", "Francis-Le Ble", "#E30613", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 24.0, 76, Formation.F433, listOf(p("Marco Bizot", Position.GK, 78, 33, "NED"), p("Pierre Lees-Melou", Position.MID, 79, 31, "FRA"), p("Ludovic Ajorque", Position.FWD, 77, 30, "FRA"))),
        RealClubSeed("tou_fra", "Toulouse FC", "TFC", CountryCode.FRA, "fra_t1", 1, "Toulouse", "Stadium de Toulouse", "#6B21A8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 22.0, 75, Formation.F352, listOf(p("Guillaume Restes", Position.GK, 77, 19, "FRA"), p("Zakaria Aboukhlal", Position.FWD, 76, 24, "MAR"))),
        RealClubSeed("aux", "AJ Auxerre", "AJA", CountryCode.FRA, "fra_t1", 1, "Auxerre", "Abbe-Deschamps", "#FFFFFF", "#0055A5", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#0055A5"), 16.0, 74, Formation.F352, listOf(p("Donovan Leon", Position.GK, 75, 32, "FRA"), p("Gaetan Perrin", Position.FWD, 76, 28, "FRA"))),
        RealClubSeed("ang", "Angers SCO", "SCO", CountryCode.FRA, "fra_t1", 1, "Angers", "Raymond Kopa", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FACC15"), 14.0, 73, Formation.F4231, listOf(p("Yahia Fofana", Position.GK, 76, 24, "CIV"), p("Himad Abdelli", Position.MID, 75, 25, "ALG"))),
        RealClubSeed("nan", "FC Nantes", "FCN", CountryCode.FRA, "fra_t1", 1, "Nantes", "La Beaujoire", "#FFE600", "#00853E", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#00853E"), 18.0, 74, Formation.F433, listOf(p("Moses Simon", Position.FWD, 78, 29, "NGA"), p("Matthis Abline", Position.FWD, 75, 21, "FRA"))),
        RealClubSeed("rei", "Stade de Reims", "SDR", CountryCode.FRA, "fra_t1", 1, "Reims", "Auguste-Delaune", "#D71920", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 20.0, 74, Formation.F433, listOf(p("Yehvann Diouf", Position.GK, 77, 25, "SEN"), p("Keito Nakamura", Position.FWD, 77, 24, "JPN"))),
        RealClubSeed("ste_fra", "AS Saint-Etienne", "ASSE", CountryCode.FRA, "fra_t1", 1, "Saint-Etienne", "Geoffroy-Guichard", "#00853E", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 16.0, 73, Formation.F433, listOf(p("Gautier Larsonneur", Position.GK, 75, 27, "FRA"), p("Zuriko Davitashvili", Position.FWD, 76, 23, "GEO"))),
        RealClubSeed("leh", "Le Havre AC", "HAC", CountryCode.FRA, "fra_t1", 1, "Le Havre", "Stade Oceane", "#003882", "#38BDF8", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HALF_AND_HALF, CrestSymbol.ANCHOR, "#FFFFFF"), 13.0, 72, Formation.F4231, listOf(p("Abdoulaye Toure", Position.MID, 74, 30, "GUI"))),
        RealClubSeed("mhsc", "Montpellier HSC", "MHSC", CountryCode.FRA, "fra_t1", 1, "Montpellier", "La Mosson", "#002B5C", "#F97316", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LAUREL, "#F97316"), 14.0, 73, Formation.F4231, listOf(p("Teji Savanier", Position.MID, 78, 32, "FRA"))),
        // Additional Ligue 2 BKT (+12 -> 18)
        RealClubSeed("fra2_ann", "FC Annecy", "FCA", CountryCode.FRA, "fra_t2", 2, "Annecy", "Parc des Sports", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 3.6, 69, Formation.F4231),
        RealClubSeed("fra2_lav", "Stade Lavallois", "LAV", CountryCode.FRA, "fra_t2", 2, "Laval", "Francis Le Basser", "#F97316", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#111111"), 3.8, 69, Formation.F352),
        RealClubSeed("fra2_bas", "SC Bastia", "SCB", CountryCode.FRA, "fra_t2", 2, "Bastia", "Armand-Cesari", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 3.6, 69, Formation.F4231),
        RealClubSeed("fra2_gre", "Grenoble Foot 38", "GF38", CountryCode.FRA, "fra_t2", 2, "Grenoble", "Stade des Alpes", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LAUREL, "#FFFFFF"), 3.8, 69, Formation.F433),
        RealClubSeed("fra2_ami", "Amiens SC", "ASC", CountryCode.FRA, "fra_t2", 2, "Amiens", "Credit Agricole la Licorne", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#111111"), 4.2, 69, Formation.F4231),
        RealClubSeed("fra2_pau", "Pau FC", "PAU", CountryCode.FRA, "fra_t2", 2, "Pau", "Nouste Camp", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#1D4ED8"), 3.4, 68, Formation.F4231),
        RealClubSeed("fra2_rod", "Rodez AF", "RAF", CountryCode.FRA, "fra_t2", 2, "Rodez", "Paul-Lignon", "#DC2626", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.EAGLE, "#FFFFFF"), 3.2, 68, Formation.F352),
        RealClubSeed("fra2_tro", "ESTAC Troyes", "EST", CountryCode.FRA, "fra_t2", 2, "Troyes", "Stade de l'Aube", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 4.5, 69, Formation.F433),
        RealClubSeed("fra2_cle", "Clermont Foot", "CF63", CountryCode.FRA, "fra_t2", 2, "Clermont-Ferrand", "Gabriel-Montpied", "#7F1D1D", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 4.4, 69, Formation.F4231),
        RealClubSeed("fra2_aja", "AC Ajaccio", "ACA", CountryCode.FRA, "fra_t2", 2, "Ajaccio", "Michel-Moretti", "#FFFFFF", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#DC2626"), 3.8, 68, Formation.F442),
        RealClubSeed("fra2_red", "Red Star FC", "RED", CountryCode.FRA, "fra_t2", 2, "Saint-Ouen", "Stade Bauer", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#DC2626"), 3.6, 68, Formation.F352),
        RealClubSeed("fra2_mar", "FC Martigues", "FCM", CountryCode.FRA, "fra_t2", 2, "Martigues", "Francis-Turcan", "#FACC15", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.ANCHOR, "#FFFFFF"), 2.8, 67, Formation.F442),
        // Additional Championnat National (+12 -> 18)
        RealClubSeed("fra3_val", "Valenciennes FC", "VAFC", CountryCode.FRA, "fra_t3", 3, "Valenciennes", "Stade du Hainaut", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 1.7, 65, Formation.F433),
        RealClubSeed("fra3_con", "US Concarneau", "USC", CountryCode.FRA, "fra_t3", 3, "Concarneau", "Guy-Piriou", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 1.4, 64, Formation.F4231),
        RealClubSeed("fra3_qrm", "Quevilly-Rouen", "QRM", CountryCode.FRA, "fra_t3", 3, "Le Petit-Quevilly", "Robert-Diochon", "#DC2626", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#FACC15"), 1.4, 64, Formation.F4231),
        RealClubSeed("fra3_ver", "FC Versailles", "FCV", CountryCode.FRA, "fra_t3", 3, "Versailles", "Jean-Bouin", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#FACC15"), 1.6, 64, Formation.F352),
        RealClubSeed("fra3_par", "Paris 13 Atletico", "P13", CountryCode.FRA, "fra_t3", 3, "Paris", "Pele Stadium", "#16A34A", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 1.2, 63, Formation.F442),
        RealClubSeed("fra3_fcp", "FC Rouen", "FCR", CountryCode.FRA, "fra_t3", 3, "Rouen", "Robert-Diochon", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"), 1.4, 64, Formation.F352),
        RealClubSeed("fra3_bou", "Bourg-en-Bresse", "FBBP", CountryCode.FRA, "fra_t3", 3, "Bourg-en-Bresse", "Marcel-Verchere", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 1.2, 63, Formation.F433),
        RealClubSeed("fra3_aub", "Aubagne FC", "AFC", CountryCode.FRA, "fra_t3", 3, "Aubagne", "De Lattre", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#1D4ED8"), 1.2, 63, Formation.F442),
        RealClubSeed("fra3_cha", "LB Chateauroux", "LBC", CountryCode.FRA, "fra_t3", 3, "Chateauroux", "Gaston-Petit", "#1D4ED8", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.TOWER, "#FFFFFF"), 1.3, 63, Formation.F4231),
        RealClubSeed("fra3_vil", "FC Villefranche", "FCVB", CountryCode.FRA, "fra_t3", 3, "Villefranche", "Armand-Chouffet", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 1.2, 63, Formation.F442),
        RealClubSeed("fra3_nim", "Nimes Olympique", "NO", CountryCode.FRA, "fra_t3", 3, "Nimes", "Les Antonins", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 1.3, 63, Formation.F4231),
        RealClubSeed("fra3_mar", "US Avranches", "USA", CountryCode.FRA, "fra_t3", 3, "Avranches", "Rene-Fenouillere", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 1.1, 63, Formation.F442),
    )
}
