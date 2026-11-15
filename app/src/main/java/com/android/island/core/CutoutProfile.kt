package com.android.island.core

/**
 * Bentuk layar yang dipilih pengguna. Tiap profil menentukan ukuran capsule iPhone-style
 * (lebar x tinggi dalam dp) agar pas menutupi lubang kamera / notch.
 * AUTO mengikuti deteksi cutout perangkat, CUSTOM memakai nilai slider.
 */
enum class CutoutProfile(val label: String, val width: Int, val height: Int) {
    AUTO("Otomatis", 130, 32),
    IPHONE("iPhone", 130, 32),
    PUNCH_HOLE("Punch hole", 110, 34),
    WATERDROP("Waterdrop", 124, 36),
    WIDE_NOTCH("Notch lebar", 176, 34),
    CUSTOM("Kustom", 130, 32);

    companion object {
        fun fromName(name: String?): CutoutProfile =
            values().firstOrNull { it.name == name } ?: CutoutProfile.AUTO

        /** Ukuran (lebar, tinggi) dalam dp yang dipakai untuk profil ini. */
        fun resolve(
            profile: CutoutProfile,
            detected: CutoutType,
            customWidth: Int,
            customHeight: Int
        ): Pair<Int, Int> = when (profile) {
            CutoutProfile.CUSTOM -> customWidth to customHeight
            CutoutProfile.AUTO -> when (detected) {
                CutoutType.NOTCH -> CutoutProfile.WIDE_NOTCH.width to CutoutProfile.WIDE_NOTCH.height
                CutoutType.WATERDROP -> CutoutProfile.WATERDROP.width to CutoutProfile.WATERDROP.height
                CutoutType.PUNCH_HOLE -> CutoutProfile.PUNCH_HOLE.width to CutoutProfile.PUNCH_HOLE.height
                else -> CutoutProfile.IPHONE.width to CutoutProfile.IPHONE.height
            }
            else -> profile.width to profile.height
        }

        /**
         * Size for the chosen profile, widened if needed so the capsule always covers the REAL
         * cutout reported by the panel (+8 dp margin each side). CUSTOM stays fully manual.
         */
        fun resolve(
            profile: CutoutProfile,
            cutout: CutoutInfo,
            density: Float,
            customWidth: Int,
            customHeight: Int
        ): Pair<Int, Int> {
            val base = resolve(profile, cutout.type, customWidth, customHeight)
            if (profile == CUSTOM || cutout.type == CutoutType.NONE || cutout.width <= 0 || density <= 0f) return base
            val needW = (cutout.width / density).toInt() + 16
            return maxOf(base.first, needW) to base.second
        }

        /**
         * Horizontal shift (dp) that moves the capsule onto an off-centre punch hole.
         * Zero for centred notches / iPhone-style islands, for no cutout and for CUSTOM.
         */
        fun autoShiftDp(
            profile: CutoutProfile,
            cutout: CutoutInfo,
            screenWidthDp: Int
        ): Float {
            if (profile == CUSTOM || cutout.type == CutoutType.NONE || cutout.width <= 0) return 0f
            val shift = (cutout.centerX - 0.5f) * screenWidthDp
            return if (kotlin.math.abs(shift) < 4f) 0f else shift
        }

        /**
         * Vertical shift (dp) so the capsule is vertically centred on a cutout that sits below
         * the very top edge (typical punch hole). Never negative: a notch touching the edge stays at 0.
         */
        fun autoShiftYDp(
            profile: CutoutProfile,
            cutout: CutoutInfo,
            density: Float,
            capsuleHeightDp: Int
        ): Float {
            if (profile == CUSTOM || cutout.type != CutoutType.PUNCH_HOLE || cutout.height <= 0 || density <= 0f) return 0f
            val centerDp = (cutout.top + cutout.bottom) / 2f / density
            return (centerDp - capsuleHeightDp / 2f).coerceAtLeast(0f)
        }
    }
}
