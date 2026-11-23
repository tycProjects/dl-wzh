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
 * Modular Real-World Americas Pyramids:
 * - Brazil: Brasileirao Serie A & Serie B
 * - Argentina: Liga Profesional & Primera Nacional
 * - USA: Major League Soccer (MLS)
 */
object AmericasClubsData {

    val divisions: List<DivisionInfo> = listOf(
        // Brazil
        DivisionInfo("bra_t1", "Brasileirao Serie A", CountryCode.BRA, 1, 0, 0, 2),
        DivisionInfo("bra_t2", "Brasileirao Serie B", CountryCode.BRA, 2, 2, 2, 0),
        // Argentina
        DivisionInfo("arg_t1", "Liga Profesional", CountryCode.ARG, 1, 0, 0, 2),
        DivisionInfo("arg_t2", "Primera Nacional", CountryCode.ARG, 2, 2, 2, 0),
        // USA
        DivisionInfo("usa_t1", "Major League Soccer", CountryCode.USA, 1, 0, 0, 0),
    )

    private fun p(name: String, pos: Position, ovr: Int, age: Int, nat: String) =
        RealPlayerSeed(name, pos, ovr, age, nat)

    private val brazilPool = listOf(
        p("Lucas Perri", Position.GK, 76, 26, "BRA"), p("Rafael", Position.GK, 75, 35, "BRA"),
        p("Brazão", Position.GK, 71, 24, "BRA"), p("Caique Franca", Position.GK, 68, 29, "BRA"),
        p("Leonardo Jardim", Position.GK, 74, 29, "BRA"), p("Marcelo Lomba", Position.GK, 72, 37, "BRA"),
        p("Fabricio Bruno", Position.DEF, 77, 28, "BRA"), p("Murilo", Position.DEF, 77, 27, "BRA"),
        p("Alex Sandro", Position.DEF, 77, 33, "BRA"), p("Bastos", Position.DEF, 76, 33, "ANG"),
        p("Guilherme Arana", Position.DEF, 77, 27, "BRA"), p("Vitao", Position.DEF, 75, 24, "BRA"),
        p("Alan Franco", Position.DEF, 74, 28, "ARG"), p("Luciano Juba", Position.DEF, 73, 25, "BRA"),
        p("Joaquim", Position.DEF, 73, 25, "BRA"), p("Marcos Rocha", Position.DEF, 74, 35, "BRA"),
        p("Gerson", Position.MID, 80, 27, "BRA"), p("Raphael Veiga", Position.MID, 80, 29, "BRA"),
        p("Thiago Almada", Position.MID, 79, 23, "ARG"), p("Alan Patrick", Position.MID, 78, 33, "BRA"),
        p("Matheus Pereira", Position.MID, 78, 28, "BRA"), p("Rodrigo Garro", Position.MID, 78, 26, "ARG"),
        p("Lucas Moura", Position.MID, 79, 32, "BRA"), p("Caio Alexandre", Position.MID, 74, 25, "BRA"),
        p("Everton Ribeiro", Position.MID, 75, 35, "BRA"), p("Jean Lucas", Position.MID, 73, 26, "BRA"),
        p("Pedro", Position.FWD, 80, 27, "BRA"), p("Estevao", Position.FWD, 78, 17, "BRA"),
        p("Igor Jesus", Position.FWD, 77, 23, "BRA"), p("Luiz Henrique", Position.FWD, 78, 23, "BRA"),
        p("Yuri Alberto", Position.FWD, 77, 23, "BRA"), p("Hulk", Position.FWD, 79, 38, "BRA"),
        p("Marcos Leonardo", Position.FWD, 77, 21, "BRA"), p("Guilherme", Position.FWD, 71, 29, "BRA"),
    )

    private val argentinaPool = listOf(
        p("Franco Armani", Position.GK, 76, 38, "ARG"), p("Sergio Romero", Position.GK, 74, 37, "ARG"),
        p("Gabriel Arias", Position.GK, 75, 37, "CHI"), p("Guido Herrera", Position.GK, 75, 32, "ARG"),
        p("Tomas Marchiori", Position.GK, 73, 29, "ARG"), p("Rodrigo Rey", Position.GK, 74, 33, "ARG"),
        p("German Pezzella", Position.DEF, 77, 33, "ARG"), p("Marcos Rojo", Position.DEF, 75, 34, "ARG"),
        p("Gonzalo Montiel", Position.DEF, 77, 27, "ARG"), p("Marcos Acuna", Position.DEF, 78, 33, "ARG"),
        p("Marco Di Cesare", Position.DEF, 74, 22, "ARG"), p("Valentin Gomez", Position.DEF, 75, 21, "ARG"),
        p("Kevin Lomonaco", Position.DEF, 74, 22, "ARG"), p("Gaston Campi", Position.DEF, 72, 33, "ARG"),
        p("Santiago Ascacibar", Position.MID, 76, 27, "ARG"), p("Juanfer Quintero", Position.MID, 77, 31, "COL"),
        p("Claudio Aquino", Position.MID, 75, 33, "ARG"), p("Kevin Zenon", Position.MID, 76, 23, "ARG"),
        p("Franco Mastantuono", Position.MID, 75, 17, "ARG"), p("Cristian Medina", Position.MID, 75, 22, "ARG"),
        p("Iker Muniain", Position.MID, 74, 31, "ESP"), p("Felipe Loyola", Position.MID, 73, 24, "CHI"),
        p("Adrian Martinez", Position.FWD, 76, 32, "ARG"), p("Miguel Borja", Position.FWD, 76, 31, "COL"),
        p("Edinson Cavani", Position.FWD, 77, 37, "URU"), p("Braian Romero", Position.FWD, 74, 33, "ARG"),
        p("Sebastian Driussi", Position.FWD, 76, 28, "ARG"), p("Gabriel Avalos", Position.FWD, 73, 34, "PAR"),
        p("Bruno Barticciotto", Position.FWD, 72, 23, "CHI"), p("Junior Arias", Position.FWD, 68, 31, "URU"),
    )

    private val usaPool = listOf(
        p("Roman Burki", Position.GK, 77, 34, "SUI"), p("Hugo Lloris", Position.GK, 78, 37, "FRA"),
        p("Drake Callender", Position.GK, 74, 27, "USA"), p("Brad Guzan", Position.GK, 71, 40, "USA"),
        p("Stefan Frei", Position.GK, 74, 38, "SUI"), p("Patrick Schulte", Position.GK, 74, 23, "USA"),
        p("Jordi Alba", Position.DEF, 79, 35, "ESP"), p("Miles Robinson", Position.DEF, 75, 27, "USA"),
        p("Walker Zimmerman", Position.DEF, 75, 31, "USA"), p("Yeimar Gomez", Position.DEF, 74, 32, "COL"),
        p("Maya Yoshida", Position.DEF, 74, 36, "JPN"), p("Aaron Long", Position.DEF, 73, 32, "USA"),
        p("Steven Moreira", Position.DEF, 74, 30, "CPV"), p("Matt Miazga", Position.DEF, 75, 29, "USA"),
        p("Sergio Busquets", Position.MID, 80, 36, "ESP"), p("Riqui Puig", Position.MID, 80, 25, "ESP"),
        p("Luciano Acosta", Position.MID, 80, 30, "ARG"), p("Emil Forsberg", Position.MID, 77, 33, "SWE"),
        p("Federico Redondo", Position.MID, 74, 21, "ARG"), p("Darlington Nagbe", Position.MID, 75, 34, "USA"),
        p("Albert Rusnak", Position.MID, 75, 30, "SVK"), p("Aleksei Miranchuk", Position.MID, 76, 29, "RUS"),
        p("Lionel Messi", Position.FWD, 88, 37, "ARG"), p("Luis Suarez", Position.FWD, 80, 37, "URU"),
        p("Denis Bouanga", Position.FWD, 79, 30, "GAB"), p("Olivier Giroud", Position.FWD, 77, 38, "FRA"),
        p("Marco Reus", Position.FWD, 77, 35, "GER"), p("Gabriel Pec", Position.FWD, 76, 23, "BRA"),
        p("Kevin Denkey", Position.FWD, 77, 23, "TOG"), p("Jordan Morris", Position.FWD, 74, 30, "USA"),
    )

    fun createClubs(): List<Club> {
        val bra = createBrazilSeeds().map { RealPlayerRosterBuilder.buildClub(it, brazilPool) }
        val arg = createArgentinaSeeds().map { RealPlayerRosterBuilder.buildClub(it, argentinaPool) }
        val usa = createUsaSeeds().map { RealPlayerRosterBuilder.buildClub(it, usaPool) }
        return bra + arg + usa
    }

    private fun createBrazilSeeds(): List<RealClubSeed> = listOf(
        RealClubSeed(
            "fla", "Flamengo", "FLA", CountryCode.BRA, "bra_t1", 1, "Rio de Janeiro", "Maracana",
            "#C8102E", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.STAR, "#FFFFFF"),
            65.0, 80, Formation.F4231,
            listOf(
                p("Agustin Rossi", Position.GK, 77, 29, "ARG"), p("Leo Ortiz", Position.DEF, 78, 28, "BRA"),
                p("Alex Sandro", Position.DEF, 77, 33, "BRA"), p("Gerson", Position.MID, 80, 27, "BRA"),
                p("Giorgian de Arrascaeta", Position.MID, 81, 30, "URU"), p("Nicolas de la Cruz", Position.MID, 79, 27, "URU"),
                p("Bruno Henrique", Position.FWD, 77, 33, "BRA"), p("Pedro", Position.FWD, 80, 27, "BRA"),
            ),
        ),
        RealClubSeed(
            "pal_bra", "Palmeiras", "PAL", CountryCode.BRA, "bra_t1", 1, "Sao Paulo", "Allianz Parque",
            "#006437", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"),
            62.0, 80, Formation.F4231,
            listOf(
                p("Weverton", Position.GK, 79, 36, "BRA"), p("Gustavo Gomez", Position.DEF, 79, 31, "PAR"),
                p("Murilo", Position.DEF, 77, 27, "BRA"), p("Richard Rios", Position.MID, 78, 24, "COL"),
                p("Raphael Veiga", Position.MID, 80, 29, "BRA"), p("Estevao", Position.FWD, 79, 17, "BRA"),
                p("Vitor Roque", Position.FWD, 77, 19, "BRA"),
            ),
        ),
        RealClubSeed(
            "bot", "Botafogo", "BOT", CountryCode.BRA, "bra_t1", 1, "Rio de Janeiro", "Nilton Santos",
            "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"),
            50.0, 79, Formation.F4231,
            listOf(
                p("John Victor", Position.GK, 77, 28, "BRA"), p("Alexander Barboza", Position.DEF, 76, 29, "ARG"),
                p("Gregore", Position.MID, 76, 30, "BRA"), p("Marlon Freitas", Position.MID, 77, 29, "BRA"),
                p("Savarino", Position.MID, 78, 28, "VEN"), p("Igor Jesus", Position.FWD, 78, 23, "BRA"),
            ),
        ),
        RealClubSeed(
            "cam_bra", "Atletico Mineiro", "CAM", CountryCode.BRA, "bra_t1", 1, "Belo Horizonte", "Arena MRV",
            "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FACC15"),
            40.0, 77, Formation.F352,
            listOf(
                p("Everson", Position.GK, 76, 34, "BRA"), p("Guilherme Arana", Position.DEF, 77, 27, "BRA"),
                p("Gustavo Scarpa", Position.MID, 77, 30, "BRA"), p("Hulk", Position.FWD, 79, 38, "BRA"),
                p("Rony", Position.FWD, 75, 29, "BRA"),
            ),
        ),
        RealClubSeed(
            "int_bra", "Internacional", "INT", CountryCode.BRA, "bra_t1", 1, "Porto Alegre", "Beira-Rio",
            "#D40026", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"),
            36.0, 77, Formation.F4231,
            listOf(
                p("Sergio Rochet", Position.GK, 77, 31, "URU"), p("Vitao", Position.DEF, 75, 24, "BRA"),
                p("Alan Patrick", Position.MID, 78, 33, "BRA"), p("Wesley", Position.FWD, 75, 25, "BRA"),
                p("Rafael Borre", Position.FWD, 77, 29, "COL"),
            ),
        ),
        RealClubSeed(
            "spfc", "Sao Paulo", "SAO", CountryCode.BRA, "bra_t1", 1, "Sao Paulo", "MorumBIS",
            "#D71920", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.STAR, "#FFFFFF"),
            38.0, 77, Formation.F4231,
            listOf(
                p("Rafael", Position.GK, 75, 35, "BRA"), p("Robert Arboleda", Position.DEF, 76, 33, "ECU"),
                p("Pablo Maia", Position.MID, 76, 22, "BRA"), p("Oscar", Position.MID, 78, 33, "BRA"),
                p("Lucas Moura", Position.FWD, 79, 32, "BRA"), p("Jonathan Calleri", Position.FWD, 77, 31, "ARG"),
            ),
        ),
        RealClubSeed(
            "bah", "Bahia", "BAH", CountryCode.BRA, "bra_t1", 1, "Salvador", "Fonte Nova",
            "#004B9B", "#D71920", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HALF_AND_HALF, CrestSymbol.STAR, "#FFFFFF"),
            30.0, 75, Formation.F433,
            listOf(
                p("Marcos Felipe", Position.GK, 73, 28, "BRA"), p("Everton Ribeiro", Position.MID, 75, 35, "BRA"),
                p("Cauly", Position.MID, 75, 29, "BRA"), p("Jean Lucas", Position.MID, 74, 26, "BRA"),
            ),
        ),
        RealClubSeed(
            "cor_bra", "Corinthians", "COR", CountryCode.BRA, "bra_t1", 1, "Sao Paulo", "Neo Quimica Arena",
            "#FFFFFF", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#D71920"),
            36.0, 77, Formation.F442,
            listOf(
                p("Hugo Souza", Position.GK, 75, 25, "BRA"), p("Rodrigo Garro", Position.MID, 78, 26, "ARG"),
                p("Andre Carrillo", Position.MID, 75, 33, "PER"), p("Memphis Depay", Position.FWD, 80, 30, "NED"),
                p("Yuri Alberto", Position.FWD, 77, 23, "BRA"),
            ),
        ),
        // Brazil Serie B
        RealClubSeed("san_bra", "Santos", "SAN", CountryCode.BRA, "bra_t2", 2, "Santos", "Vila Belmiro", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.CROWN, "#D4AF37"), 18.0, 73, Formation.F4231, listOf(p("Neymar", Position.FWD, 83, 32, "BRA"), p("Guilherme", Position.FWD, 72, 29, "BRA"))),
        RealClubSeed("spt_bra", "Sport Recife", "SPT", CountryCode.BRA, "bra_t2", 2, "Recife", "Ilha do Retiro", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.LION, "#FACC15"), 7.0, 70, Formation.F433, listOf(p("Caique Franca", Position.GK, 69, 29, "BRA"), p("Lucas Lima", Position.MID, 70, 34, "BRA"))),
        RealClubSeed("cea", "Ceara", "CEA", CountryCode.BRA, "bra_t2", 2, "Fortaleza", "Castelao", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 6.0, 69, Formation.F4231, listOf(p("Erick Pulga", Position.FWD, 71, 24, "BRA"), p("Saulo Mineiro", Position.FWD, 69, 27, "BRA"))),
        RealClubSeed("mir", "Mirassol", "MIR", CountryCode.BRA, "bra_t2", 2, "Mirassol", "Maião", "#FACC15", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#16A34A"), 5.5, 69, Formation.F433, listOf(p("Alex Muralha", Position.GK, 68, 35, "BRA"), p("Dellatorre", Position.FWD, 69, 32, "BRA"))),
        RealClubSeed("ctb", "Coritiba", "CFC", CountryCode.BRA, "bra_t2", 2, "Curitiba", "Couto Pereira", "#006437", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HOOPS, CrestSymbol.STAR, "#FFFFFF"), 6.5, 69, Formation.F4231, listOf(p("Pedro Morisco", Position.GK, 69, 20, "BRA"), p("Matheus Frizzo", Position.MID, 70, 26, "BRA"))),
        RealClubSeed("goi", "Goias", "GOI", CountryCode.BRA, "bra_t2", 2, "Goiania", "Serrinha", "#006437", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 5.5, 68, Formation.F442, listOf(p("Tadeu", Position.GK, 71, 32, "BRA"), p("Thiago Galhardo", Position.FWD, 70, 35, "BRA"))),
        // Additional Brasileirao Serie A clubs (8 -> 20)
        RealClubSeed("bra1_for", "Fortaleza", "FOR", CountryCode.BRA, "bra_t1", 1, "Fortaleza", "Castelao", "#1D4ED8", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.LION, "#FFFFFF"), 28.0, 76, Formation.F4231, listOf(p("Joao Ricardo", Position.GK, 75, 36, "BRA"), p("Juan Martin Lucero", Position.FWD, 76, 33, "ARG"))),
        RealClubSeed("bra1_cru", "Cruzeiro", "CRU", CountryCode.BRA, "bra_t1", 1, "Belo Horizonte", "Mineirao", "#0038A8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 32.0, 76, Formation.F4231, listOf(p("Cassio", Position.GK, 75, 37, "BRA"), p("Matheus Pereira", Position.MID, 78, 28, "BRA"), p("Gabriel Barbosa", Position.FWD, 77, 28, "BRA"))),
        RealClubSeed("bra1_gre", "Gremio", "GRE", CountryCode.BRA, "bra_t1", 1, "Porto Alegre", "Arena do Gremio", "#0099D6", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 26.0, 75, Formation.F4231, listOf(p("Agustin Marchesin", Position.GK, 75, 36, "ARG"), p("Martin Braithwaite", Position.FWD, 76, 33, "DEN"))),
        RealClubSeed("bra1_flu", "Fluminense", "FLU", CountryCode.BRA, "bra_t1", 1, "Rio de Janeiro", "Maracana", "#7F1D1D", "#006437", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#FFFFFF"), 28.0, 75, Formation.F4231, listOf(p("Fabio", Position.GK, 75, 44, "BRA"), p("Jhon Arias", Position.FWD, 78, 27, "COL"))),
        RealClubSeed("bra1_vas", "Vasco da Gama", "VAS", CountryCode.BRA, "bra_t1", 1, "Rio de Janeiro", "Sao Januario", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.ANCHOR, "#FFFFFF"), 22.0, 74, Formation.F4231, listOf(p("Leo Jardim", Position.GK, 75, 29, "BRA"), p("Philippe Coutinho", Position.MID, 76, 32, "BRA"), p("Pablo Vegetti", Position.FWD, 75, 36, "ARG"))),
        RealClubSeed("bra1_bra", "RB Bragantino", "RBB", CountryCode.BRA, "bra_t1", 1, "Braganca Paulista", "Nabi Abi Chedid", "#FFFFFF", "#DC2626", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.BOLT, "#DC2626"), 22.0, 74, Formation.F433, listOf(p("Cleiton", Position.GK, 74, 27, "BRA"), p("Eduardo Sasha", Position.FWD, 74, 32, "BRA"))),
        RealClubSeed("bra1_vit", "Vitoria", "VIT", CountryCode.BRA, "bra_t1", 1, "Salvador", "Barradao", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.LION, "#FFFFFF"), 14.0, 72, Formation.F4231, listOf(p("Lucas Arcanjo", Position.GK, 72, 26, "BRA"), p("Matheuzinho", Position.MID, 73, 27, "BRA"))),
        RealClubSeed("bra1_juv", "Juventude", "JUV", CountryCode.BRA, "bra_t1", 1, "Caxias do Sul", "Alfredo Jaconi", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 12.0, 71, Formation.F4231, listOf(p("Gabriel Vasconcelos", Position.GK, 72, 32, "BRA"), p("Lucas Barbosa", Position.FWD, 72, 23, "BRA"))),
        RealClubSeed("bra1_atl", "Atletico Goianiense", "ACG", CountryCode.BRA, "bra_t1", 1, "Goiania", "Antonio Accioly", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.EAGLE, "#FFFFFF"), 11.0, 71, Formation.F4231, listOf(p("Ronaldo", Position.GK, 71, 28, "BRA"), p("Luiz Fernando", Position.FWD, 72, 28, "BRA"))),
        RealClubSeed("bra1_cri", "Criciuma", "CRI", CountryCode.BRA, "bra_t1", 1, "Criciuma", "Heriberto Hulse", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.LION, "#FFFFFF"), 11.0, 71, Formation.F442, listOf(p("Gustavo", Position.GK, 71, 31, "BRA"), p("Yannick Bolasie", Position.FWD, 73, 35, "COD"))),
        RealClubSeed("bra1_cui", "Cuiaba", "CUI", CountryCode.BRA, "bra_t1", 1, "Cuiaba", "Arena Pantanal", "#FACC15", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#16A34A"), 11.0, 71, Formation.F433, listOf(p("Walter", Position.GK, 72, 37, "BRA"), p("Isidro Pitta", Position.FWD, 72, 25, "PAR"))),
        RealClubSeed("bra1_cap", "Athletico Paranaense", "CAP", CountryCode.BRA, "bra_t1", 1, "Curitiba", "Ligga Arena", "#DC2626", "#111111", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.DIAGONAL_SASH, CrestSymbol.FLAME, "#FFFFFF"), 24.0, 74, Formation.F4231, listOf(p("Mycael", Position.GK, 73, 20, "BRA"), p("Pablo", Position.FWD, 73, 32, "BRA"))),
        // Additional Brasileirao Serie B clubs (6 -> 20)
        RealClubSeed("bra2_ame", "America Mineiro", "AME", CountryCode.BRA, "bra_t2", 2, "Belo Horizonte", "Independencia", "#16A34A", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 6.0, 69, Formation.F4231, listOf(p("Elias Martello", Position.GK, 69, 29, "BRA"), p("Juninho", Position.MID, 70, 37, "BRA"))),
        RealClubSeed("bra2_ava", "Avai", "AVA", CountryCode.BRA, "bra_t2", 2, "Florianopolis", "Ressacada", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FFFFFF"), 5.0, 68, Formation.F4231, listOf(p("Cesar Augusto", Position.GK, 68, 32, "BRA"), p("Giovanni", Position.MID, 69, 30, "BRA"))),
        RealClubSeed("bra2_vil", "Vila Nova", "VIL", CountryCode.BRA, "bra_t2", 2, "Goiania", "OBA", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 5.0, 68, Formation.F4231, listOf(p("Denis Junior", Position.GK, 68, 26, "BRA"), p("Alesson", Position.FWD, 69, 25, "BRA"))),
        RealClubSeed("bra2_nov", "Novorizontino", "NOV", CountryCode.BRA, "bra_t2", 2, "Novo Horizonte", "Jorge Ismael", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#111111"), 5.2, 69, Formation.F352, listOf(p("Jordi", Position.GK, 69, 31, "BRA"), p("Neto Pessoa", Position.FWD, 69, 30, "BRA"))),
        RealClubSeed("bra2_ope", "Operario Ferroviario", "OPE", CountryCode.BRA, "bra_t2", 2, "Ponta Grossa", "Germano Kruger", "#111111", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 4.8, 68, Formation.F4231, listOf(p("Rafael Santos", Position.GK, 68, 35, "BRA"), p("Ronaldo", Position.FWD, 68, 28, "BRA"))),
        RealClubSeed("bra2_ama", "Amazonas", "AMA", CountryCode.BRA, "bra_t2", 2, "Manaus", "Arena da Amazonia", "#FACC15", "#111111", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.LION, "#111111"), 4.6, 68, Formation.F4231, listOf(p("Marcão", Position.GK, 68, 31, "BRA"), p("Sassa", Position.FWD, 69, 30, "BRA"))),
        RealClubSeed("bra2_pay", "Paysandu", "PAY", CountryCode.BRA, "bra_t2", 2, "Belem", "Curuzu", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 4.6, 68, Formation.F433, listOf(p("Matheus Nogueira", Position.GK, 68, 27, "BRA"), p("Nicolas", Position.FWD, 69, 35, "BRA"))),
        RealClubSeed("bra2_pon", "Ponte Preta", "PON", CountryCode.BRA, "bra_t2", 2, "Campinas", "Moises Lucarelli", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.STAR, "#111111"), 4.5, 67, Formation.F4231, listOf(p("Pedro Rocha", Position.GK, 67, 26, "BRA"), p("Jeh", Position.FWD, 68, 25, "BRA"))),
        RealClubSeed("bra2_crb", "CRB", "CRB", CountryCode.BRA, "bra_t2", 2, "Maceio", "Rei Pele", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 4.5, 67, Formation.F4231, listOf(p("Matheus Albino", Position.GK, 68, 29, "BRA"), p("Anselmo Ramon", Position.FWD, 69, 36, "BRA"))),
        RealClubSeed("bra2_bot", "Botafogo SP", "BSP", CountryCode.BRA, "bra_t2", 2, "Ribeirao Preto", "Santa Cruz", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.STAR, "#FFFFFF"), 4.2, 67, Formation.F4231, listOf(p("Joao Carlos", Position.GK, 67, 36, "BRA"), p("Alex Sandro", Position.FWD, 67, 29, "BRA"))),
        RealClubSeed("bra2_cha", "Chapecoense", "CHA", CountryCode.BRA, "bra_t2", 2, "Chapeco", "Arena Conda", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 4.2, 67, Formation.F442, listOf(p("Matheus Cavichioli", Position.GK, 67, 38, "BRA"), p("Mario Sergio", Position.FWD, 67, 29, "BRA"))),
        RealClubSeed("bra2_gua", "Guarani", "GUA", CountryCode.BRA, "bra_t2", 2, "Campinas", "Brinco de Ouro", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LAUREL, "#FFFFFF"), 4.2, 67, Formation.F4231, listOf(p("Pegorari", Position.GK, 67, 33, "BRA"), p("Caio Dantas", Position.FWD, 68, 31, "BRA"))),
        RealClubSeed("bra2_itu", "Ituano", "ITU", CountryCode.BRA, "bra_t2", 2, "Itu", "Novelli Junior", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.STAR, "#FFFFFF"), 4.0, 66, Formation.F442, listOf(p("Jefferson Paulino", Position.GK, 67, 33, "BRA"), p("Bruno Xavier", Position.FWD, 67, 28, "BRA"))),
        RealClubSeed("bra2_bru", "Brusque", "BRU", CountryCode.BRA, "bra_t2", 2, "Brusque", "Augusto Bauer", "#FACC15", "#16A34A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#DC2626"), 4.0, 66, Formation.F4231, listOf(p("Matheus Nogueira", Position.GK, 66, 27, "BRA"), p("Diego Mathias", Position.MID, 66, 25, "BRA"))),
    )

    private fun createArgentinaSeeds(): List<RealClubSeed> = listOf(
        RealClubSeed(
            "riv", "River Plate", "RIV", CountryCode.ARG, "arg_t1", 1, "Buenos Aires", "Monumental",
            "#FFFFFF", "#D71920", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.CROWN, "#D71920"),
            45.0, 78, Formation.F433,
            listOf(
                p("Franco Armani", Position.GK, 77, 38, "ARG"), p("German Pezzella", Position.DEF, 77, 33, "ARG"),
                p("Marcos Acuna", Position.DEF, 78, 33, "ARG"), p("Gonzalo Montiel", Position.DEF, 77, 27, "ARG"),
                p("Franco Mastantuono", Position.MID, 76, 17, "ARG"), p("Sebastian Driussi", Position.FWD, 76, 28, "ARG"),
                p("Miguel Borja", Position.FWD, 76, 31, "COL"),
            ),
        ),
        RealClubSeed(
            "boc", "Boca Juniors", "BOC", CountryCode.ARG, "arg_t1", 1, "Buenos Aires", "La Bombonera",
            "#003882", "#F7B500", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.STAR, "#F7B500"),
            42.0, 77, Formation.F4231,
            listOf(
                p("Agustin Marchesin", Position.GK, 76, 36, "ARG"), p("Marcos Rojo", Position.DEF, 75, 34, "ARG"),
                p("Ander Herrera", Position.MID, 77, 35, "ESP"), p("Kevin Zenon", Position.MID, 76, 23, "ARG"),
                p("Carlos Palacios", Position.MID, 75, 24, "CHI"), p("Edinson Cavani", Position.FWD, 77, 37, "URU"),
                p("Miguel Merentiel", Position.FWD, 76, 28, "URU"),
            ),
        ),
        RealClubSeed(
            "rcg", "Racing Club", "RAC", CountryCode.ARG, "arg_t1", 1, "Avellaneda", "El Cilindro",
            "#6CABDD", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#1E3A8A"),
            26.0, 76, Formation.F352,
            listOf(
                p("Gabriel Arias", Position.GK, 76, 37, "CHI"), p("Marco Di Cesare", Position.DEF, 75, 22, "ARG"),
                p("Juan Nardoni", Position.MID, 75, 22, "ARG"), p("Luciano Vietto", Position.FWD, 75, 30, "ARG"),
                p("Adrian Martinez", Position.FWD, 77, 32, "ARG"),
            ),
        ),
        RealClubSeed(
            "vel", "Velez Sarsfield", "VEL", CountryCode.ARG, "arg_t1", 1, "Buenos Aires", "Jose Amalfitani",
            "#FFFFFF", "#00529F", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.CHEVRON, CrestSymbol.STAR, "#00529F"),
            20.0, 75, Formation.F4231,
            listOf(
                p("Tomas Marchiori", Position.GK, 73, 29, "ARG"), p("Valentin Gomez", Position.DEF, 75, 21, "ARG"),
                p("Christian Ordonez", Position.MID, 74, 20, "ARG"), p("Braian Romero", Position.FWD, 74, 33, "ARG"),
            ),
        ),
        RealClubSeed(
            "tal", "Talleres", "TAL", CountryCode.ARG, "arg_t1", 1, "Cordoba", "Mario Alberto Kempes",
            "#0A2240", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"),
            18.0, 74, Formation.F4231,
            listOf(
                p("Guido Herrera", Position.GK, 75, 32, "ARG"), p("Matias Galarza", Position.MID, 73, 22, "PAR"),
                p("Ruben Botta", Position.MID, 74, 34, "ARG"), p("Federico Girotti", Position.FWD, 73, 25, "ARG"),
            ),
        ),
        RealClubSeed(
            "est_arg", "Estudiantes LP", "EST", CountryCode.ARG, "arg_t1", 1, "La Plata", "UNO Estadio",
            "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#D4AF37"),
            20.0, 74, Formation.F4231,
            listOf(
                p("Matias Mansilla", Position.GK, 73, 28, "ARG"), p("Santiago Ascacibar", Position.MID, 76, 27, "ARG"),
                p("Cristian Medina", Position.MID, 75, 22, "ARG"), p("Guido Carrillo", Position.FWD, 74, 33, "ARG"),
            ),
        ),
        RealClubSeed(
            "ind_arg", "Independiente", "IND", CountryCode.ARG, "arg_t1", 1, "Avellaneda", "Libertadores de America",
            "#D71920", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.FLAME, "#FFFFFF"),
            18.0, 74, Formation.F433,
            listOf(
                p("Rodrigo Rey", Position.GK, 75, 33, "ARG"), p("Kevin Lomonaco", Position.DEF, 74, 22, "ARG"),
                p("Felipe Loyola", Position.MID, 74, 24, "CHI"), p("Gabriel Avalos", Position.FWD, 74, 34, "PAR"),
            ),
        ),
        RealClubSeed(
            "slo_arg", "San Lorenzo", "SLO", CountryCode.ARG, "arg_t1", 1, "Buenos Aires", "Nuevo Gasometro",
            "#00338D", "#D71920", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"),
            15.0, 73, Formation.F4231,
            listOf(
                p("Orlando Gill", Position.GK, 70, 24, "PAR"), p("Gaston Campi", Position.DEF, 72, 33, "ARG"),
                p("Iker Muniain", Position.MID, 74, 31, "ESP"), p("Alexis Cuello", Position.FWD, 71, 24, "ARG"),
            ),
        ),
        // Primera Nacional
        RealClubSeed("smt", "San Martin Tucuman", "SMT", CountryCode.ARG, "arg_t2", 2, "Tucuman", "La Ciudadela", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 3.2, 67, Formation.F442, listOf(p("Dario Sand", Position.GK, 68, 36, "ARG"), p("Junior Arias", Position.FWD, 68, 31, "URU"))),
        RealClubSeed("ald", "Aldosivi", "ALD", CountryCode.ARG, "arg_t2", 2, "Mar del Plata", "Jose Maria Minella", "#16A34A", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.ANCHOR, "#FFFFFF"), 3.0, 67, Formation.F433, listOf(p("Jorge Carranza", Position.GK, 67, 43, "ARG"), p("Elias Torres", Position.FWD, 68, 23, "ARG"))),
        RealClubSeed("col_arg", "Colon", "COL", CountryCode.ARG, "arg_t2", 2, "Santa Fe", "Brigadier Lopez", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.STAR, "#FFFFFF"), 3.5, 67, Formation.F442, listOf(p("Marcos Diaz", Position.GK, 69, 38, "ARG"), p("Emmanuel Gigliotti", Position.FWD, 68, 37, "ARG"))),
        RealClubSeed("fer", "Ferro Carril Oeste", "FCO", CountryCode.ARG, "arg_t2", 2, "Buenos Aires", "Arquitecto Etcheverri", "#16A34A", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 2.4, 66, Formation.F4231, listOf(p("Fernando Monetti", Position.GK, 67, 35, "ARG"), p("Mateo Levato", Position.FWD, 66, 28, "ARG"))),
        RealClubSeed("qui_arg", "Quilmes", "QUI", CountryCode.ARG, "arg_t2", 2, "Quilmes", "Centenario", "#FFFFFF", "#1E3A8A", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#1E3A8A"), 2.5, 66, Formation.F442, listOf(p("Esteban Glellel", Position.GK, 66, 25, "SYR"), p("Emanuel Herrera", Position.FWD, 67, 37, "ARG"))),
        RealClubSeed("gim_j", "Gimnasia Jujuy", "GYJ", CountryCode.ARG, "arg_t2", 2, "San Salvador", "23 de Agosto", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 2.2, 65, Formation.F442, listOf(p("Milton Alvarez", Position.GK, 66, 35, "ARG"), p("Cristian Menendez", Position.FWD, 66, 36, "ARG"))),
        // Additional Liga Profesional clubs (8 -> 20)
        RealClubSeed("arg1_hur", "Huracan", "HUR", CountryCode.ARG, "arg_t1", 1, "Buenos Aires", "Tomas Adolfo Duco", "#FFFFFF", "#DC2626", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.FLAME, "#DC2626"), 14.0, 73, Formation.F4231, listOf(p("Hernan Galindez", Position.GK, 74, 37, "ECU"), p("Rodrigo Echeverria", Position.MID, 74, 29, "CHI"))),
        RealClubSeed("arg1_ros", "Rosario Central", "ROS", CountryCode.ARG, "arg_t1", 1, "Rosario", "Gigante de Arroyito", "#0038A8", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FACC15"), 15.0, 73, Formation.F4231, listOf(p("Jorge Broun", Position.GK, 73, 38, "ARG"), p("Ignacio Malcorra", Position.MID, 74, 37, "ARG"))),
        RealClubSeed("arg1_new", "Newell's Old Boys", "NOB", CountryCode.ARG, "arg_t1", 1, "Rosario", "Marcelo Bielsa", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.STAR, "#FFFFFF"), 13.0, 72, Formation.F4231, listOf(p("Keylor Navas", Position.GK, 78, 37, "CRC"), p("Ever Banega", Position.MID, 74, 36, "ARG"))),
        RealClubSeed("arg1_bel", "Belgrano", "BEL", CountryCode.ARG, "arg_t1", 1, "Cordoba", "Julio Cesar Villagra", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 12.0, 72, Formation.F4231, listOf(p("Juan Espinola", Position.GK, 72, 30, "PAR"), p("Franco Jara", Position.FWD, 72, 36, "ARG"))),
        RealClubSeed("arg1_lan", "Lanus", "LAN", CountryCode.ARG, "arg_t1", 1, "Lanus", "La Fortaleza", "#7F1D1D", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 13.0, 72, Formation.F4231, listOf(p("Nahuel Losada", Position.GK, 73, 31, "ARG"), p("Marcelino Moreno", Position.MID, 74, 30, "ARG"))),
        RealClubSeed("arg1_arg", "Argentinos Juniors", "AAA", CountryCode.ARG, "arg_t1", 1, "Buenos Aires", "Diego Armando Maradona", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.DIAGONAL_SASH, CrestSymbol.STAR, "#FFFFFF"), 14.0, 73, Formation.F433, listOf(p("Diego Rodriguez", Position.GK, 72, 35, "ARG"), p("Alan Lescano", Position.MID, 73, 23, "ARG"))),
        RealClubSeed("arg1_god", "Godoy Cruz", "GOD", CountryCode.ARG, "arg_t1", 1, "Mendoza", "Malvinas Argentinas", "#1D4ED8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 12.0, 72, Formation.F4231, listOf(p("Franco Petroli", Position.GK, 72, 26, "ARG"), p("Salomon Rodriguez", Position.FWD, 72, 24, "URU"))),
        RealClubSeed("arg1_uni", "Union Santa Fe", "UNI", CountryCode.ARG, "arg_t1", 1, "Santa Fe", "15 de Abril", "#DC2626", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 11.0, 71, Formation.F352, listOf(p("Thiago Cardozo", Position.GK, 71, 28, "URU"), p("Adrian Balboa", Position.FWD, 71, 30, "URU"))),
        RealClubSeed("arg1_def", "Defensa y Justicia", "DYJ", CountryCode.ARG, "arg_t1", 1, "Florencio Varela", "Norberto Tomaghello", "#16A34A", "#FACC15", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#FACC15"), 12.0, 71, Formation.F4231, listOf(p("Enrique Bologna", Position.GK, 71, 42, "ARG"), p("Aaron Molinas", Position.MID, 72, 24, "ARG"))),
        RealClubSeed("arg1_atp", "Atletico Tucuman", "ATU", CountryCode.ARG, "arg_t1", 1, "Tucuman", "Jose Fierro", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 10.5, 71, Formation.F442, listOf(p("Tomas Durso", Position.GK, 70, 25, "ARG"), p("Mateo Coronel", Position.FWD, 71, 26, "ARG"))),
        RealClubSeed("arg1_pla", "Platense", "PLA", CountryCode.ARG, "arg_t1", 1, "Vicente Lopez", "Ciudad de Vicente Lopez", "#78350F", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.STAR, "#FFFFFF"), 10.5, 71, Formation.F442, listOf(p("Juan Pablo Cozzani", Position.GK, 72, 26, "ARG"), p("Mateo Pellegrino", Position.FWD, 72, 23, "ARG"))),
        RealClubSeed("arg1_gim", "Gimnasia LP", "GELP", CountryCode.ARG, "arg_t1", 1, "La Plata", "Juan Carmelo Zerillo", "#FFFFFF", "#0038A8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.CROWN, "#0038A8"), 11.0, 71, Formation.F4231, listOf(p("Marcos Ledesma", Position.GK, 71, 28, "ARG"), p("Rodrigo Castillo", Position.FWD, 71, 25, "ARG"))),
        // Additional Primera Nacional clubs (6 -> 20)
        RealClubSeed("arg2_all", "All Boys", "ALL", CountryCode.ARG, "arg_t2", 2, "Buenos Aires", "Islas Malvinas", "#FFFFFF", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HOOPS, CrestSymbol.STAR, "#111111"), 2.2, 65, Formation.F442, listOf(p("Lisandro Mitre", Position.GK, 66, 28, "ARG"), p("Franco Toloza", Position.FWD, 66, 30, "ARG"))),
        RealClubSeed("arg2_chi", "Nueva Chicago", "NCH", CountryCode.ARG, "arg_t2", 2, "Buenos Aires", "Republica de Mataderos", "#16A34A", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LION, "#FFFFFF"), 2.4, 66, Formation.F4231, listOf(p("Facundo Ferrero", Position.GK, 67, 29, "ARG"), p("Facundo Castro", Position.FWD, 67, 28, "ARG"))),
        RealClubSeed("arg2_atl", "Atlanta", "ATL", CountryCode.ARG, "arg_t2", 2, "Buenos Aires", "Don Leon Kolbowski", "#FACC15", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#1D4ED8"), 2.2, 65, Formation.F442, listOf(p("Alejandro Sanchez", Position.GK, 66, 38, "ARG"), p("Ivo Costantino", Position.FWD, 66, 25, "ARG"))),
        RealClubSeed("arg2_tem", "Temperley", "TEM", CountryCode.ARG, "arg_t2", 2, "Temperley", "Alfredo Beranger", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.STAR, "#FFFFFF"), 2.3, 66, Formation.F4231, listOf(p("Francisco Rago", Position.GK, 66, 35, "ARG"), p("Luis Lopez", Position.FWD, 66, 37, "ARG"))),
        RealClubSeed("arg2_cha", "Chacarita Juniors", "CHJ", CountryCode.ARG, "arg_t2", 2, "San Martin", "Chacarita Stadium", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 2.4, 66, Formation.F4231, listOf(p("Federico Losas", Position.GK, 66, 22, "ARG"), p("Rodrigo Salinas", Position.FWD, 66, 38, "ARG"))),
        RealClubSeed("arg2_san", "San Telmo", "STE", CountryCode.ARG, "arg_t2", 2, "Dock Sud", "Osvaldo Baletto", "#38BDF8", "#1D4ED8", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.ANCHOR, "#FFFFFF"), 2.2, 66, Formation.F433, listOf(p("Brian Bustos", Position.GK, 66, 28, "ARG"), p("Franco Tisera", Position.FWD, 66, 35, "ARG"))),
        RealClubSeed("arg2_gimm", "Gimnasia Mendoza", "GYM", CountryCode.ARG, "arg_t2", 2, "Mendoza", "Victor Legrotaglie", "#FFFFFF", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.CROWN, "#111111"), 2.5, 66, Formation.F4231, listOf(p("Matias Tagliamonte", Position.GK, 67, 26, "ARG"), p("Luis Silba", Position.FWD, 66, 34, "ARG"))),
        RealClubSeed("arg2_est", "Estudiantes RC", "ERC", CountryCode.ARG, "arg_t2", 2, "Rio Cuarto", "Antonio Candini", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LION, "#FFFFFF"), 2.3, 65, Formation.F442, listOf(p("Williams Barlasina", Position.GK, 66, 26, "ARG"), p("Francisco Galvan", Position.FWD, 65, 25, "ARG"))),
        RealClubSeed("arg2_alm", "Almirante Brown", "ALM", CountryCode.ARG, "arg_t2", 2, "Isidro Casanova", "Fragata Sarmiento", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.ANCHOR, "#FFFFFF"), 2.1, 65, Formation.F442, listOf(p("Ramiro Martinez", Position.GK, 65, 33, "ARG"), p("Brian Fernandez", Position.FWD, 66, 30, "ARG"))),
        RealClubSeed("arg2_dep", "Deportivo Moron", "DMO", CountryCode.ARG, "arg_t2", 2, "Moron", "Nuevo Francisco Urbano", "#FFFFFF", "#DC2626", defaultSpec(CrestShape.ROUNDEL, CrestPattern.DIAGONAL_SASH, CrestSymbol.EAGLE, "#DC2626"), 2.1, 65, Formation.F4231, listOf(p("Juan Martin Rojas", Position.GK, 65, 25, "ARG"), p("Gonzalo Berterame", Position.FWD, 65, 28, "ARG"))),
        RealClubSeed("arg2_rac", "Racing Cordoba", "RCC", CountryCode.ARG, "arg_t2", 2, "Cordoba", "Miguel Sancho", "#38BDF8", "#FFFFFF", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 2.2, 65, Formation.F442, listOf(p("Joaquin Mattalia", Position.GK, 65, 32, "ARG"), p("Bruno Nasta", Position.FWD, 67, 31, "ARG"))),
        RealClubSeed("arg2_mit", "Mitre Santiago", "MIT", CountryCode.ARG, "arg_t2", 2, "Santiago del Estero", "Doctores Castiglione", "#FACC15", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 2.2, 65, Formation.F4231, listOf(p("Luciano Jachfe", Position.GK, 65, 23, "ARG"), p("Favrio Cabral", Position.MID, 65, 23, "ARG"))),
        RealClubSeed("arg2_depma", "Deportivo Madryn", "DMA", CountryCode.ARG, "arg_t2", 2, "Puerto Madryn", "Abel Sastre", "#FACC15", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.ANCHOR, "#FFFFFF"), 2.3, 66, Formation.F442, listOf(p("Yair Bonnin", Position.GK, 66, 34, "ARG"), p("Nicolas Ferreyra", Position.FWD, 66, 31, "ARG"))),
        RealClubSeed("arg2_patron", "Patronato", "PAT", CountryCode.ARG, "arg_t2", 2, "Parana", "Presbitero Grella", "#DC2626", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#FFFFFF"), 2.3, 66, Formation.F4231, listOf(p("Julio Salvá", Position.GK, 66, 37, "ARG"), p("Alan Bonansea", Position.FWD, 66, 28, "ARG"))),
    )

    private fun createUsaSeeds(): List<RealClubSeed> = listOf(
        RealClubSeed(
            "mia", "Inter Miami", "MIA", CountryCode.USA, "usa_t1", 1, "Miami", "Chase Stadium",
            "#F7B5CD", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.EAGLE, "#111111"),
            45.0, 77, Formation.F433,
            listOf(
                p("Drake Callender", Position.GK, 74, 27, "USA"), p("Jordi Alba", Position.DEF, 79, 35, "ESP"),
                p("Sergio Busquets", Position.MID, 80, 36, "ESP"), p("Federico Redondo", Position.MID, 74, 21, "ARG"),
                p("Lionel Messi", Position.FWD, 88, 37, "ARG"), p("Luis Suarez", Position.FWD, 80, 37, "URU"),
            ),
        ),
        RealClubSeed(
            "lafc", "LAFC", "LAFC", CountryCode.USA, "usa_t1", 1, "Los Angeles", "BMO Stadium",
            "#111111", "#C39E6D", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.EAGLE, "#C39E6D"),
            32.0, 76, Formation.F433,
            listOf(
                p("Hugo Lloris", Position.GK, 78, 37, "FRA"), p("Aaron Long", Position.DEF, 73, 32, "USA"),
                p("Timothy Tillman", Position.MID, 73, 25, "USA"), p("Denis Bouanga", Position.FWD, 79, 30, "GAB"),
                p("Olivier Giroud", Position.FWD, 77, 38, "FRA"),
            ),
        ),
        RealClubSeed(
            "lag", "LA Galaxy", "LAG", CountryCode.USA, "usa_t1", 1, "Los Angeles", "Dignity Health Sports Park",
            "#FFFFFF", "#00245D", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.STAR, "#FFD200"),
            30.0, 76, Formation.F4231,
            listOf(
                p("John McCarthy", Position.GK, 73, 32, "USA"), p("Maya Yoshida", Position.DEF, 74, 36, "JPN"),
                p("Riqui Puig", Position.MID, 80, 25, "ESP"), p("Marco Reus", Position.MID, 77, 35, "GER"),
                p("Gabriel Pec", Position.FWD, 76, 23, "BRA"), p("Joseph Paintsil", Position.FWD, 76, 26, "GHA"),
            ),
        ),
        RealClubSeed(
            "clb", "Columbus Crew", "CLB", CountryCode.USA, "usa_t1", 1, "Columbus", "Lower.com Field",
            "#FEDD00", "#111111", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.BOLT, "#111111"),
            26.0, 75, Formation.F352,
            listOf(
                p("Patrick Schulte", Position.GK, 74, 23, "USA"), p("Steven Moreira", Position.DEF, 74, 30, "CPV"),
                p("Darlington Nagbe", Position.MID, 75, 34, "USA"), p("Diego Rossi", Position.FWD, 77, 26, "URU"),
            ),
        ),
        RealClubSeed(
            "cin", "FC Cincinnati", "CIN", CountryCode.USA, "usa_t1", 1, "Cincinnati", "TQL Stadium",
            "#003087", "#FE5000", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.LION, "#FE5000"),
            28.0, 75, Formation.F352,
            listOf(
                p("Roman Celentano", Position.GK, 74, 24, "USA"), p("Matt Miazga", Position.DEF, 75, 29, "USA"),
                p("Miles Robinson", Position.DEF, 75, 27, "USA"), p("Evander", Position.MID, 77, 26, "BRA"),
                p("Kevin Denkey", Position.FWD, 77, 23, "TOG"),
            ),
        ),
        RealClubSeed(
            "sea", "Seattle Sounders", "SEA", CountryCode.USA, "usa_t1", 1, "Seattle", "Lumen Field",
            "#5D9741", "#005595", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"),
            22.0, 74, Formation.F4231,
            listOf(
                p("Stefan Frei", Position.GK, 74, 38, "SUI"), p("Yeimar Gomez", Position.DEF, 74, 32, "COL"),
                p("Albert Rusnak", Position.MID, 75, 30, "SVK"), p("Jordan Morris", Position.FWD, 74, 30, "USA"),
            ),
        ),
        RealClubSeed(
            "atl", "Atlanta United", "ATL", CountryCode.USA, "usa_t1", 1, "Atlanta", "Mercedes-Benz Stadium",
            "#80000A", "#A19060", defaultSpec(CrestShape.ROUNDEL, CrestPattern.VERTICAL_STRIPES, CrestSymbol.STAR, "#A19060"),
            28.0, 74, Formation.F4231,
            listOf(
                p("Brad Guzan", Position.GK, 71, 40, "USA"), p("Aleksei Miranchuk", Position.MID, 76, 29, "RUS"),
                p("Miguel Almiron", Position.MID, 77, 30, "PAR"), p("Emmanuel Latte Lath", Position.FWD, 76, 25, "CIV"),
            ),
        ),
        RealClubSeed(
            "nyrb", "NY Red Bulls", "RBNY", CountryCode.USA, "usa_t1", 1, "Harrison", "Sports Illustrated Stadium",
            "#ED1E36", "#FFFFFF", defaultSpec(CrestShape.ROUNDEL, CrestPattern.HALF_AND_HALF, CrestSymbol.BOLT, "#FFFFFF"),
            20.0, 73, Formation.F442,
            listOf(
                p("Carlos Coronel", Position.GK, 73, 27, "PAR"), p("Emil Forsberg", Position.MID, 77, 33, "SWE"),
                p("Lewis Morgan", Position.FWD, 74, 28, "SCO"), p("Eric Maxim Choupo-Moting", Position.FWD, 75, 35, "CMR"),
            ),
        ),
        // Additional MLS clubs (8 -> 20)
        RealClubSeed("usa1_nycf", "New York City FC", "NYC", CountryCode.USA, "usa_t1", 1, "New York", "Yankee Stadium", "#6CABDD", "#1C2C5B", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 24.0, 74, Formation.F4231, listOf(p("Matt Freese", Position.GK, 73, 26, "USA"), p("Santiago Rodriguez", Position.MID, 75, 24, "URU"), p("Alonso Martinez", Position.FWD, 74, 26, "CRC"))),
        RealClubSeed("usa1_orl", "Orlando City", "ORL", CountryCode.USA, "usa_t1", 1, "Orlando", "Inter&Co Stadium", "#633492", "#FDE100", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.LION, "#FDE100"), 22.0, 74, Formation.F4231, listOf(p("Pedro Gallese", Position.GK, 75, 34, "PER"), p("Luis Muriel", Position.FWD, 75, 33, "COL"))),
        RealClubSeed("usa1_cha", "Charlotte FC", "CLT", CountryCode.USA, "usa_t1", 1, "Charlotte", "Bank of America Stadium", "#1A85C8", "#111111", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.CROWN, "#FFFFFF"), 22.0, 73, Formation.F4231, listOf(p("Kristijan Kahlina", Position.GK, 75, 32, "CRO"), p("Wilfried Zaha", Position.FWD, 77, 32, "CIV"))),
        RealClubSeed("usa1_phi", "Philadelphia Union", "PHI", CountryCode.USA, "usa_t1", 1, "Chester", "Subaru Park", "#002D55", "#B3995D", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.SOLID, CrestSymbol.STAR, "#B3995D"), 18.0, 73, Formation.F442, listOf(p("Andre Blake", Position.GK, 76, 34, "JAM"), p("Daniel Gazdag", Position.MID, 75, 28, "HUN"))),
        RealClubSeed("usa1_hou", "Houston Dynamo", "HOU", CountryCode.USA, "usa_t1", 1, "Houston", "Shell Energy Stadium", "#FF6B00", "#111111", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.BOLT, "#FFFFFF"), 18.0, 73, Formation.F4231, listOf(p("Steve Clark", Position.GK, 72, 38, "USA"), p("Ezequiel Ponce", Position.FWD, 74, 27, "ARG"))),
        RealClubSeed("usa1_min", "Minnesota United", "MIN", CountryCode.USA, "usa_t1", 1, "Saint Paul", "Allianz Field", "#8CD2F4", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.DIAGONAL_SASH, CrestSymbol.EAGLE, "#8CD2F4"), 18.0, 73, Formation.F352, listOf(p("Dayne St. Clair", Position.GK, 73, 27, "CAN"), p("Kelvin Yeboah", Position.FWD, 74, 24, "ITA"))),
        RealClubSeed("usa1_rsl", "Real Salt Lake", "RSL", CountryCode.USA, "usa_t1", 1, "Sandy", "America First Field", "#B30838", "#013A81", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HALF_AND_HALF, CrestSymbol.CROWN, "#FACC15"), 18.0, 73, Formation.F4231, listOf(p("Rafael Cabral", Position.GK, 72, 34, "BRA"), p("Diego Luna", Position.MID, 74, 21, "USA"))),
        RealClubSeed("usa1_por", "Portland Timbers", "POR", CountryCode.USA, "usa_t1", 1, "Portland", "Providence Park", "#00482B", "#D69A00", defaultSpec(CrestShape.ROUNDEL, CrestPattern.SOLID, CrestSymbol.LAUREL, "#D69A00"), 19.0, 73, Formation.F4231, listOf(p("Maxime Crepeau", Position.GK, 73, 30, "CAN"), p("Felipe Mora", Position.FWD, 74, 31, "CHI"))),
        RealClubSeed("usa1_van", "Vancouver Whitecaps", "VAN", CountryCode.USA, "usa_t1", 1, "Vancouver", "BC Place", "#00245E", "#9DC2EA", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.STAR, "#FFFFFF"), 17.0, 73, Formation.F352, listOf(p("Yohei Takaoka", Position.GK, 72, 28, "JPN"), p("Ryan Gauld", Position.MID, 76, 28, "SCO"), p("Brian White", Position.FWD, 74, 28, "USA"))),
        RealClubSeed("usa1_stl", "St. Louis City SC", "STL", CountryCode.USA, "usa_t1", 1, "St. Louis", "Energizer Park", "#D21242", "#0F1E3C", defaultSpec(CrestShape.MODERN_CREST, CrestPattern.SOLID, CrestSymbol.TOWER, "#FFFFFF"), 18.0, 72, Formation.F4231, listOf(p("Roman Burki", Position.GK, 77, 34, "SUI"), p("Joao Klauss", Position.FWD, 73, 27, "BRA"))),
        RealClubSeed("usa1_dal", "FC Dallas", "DAL", CountryCode.USA, "usa_t1", 1, "Frisco", "Toyota Stadium", "#E81F3E", "#001F5B", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.HOOPS, CrestSymbol.STAR, "#FFFFFF"), 18.0, 72, Formation.F352, listOf(p("Maarten Paes", Position.GK, 73, 26, "IDN"), p("Petar Musa", Position.FWD, 74, 26, "CRO"))),
        RealClubSeed("usa1_aus", "Austin FC", "ATX", CountryCode.USA, "usa_t1", 1, "Austin", "Q2 Stadium", "#00B140", "#111111", defaultSpec(CrestShape.CLASSIC_SHIELD, CrestPattern.VERTICAL_STRIPES, CrestSymbol.LAUREL, "#FFFFFF"), 18.0, 72, Formation.F4231, listOf(p("Brad Stuver", Position.GK, 73, 33, "USA"), p("Brandon Vazquez", Position.FWD, 74, 26, "USA"))),
    )
}
