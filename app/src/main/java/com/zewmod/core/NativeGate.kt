package com.zewmod.core

/**
 * ZewMod — Native Bridge
 *
 * Reverse-engineered dari libnikumods_native.so
 * JNI exports yang ditemukan:
 *   Java_com_faceboook_appmanager_data_NativeGate_nativePatchUrl
 *   Java_com_faceboook_appmanager_data_NativeGate_nativeConfigUrl
 *   Java_com_faceboook_appmanager_data_NativeGate_nativeUiStrings
 *
 * Library ini obfuscated tapi XOR-encrypted string di rodata —
 * keys: "Bz$W8", "c(?CK4X", "4% X~;" dan variasi lainnya.
 *
 * Fungsi: menyimpan URL config + patch server yang di-hardcode
 * di dalam .so agar susah ditemukan lewat string scan biasa.
 */
object NativeGate {

    init {
        System.loadLibrary("zewmod_native")
    }

    /** Return URL server patch (Assembly-CSharp-patch.bytes sumber) */
    external fun nativePatchUrl(): String

    /** Return URL config JSON dari server */
    external fun nativeConfigUrl(): String

    /** Return obfuscated UI strings (versi, nama mod, dsb) */
    external fun nativeUiStrings(): Array<String>
}
