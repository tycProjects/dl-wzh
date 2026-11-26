#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <unistd.h>
#include <cstdlib>
#include <cstring>
#include "node.h"

// Send Node's stdout/stderr to logcat (tag WZHNode) so server errors are visible with adb.
static int pipe_fds[2];
static pthread_t log_thread;

static void* log_pump(void*) {
    char buf[1024];
    ssize_t n;
    while ((n = read(pipe_fds[0], buf, sizeof(buf) - 1)) > 0) {
        if (buf[n - 1] == '\n') --n;
        buf[n] = 0;
        __android_log_write(ANDROID_LOG_INFO, "WZHNode", buf);
    }
    return nullptr;
}

static void redirect_output() {
    setvbuf(stdout, nullptr, _IONBF, 0);
    setvbuf(stderr, nullptr, _IONBF, 0);
    if (pipe(pipe_fds) != 0) return;
    dup2(pipe_fds[1], 1);
    dup2(pipe_fds[1], 2);
    if (pthread_create(&log_thread, nullptr, log_pump, nullptr) == 0) pthread_detach(log_thread);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_wzh_nodert_NodeBridge_start(JNIEnv* env, jclass, jobjectArray arguments) {
    jsize argc = env->GetArrayLength(arguments);
    size_t total = 0;
    for (jsize i = 0; i < argc; i++) {
        jstring s = (jstring) env->GetObjectArrayElement(arguments, i);
        const char* c = env->GetStringUTFChars(s, nullptr);
        total += strlen(c) + 1;
        env->ReleaseStringUTFChars(s, c);
        env->DeleteLocalRef(s);
    }
    // Node wants argv in one contiguous block.
    char* block = (char*) calloc(total + 1, 1);
    char** argv = (char**) calloc(argc + 1, sizeof(char*));
    char* cur = block;
    for (jsize i = 0; i < argc; i++) {
        jstring s = (jstring) env->GetObjectArrayElement(arguments, i);
        const char* c = env->GetStringUTFChars(s, nullptr);
        size_t len = strlen(c);
        memcpy(cur, c, len);
        argv[i] = cur;
        cur += len + 1;
        env->ReleaseStringUTFChars(s, c);
        env->DeleteLocalRef(s);
    }
    redirect_output();
    return (jint) node::Start((int) argc, argv);
}
