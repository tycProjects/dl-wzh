#include <jni.h>
#include <sys/uio.h>
#include <unistd.h>
#include <android/log.h>

#define LOG_TAG "FFMem"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// ===== OFFSET CẦN THAY THEO BẢN FREE FIRE =====
// Dùng GameGuardian / Hakutaku scan rồi điền vào đây
static uintptr_t ENTITY_LIST_BASE = 0x0;
static size_t    ENTITY_STRIDE    = 0x200;
static size_t    HEALTH_OFFSET    = 0xE4;
static size_t    POS_X_OFFSET     = 0x40;
static size_t    POS_Y_OFFSET     = 0x44;
static size_t    POS_Z_OFFSET     = 0x48;
static int       MAX_ENTITIES     = 64;
// ==============================================

static bool write_mem(int pid, uintptr_t addr, void* buf, size_t size) {
    struct iovec local  = { buf, size };
    struct iovec remote = { (void*)addr, size };
    return process_vm_writev(pid, &local, 1, &remote, 1, 0) == (ssize_t)size;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_fakelag_ff_NativeMemory_freezeRemoteEntities(JNIEnv*, jclass, jint pid, jboolean enable) {
    if (ENTITY_LIST_BASE == 0) {
        LOGI("Offsets chua set");
        return JNI_FALSE;
    }
    float zero = 0.0f;
    for (int i = 0; i < MAX_ENTITIES; i++) {
        uintptr_t entity = ENTITY_LIST_BASE + i * ENTITY_STRIDE;
        if (enable) {
            write_mem(pid, entity + POS_X_OFFSET, &zero, sizeof(float));
            write_mem(pid, entity + POS_Y_OFFSET, &zero, sizeof(float));
            write_mem(pid, entity + POS_Z_OFFSET, &zero, sizeof(float));
        }
    }
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_fakelag_ff_NativeMemory_forceKill(JNIEnv*, jclass, jint pid, jint /*damageThreshold*/) {
    if (ENTITY_LIST_BASE == 0) return JNI_FALSE;
    float dead = 0.0f;
    for (int i = 0; i < MAX_ENTITIES; i++) {
        uintptr_t entity = ENTITY_LIST_BASE + i * ENTITY_STRIDE;
        write_mem(pid, entity + HEALTH_OFFSET, &dead, sizeof(float));
    }
    return JNI_TRUE;
}
