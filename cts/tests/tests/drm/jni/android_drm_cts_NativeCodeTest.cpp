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
#include <utils/Log.h>

/*
 * Returns true iff this device may be vulnerable to installation of rogue drm
 * plugins, as determined by the existance of the _installDrmEngine symbol in the
 * libdrmframework_jni.so library.
 */
static jboolean android_drm_cts_InstallDrmEngineTest(JNIEnv* env, jobject thiz)
{
    jboolean result = false;

    // install /system/lib/libdrmtestplugin.so
    FILE *f = popen("service call drm.drmManager 6 i32 0 i32 31 i32 1937339183 i32 795698548 "
                    "i32 794978668 i32 1684171116 i32 1702129010 i32 1819309171 i32 1852401525 "
                    "i32 7303982 i32 1598902849", "r");
    if (f) {
        char buffer[128];
        if (fgets(buffer, sizeof(buffer), f) != NULL) {
            const char *match = "Result: Parcel(00000000    '....')";
            if (!strncmp(buffer, match, strlen(match))) {
                result = true;
            }
        }
        pclose(f);
    }
    return result;
}

static JNINativeMethod gMethods[] = {
    {  "doInstallDrmEngineTest", "()Z",
       (void *) android_drm_cts_InstallDrmEngineTest },
};

int register_android_drm_cts_NativeCodeTest(JNIEnv* env)
{
    jclass clazz = env->FindClass("android/drm/cts/NativeCodeTest");
    return env->RegisterNatives(clazz, gMethods,
            sizeof(gMethods) / sizeof(JNINativeMethod));
}
