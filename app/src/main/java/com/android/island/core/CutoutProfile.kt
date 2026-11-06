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
    }
}
