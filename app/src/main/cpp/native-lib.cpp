#include <jni.h>
#include <android/log.h>
#include <atomic>

namespace {
std::atomic<bool> demo_enabled{true};
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_promptdrop_ragedemo_MainActivity_nativeDemoStatus(JNIEnv*, jobject) {
    return demo_enabled.load() ? JNI_TRUE : JNI_FALSE;
}
extern "C" JNIEXPORT void JNICALL
Java_com_promptdrop_ragedemo_MainActivity_nativeSetDemo(JNIEnv*, jobject, jboolean enabled) {
    demo_enabled.store(enabled == JNI_TRUE);
    __android_log_print(ANDROID_LOG_INFO, "PromptDropDemo",
                        "offline demo=%s", enabled ? "enabled" : "disabled");
}
