package com.example.gditroll

enum class Effect(val label: String, val soundRes: Int) {
    RGB("RGB Wash", R.raw.rgb_invert),
    SPIRAL("GDI Spiral", R.raw.gdi_spiral),
    GLITCH("GDI Glitch", R.raw.gdi_glitch),
    WAVE("GDI Wave", R.raw.gdi_wave),
    CHAOS("GDI Chaos", R.raw.gdi_chaos),
    TUNNEL("Túnel", R.raw.gdi_spiral),
    MELT("Derretimento", R.raw.gdi_wave),
    PLASMA("Plasma", R.raw.rgb_invert),
    MOIRE("Moiré", R.raw.gdi_spiral),
    BLOCKS("Blocos corrompidos", R.raw.gdi_glitch),
    ICONS("Rastro de erros", R.raw.gdi_chaos),
    HYPNO("Hipnose", R.raw.gdi_spiral),
    MIX("GDI Total", R.raw.gdi_chaos),
    SCR_MELT("Tela derretendo", R.raw.gdi_wave),
    SCR_WAVE("Tela ondulando", R.raw.gdi_wave),
    SCR_TUNNEL("Túnel de tela", R.raw.gdi_spiral),
    SCR_GLITCH("Glitch RGB na tela", R.raw.gdi_glitch),
    SCR_TOTAL("GDI Total (tela)", R.raw.gdi_chaos),
    MEMZ_MATRIX("MEMZ: Matrix", R.raw.gdi_glitch),
    MEMZ_PIPES("MEMZ: Tubos 3D", R.raw.gdi_wave),
    MEMZ_EYES("MEMZ: Olhos", R.raw.gdi_chaos),
    SCR_TILES("MEMZ: Tiles", R.raw.gdi_glitch),
    SCR_MIRROR("MEMZ: Caleidoscópio", R.raw.gdi_spiral),
    SCR_MEMZ_TOTAL("MEMZ Total", R.raw.gdi_chaos),
    SCR_MULTIPLY("Tela se multiplicando", R.raw.gdi_chaos),
    SCR_SPIN("Tela girando", R.raw.gdi_spiral);

    /** Efeitos SCR_* distorcem a imagem da própria tela e precisam da gravação de tela. */
    fun needsCapture() = name.startsWith("SCR_")


    companion object {
        fun from(index: Int) = values()[index.coerceIn(0, values().size - 1)]
    }
}

enum class Intensity(val label: String, val alpha: Float, val speed: Float) {
    SOFT("Suave", 0.45f, 0.7f),
    NORMAL("Normal", 0.75f, 1f),
    STRONG("Forte", 1f, 1.4f),
    EXTREME("Extremo", 1f, 1.8f);

    companion object {
        fun from(index: Int) = values()[index.coerceIn(0, values().size - 1)]
    }
}
