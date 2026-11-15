/**
 * ZewMod Engine - JNI Stub
 *
 * This file provides stub implementations so the project compiles without the original .so libs.
 * To use real engine: replace this file's implementations with the actual native lib,
 * OR drop the original .so files into app/src/main/jniLibs/<abi>/ and remove CMakeLists.txt.
 *
 * Original libs from APK:
 *   l30848996_a32.so  -> rename to libzewmod_engine.so -> armeabi-v7a/
 *   l30848996_a64.so  -> rename to libzewmod_engine.so -> arm64-v8a/
 *   l30848996_x86.so  -> rename to libzewmod_engine.so -> x86/
 *   l30848996_x64.so  -> rename to libzewmod_engine.so -> x86_64/
 *
 * The ZewModBridge.kt already handles mock-mode fallback if the lib is missing at runtime.
 */

#include <jni.h>
#include <string.h>
#include <android/log.h>

#define TAG "ZewModEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

/* ---- ZewModBridge native methods ---- */

JNIEXPORT jboolean JNICALL
Java_com_zewmod_engine_bridge_ZewModBridge_nativeInit(
        JNIEnv *env, jobject thiz, jstring config_json) {
    const char *config = (*env)->GetStringUTFChars(env, config_json, 0);
    LOGI("nativeInit: %s", config);
    (*env)->ReleaseStringUTFChars(env, config_json, config);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_zewmod_engine_bridge_ZewModBridge_nativeStart(
        JNIEnv *env, jobject thiz) {
    LOGI("nativeStart called");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_zewmod_engine_bridge_ZewModBridge_nativeStop(
        JNIEnv *env, jobject thiz) {
    LOGI("nativeStop called");
}

JNIEXPORT jboolean JNICALL
Java_com_zewmod_engine_bridge_ZewModBridge_nativeUpdateFeatures(
        JNIEnv *env, jobject thiz, jstring features_csv) {
    const char *features = (*env)->GetStringUTFChars(env, features_csv, 0);
    LOGI("nativeUpdateFeatures: %s", features);
    (*env)->ReleaseStringUTFChars(env, features_csv, features);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_zewmod_engine_bridge_ZewModBridge_nativeUpdateSlider(
        JNIEnv *env, jobject thiz, jstring feature_id, jint value) {
    const char *id = (*env)->GetStringUTFChars(env, feature_id, 0);
    LOGI("nativeUpdateSlider: %s = %d", id, value);
    (*env)->ReleaseStringUTFChars(env, feature_id, id);
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_zewmod_engine_bridge_ZewModBridge_nativeGetStatus(
        JNIEnv *env, jobject thiz) {
    return (*env)->NewStringUTF(env, "STUB_OK");
}

JNIEXPORT jboolean JNICALL
Java_com_zewmod_engine_bridge_ZewModBridge_nativeIsRunning(
        JNIEnv *env, jobject thiz) {
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_zewmod_engine_bridge_ZewModBridge_nativeSetTarget(
        JNIEnv *env, jobject thiz, jstring game_package) {
    const char *pkg = (*env)->GetStringUTFChars(env, game_package, 0);
    LOGI("nativeSetTarget: %s", pkg);
    (*env)->ReleaseStringUTFChars(env, game_package, pkg);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_zewmod_engine_bridge_ZewModBridge_nativeRelease(
        JNIEnv *env, jobject thiz) {
    LOGI("nativeRelease called");
}

/* ---- ZewModVpnService tunnel methods ---- */

JNIEXPORT jboolean JNICALL
Java_com_zewmod_engine_service_ZewModVpnService_tunnelStart(
        JNIEnv *env, jobject thiz,
        jint fd, jstring ip, jint port, jstring key) {
    const char *c_ip  = (*env)->GetStringUTFChars(env, ip,  0);
    const char *c_key = (*env)->GetStringUTFChars(env, key, 0);
    LOGI("tunnelStart fd=%d ip=%s port=%d key=%s", fd, c_ip, port, c_key);
    (*env)->ReleaseStringUTFChars(env, ip,  c_ip);
    (*env)->ReleaseStringUTFChars(env, key, c_key);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_zewmod_engine_service_ZewModVpnService_tunnelStop(
        JNIEnv *env, jobject thiz) {
    LOGI("tunnelStop called");
}
