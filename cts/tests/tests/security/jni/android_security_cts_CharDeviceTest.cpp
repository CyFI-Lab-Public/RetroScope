/*
 * Copyright (C) 2012 The Android Open Source Project
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
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <cutils/log.h>

#define PHYS_OFFSET 0x40000000

/*
 * Native methods used by
 * cts/tests/tests/permission/src/android/security/cts/CharDeviceTest.java
 */

jboolean android_security_cts_CharDeviceTest_doExynosWriteTest(JNIEnv* env, jobject thiz)
{
    int page_size = sysconf(_SC_PAGE_SIZE);
    int length = page_size * page_size;
    int fd = open("/dev/exynos-mem", O_RDWR);
    if (fd < 0) {
        return true;
    }

    char *addr = (char *)
        mmap(NULL, length, PROT_READ|PROT_WRITE, MAP_SHARED, fd, PHYS_OFFSET);

    if (addr == MAP_FAILED) {
        goto done2;
    }

    /*
     * In the presence of the vulnerability, the code below will
     * cause the device to crash, because we're scribbling all
     * over kernel memory
     */

    int i;
    for (i = 0; i < length; i++) {
        addr[i] = 'A';
    }
    usleep(100000);

done1:
    munmap(addr, length);
done2:
    close(fd);
    return true;
}

jboolean android_security_cts_CharDeviceTest_doExynosReadTest(JNIEnv* env, jobject thiz)
{
    const char *MAGIC_STRING = "KHAAAAN!!! EXYNOS!!!";

    jboolean ret = true;
    int page_size = sysconf(_SC_PAGE_SIZE);
    int length = page_size * page_size;
    int fd = open("/dev/exynos-mem", O_RDONLY);
    if (fd < 0) {
        return true;
    }

    char *addr = (char *)
        mmap(NULL, length, PROT_READ, MAP_SHARED, fd, PHYS_OFFSET);

    if (addr == MAP_FAILED) {
        goto done2;
    }

    // Throw the magic string into the kernel ring buffer. Once
    // there, we shouldn't be able to access it.
    ALOGE("%s", MAGIC_STRING);

    // Now see if we can scan kernel memory, looking for our magic
    // string.  If we find it, return false.
    int i;
    for (i = 0; i < (length - strlen(MAGIC_STRING)); i++) {
        if (strncmp(&addr[i], MAGIC_STRING, strlen(MAGIC_STRING)) == 0) {
            ret = false;
            break;
        }
    }

done1:
    munmap(addr, length);
done2:
    close(fd);
    return ret;
}

static JNINativeMethod gMethods[] = {
    {  "doExynosWriteTest", "()Z",
            (void *) android_security_cts_CharDeviceTest_doExynosWriteTest },
    {  "doExynosReadTest", "()Z",
            (void *) android_security_cts_CharDeviceTest_doExynosReadTest },
};

int register_android_security_cts_CharDeviceTest(JNIEnv* env)
{
    jclass clazz = env->FindClass("android/security/cts/CharDeviceTest");
    return env->RegisterNatives(clazz, gMethods,
            sizeof(gMethods) / sizeof(JNINativeMethod));
}
