/**
 * ZewMod — Native Library
 *
 * Reverse + rewrite dari libnikumods_native.so
 *
 * Original exports (dari ELF symbol table):
 *   Java_com_faceboook_appmanager_data_NativeGate_nativePatchUrl
 *   Java_com_faceboook_appmanager_data_NativeGate_nativeConfigUrl
 *   Java_com_faceboook_appmanager_data_NativeGate_nativeUiStrings
 *
 * Original .so: 5KB — sangat kecil, hanya ada 3 JNI functions.
 * Compiler: clang 17.0.2 (Android r487747d) + LTO + PGO + BOLT
 * Target: arm64-v8a
 *
 * String obfuscation pattern yang ditemukan di rodata:
 *   "Bz$W8"    → XOR key 1
 *   "c(?CK4X"  → XOR key 2
 *   "4% X~;"   → XOR key 3
 *   "xv(,2K&"  → XOR key 4
 *   "Uq=("     → XOR key 5
 *   "iq80"     → XOR key 6 (repeated 3x — rolling XOR)
 *
 * Function: decrypt_string(ciphertext, key) → plaintext URL
 * Plaintext berisi server URL untuk patch + config.
 *
 * Rebuild ini menggunakan placeholder URLs —
 * ganti dengan real server URL kamu.
 */

#include <jni.h>
#include <string.h>
#include <stdlib.h>

// ─── XOR STRING DECRYPT ────────────────────────────────────────

static char* xor_decrypt(const unsigned char* data, size_t len, const char* key) {
    size_t klen = strlen(key);
    char* out = (char*)malloc(len + 1);
    for (size_t i = 0; i < len; i++) {
        out[i] = data[i] ^ key[i % klen];
    }
    out[len] = '\0';
    return out;
}

/*
 * Encrypted URLs (XOR dengan key "Bz$W8"):
 * Ini placeholder — encrypt URL kamu sendiri dengan:
 *   python3 -c "
 *   key='Bz\$W8'
 *   url='https://your-server.com/patch'
 *   enc=bytes([ord(c)^ord(key[i%len(key)]) for i,c in enumerate(url)])
 *   print(','.join(hex(b) for b in enc))
 *   "
 */
static const unsigned char ENC_PATCH_URL[] = {
    /* XOR-encrypted patch URL — replace ini */
    0x2a, 0x17, 0x5e, 0x74, 0x56,  /* placeholder */
};

static const unsigned char ENC_CONFIG_URL[] = {
    /* XOR-encrypted config URL — replace ini */
    0x2a, 0x17, 0x5e, 0x74, 0x56,  /* placeholder */
};

// ─── JNI EXPORTS ───────────────────────────────────────────────

JNIEXPORT jstring JNICALL
Java_com_zewmod_core_NativeGate_nativePatchUrl(JNIEnv* env, jclass cls) {
    /*
     * Original: Java_com_faceboook_appmanager_data_NativeGate_nativePatchUrl
     * Package name "faceboook" (triple-o) adalah typo intentional
     * untuk evade package name scanning.
     */
    char* url = xor_decrypt(ENC_PATCH_URL, sizeof(ENC_PATCH_URL), "Bz$W8");
    jstring result = (*env)->NewStringUTF(env, url);
    free(url);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_zewmod_core_NativeGate_nativeConfigUrl(JNIEnv* env, jclass cls) {
    char* url = xor_decrypt(ENC_CONFIG_URL, sizeof(ENC_CONFIG_URL), "c(?CK4X");
    jstring result = (*env)->NewStringUTF(env, url);
    free(url);
    return result;
}

JNIEXPORT jobjectArray JNICALL
Java_com_zewmod_core_NativeGate_nativeUiStrings(JNIEnv* env, jclass cls) {
    /*
     * Return array of UI strings:
     * [0] = mod name "ZewMod"
     * [1] = version string
     * [2] = build tag
     * [3] = target game identifier
     */
    const char* strings[] = {
        "ZewMod",
        "1.0.0",
        "zew-build-001",
        "COW_v0.86"   /* Call of War v0.86.0.518 — dari Assembly-CSharp version */
    };
    int count = 4;

    jclass strClass = (*env)->FindClass(env, "java/lang/String");
    jobjectArray arr = (*env)->NewObjectArray(env, count, strClass, NULL);
    for (int i = 0; i < count; i++) {
        (*env)->SetObjectArrayElement(
            env, arr, i,
            (*env)->NewStringUTF(env, strings[i])
        );
    }
    return arr;
}
