package com.example.data.badges

import com.example.model.Club
import com.example.model.ClubCrestSpec
import com.example.model.CrestPattern
import com.example.model.CrestShape
import com.example.model.CrestSymbol

/**
 * Custom AI-generated SVG vector shapes, heraldic symbols, and SVG markup builder
 * for authentic real-world club crests across all tiers.
 */
object ClubSvgBadges {

    fun outerShapePathData(shape: CrestShape): String = when (shape) {
        CrestShape.CLASSIC_SHIELD ->
            "M 50 5 L 89 18 L 85 57 C 81 78 65 89 50 96 C 35 89 19 78 15 57 L 11 18 Z"
        CrestShape.MODERN_CREST ->
            "M 50 4 L 91 22 L 82 68 C 75 84 61 92 50 96 C 39 92 25 84 18 68 L 9 22 Z"
        CrestShape.ROUNDEL ->
            "M 50 5 A 45 45 0 1 1 49.9 5 Z"
        CrestShape.ITALIAN_OVAL ->
            "M 50 5 C 74 5 87 24 87 50 C 87 76 74 95 50 95 C 26 95 13 76 13 50 C 13 24 26 5 50 5 Z"
        CrestShape.SPANISH_CROWN ->
            "M 24 14 L 38 22 L 50 7 L 62 22 L 76 14 L 84 30 L 82 64 C 77 82 63 91 50 96 C 37 91 23 82 18 64 L 16 30 Z"
        CrestShape.DIAMOND ->
            "M 50 4 L 94 50 L 50 96 L 6 50 Z"
    }

    fun symbolPathData(symbol: CrestSymbol): String? = when (symbol) {
        CrestSymbol.NONE -> null
        CrestSymbol.LION ->
            "M 42 28 L 50 22 L 58 28 L 62 36 L 57 42 L 61 52 L 54 62 L 50 56 L 46 62 L 39 52 L 43 42 L 38 36 Z"
        CrestSymbol.CROWN ->
            "M 26 54 L 30 32 L 42 42 L 50 24 L 58 42 L 70 32 L 74 54 Z M 26 58 L 74 58 L 72 64 L 28 64 Z"
        CrestSymbol.EAGLE ->
            "M 50 24 L 56 32 L 78 28 L 66 44 L 74 56 L 56 52 L 50 66 L 44 52 L 26 56 L 34 44 L 22 28 L 44 32 Z"
        CrestSymbol.STAR ->
            "M 50 22 L 57 40 L 76 40 L 61 51 L 67 69 L 50 58 L 33 69 L 39 51 L 24 40 L 43 40 Z"
        CrestSymbol.CANNON ->
            "M 26 52 L 66 36 L 72 44 L 34 60 Z M 40 58 A 8 8 0 1 1 39.9 58 Z M 66 38 L 76 34 L 78 38 L 68 42 Z"
        CrestSymbol.ANCHOR ->
            "M 47 24 L 53 24 L 53 60 C 62 59 68 53 69 46 L 74 48 C 72 60 62 67 50 68 C 38 67 28 60 26 48 L 31 46 C 32 53 38 59 47 60 Z M 36 35 L 64 35 L 64 40 L 36 40 Z"
        CrestSymbol.BOLT ->
            "M 56 20 L 32 50 L 48 50 L 42 74 L 68 42 L 52 42 Z"
        CrestSymbol.TOWER ->
            "M 34 26 L 41 26 L 41 32 L 47 32 L 47 26 L 53 26 L 53 32 L 59 32 L 59 26 L 66 26 L 66 64 L 34 64 Z"
        CrestSymbol.CRESCENT ->
            "M 58 24 C 41 24 28 36 28 50 C 28 64 41 76 58 76 C 47 72 41 62 41 50 C 41 38 47 28 58 24 Z M 63 40 L 67 48 L 75 49 L 69 54 L 71 62 L 63 58 L 56 62 L 58 54 L 52 49 L 60 48 Z"
        CrestSymbol.LAUREL ->
            "M 32 62 C 24 50 26 34 38 26 C 33 38 35 50 44 58 Z M 68 62 C 76 50 74 34 62 26 C 67 38 65 50 56 58 Z M 50 24 L 54 34 L 50 40 L 46 34 Z"
        CrestSymbol.FLAME ->
            "M 50 20 C 60 32 68 42 64 56 C 61 64 55 68 50 68 C 44 68 37 62 36 52 C 35 42 44 34 50 20 Z M 50 40 C 46 48 46 56 50 60 C 54 56 55 48 50 40 Z"
    }

    /**
     * Generates standalone exportable SVG XML markup for any club badge.
     */
    fun toSvgMarkup(club: Club): String {
        club.crestSpec.customSvg?.let { return it }
        val outer = outerShapePathData(club.crestSpec.shape)
        val resolvedSymbol = if (club.crestSpec.symbol == CrestSymbol.NONE) CrestSymbol.STAR else club.crestSpec.symbol
        val symbol = symbolPathData(resolvedSymbol)
        val symbolTag = if (symbol != null) {
            """<path d="$symbol" fill="${club.crestSpec.accentHex}" fill-opacity="0.92" stroke="#070B12" stroke-width="1.8"/>"""
        } else {
            ""
        }
        return """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
              <path d="$outer" fill="${club.primaryColorHex}" stroke="${club.secondaryColorHex}" stroke-width="5"/>
              $symbolTag
            </svg>
        """.trimIndent()
    }

    fun defaultSpec(
        shape: CrestShape,
        pattern: CrestPattern,
        symbol: CrestSymbol,
        accentHex: String = "#F8FAFC",
        customSvg: String? = null,
    ) = ClubCrestSpec(
        shape = shape,
        pattern = pattern,
        symbol = symbol,
        accentHex = accentHex,
        customSvg = customSvg,
    )
}
