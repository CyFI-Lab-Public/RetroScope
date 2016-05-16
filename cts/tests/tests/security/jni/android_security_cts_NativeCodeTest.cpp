/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <sys/prctl.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <signal.h>
#include <stdlib.h>
#include <cutils/log.h>
#include <linux/perf_event.h>

/*
 * Returns true iff this device is vulnerable to CVE-2013-2094.
 * A patch for CVE-2013-2094 can be found at
 * http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?id=8176cced706b5e5d15887584150764894e94e02f
 */
static jboolean android_security_cts_NativeCodeTest_doPerfEventTest(JNIEnv* env, jobject thiz)
{
    uint64_t attr[10] = { 0x4800000001, (uint32_t) -1, 0, 0, 0, 0x300 };

    int fd = syscall(__NR_perf_event_open, attr, 0, -1, -1, 0);
    jboolean result = (fd != -1);

    if (fd != -1) {
        close(fd);
    }

    return result;
}

/*
 * Detects if the following patch is present.
 * http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?id=c95eb3184ea1a3a2551df57190c81da695e2144b
 *
 * Returns true if the patch is applied, or crashes the system otherwise.
 *
 * While you're at it, you want to apply the following patch too.
 * http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?id=b88a2595b6d8aedbd275c07dfa784657b4f757eb
 * This test doesn't cover the above patch. TODO write a new test.
 *
 * Credit: https://github.com/deater/perf_event_tests/blob/master/exploits/arm_perf_exploit.c
 */
static jboolean android_security_cts_NativeCodeTest_doPerfEventTest2(JNIEnv* env, jobject thiz)
{
    struct perf_event_attr pe[2];
    int fd[2];
    memset(pe, 0, sizeof(pe));
    pe[0].type = 2;
    pe[0].config = 72;
    pe[0].size = 80;
    pe[1].type = PERF_TYPE_RAW;
    pe[1].size = 80;
    fd[0]=syscall(__NR_perf_event_open, &pe[0], 0, 0, -1, 0);
    fd[1]=syscall(__NR_perf_event_open, &pe[1], 0, 0, fd[0], 0);
    close(fd[0]);
    close(fd[1]);
    return true;
}

#define SEARCH_SIZE 0x4000

static int secret;

static bool isValidChildAddress(pid_t child, uintptr_t addr) {
    long word;
    long ret = syscall(__NR_ptrace, PTRACE_PEEKDATA, child, addr, &word);
    return (ret == 0);
}

/* A lazy, do nothing child. GET A JOB. */
static void child() {
    int res;
    ALOGE("in child");
    secret = 0xbaadadd4;
    res = prctl(PR_SET_DUMPABLE, 1, 0, 0, 0);
    if (res != 0) {
        ALOGE("prctl failed");
    }
    res = ptrace(PTRACE_TRACEME, 0, 0, 0);
    if (res != 0) {
        ALOGE("child ptrace failed");
    }
    signal(SIGSTOP, SIG_IGN);
    kill(getpid(), SIGSTOP);
}

static jboolean parent(pid_t child) {
    int status;
    // Wait for the child to suspend itself so we can trace it.
    waitpid(child, &status, 0);
    jboolean result = true;

    uintptr_t addr;
    for (addr = 0x00000000; addr < 0xFFFF1000; addr+=SEARCH_SIZE) {
        if (isValidChildAddress(child, addr)) {
            // Don't scribble on our memory.
            // (which has the same mapping as our child)
            // We don't want to corrupt ourself.
            continue;
        }

        errno = 0;
        syscall(__NR_ptrace, PTRACE_PEEKDATA, child, &secret, addr);
        if (errno == 0) {
            result = false;
            // We found an address which isn't in our our, or our child's,
            // address space, but yet which is still writable. Scribble
            // all over it.
            ALOGE("parent: found writable at %x", addr);
            uintptr_t addr2;
            for (addr2 = addr; addr2 < addr + SEARCH_SIZE; addr2++) {
                syscall(__NR_ptrace, PTRACE_PEEKDATA, child, &secret, addr2);
            }
        }
    }

    ptrace(PTRACE_DETACH, child, 0, 0);
    return result;
}

/*
 * Prior to https://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/arch/arm/include/asm/uaccess.h?id=8404663f81d212918ff85f493649a7991209fa04
 * there was a flaw in the kernel's handling of get_user and put_user
 * requests. Normally, get_user and put_user are supposed to guarantee
 * that reads/writes outside the process's address space are not
 * allowed.
 *
 * In this test, we use prctl(PTRACE_PEEKDATA) to force a write to
 * an address outside of our address space. Without the patch applied,
 * this write succeeds, because prctl(PTRACE_PEEKDATA) uses the
 * vulnerable put_user call.
 */
static jboolean android_security_cts_NativeCodeTest_doVrootTest(JNIEnv*, jobject)
{
    ALOGE("Starting doVrootTest");
    pid_t pid = fork();
    if (pid == -1) {
        return false;
    }

    if (pid == 0) {
        child();
        exit(0);
    }

    return parent(pid);
}

static JNINativeMethod gMethods[] = {
    {  "doPerfEventTest", "()Z",
            (void *) android_security_cts_NativeCodeTest_doPerfEventTest },
    {  "doPerfEventTest2", "()Z",
            (void *) android_security_cts_NativeCodeTest_doPerfEventTest2 },
    {  "doVrootTest", "()Z",
            (void *) android_security_cts_NativeCodeTest_doVrootTest },
};

int register_android_security_cts_NativeCodeTest(JNIEnv* env)
{
    jclass clazz = env->FindClass("android/security/cts/NativeCodeTest");
    return env->RegisterNatives(clazz, gMethods,
            sizeof(gMethods) / sizeof(JNINativeMethod));
}
