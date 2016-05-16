/*
 * Copyright (C) 2010 The Android Open Source Project
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
 *
 */
#include <cpu-features.h>
#include <jni.h>
#include <string.h>

jboolean android_os_cts_CpuFeatures_isArmCpu(JNIEnv* env, jobject thiz)
{
    AndroidCpuFamily cpuFamily = android_getCpuFamily();
    return cpuFamily == ANDROID_CPU_FAMILY_ARM;
}

jboolean android_os_cts_CpuFeatures_isArm7Compatible(JNIEnv* env, jobject thiz)
{
    uint64_t cpuFeatures = android_getCpuFeatures();
    return (cpuFeatures & ANDROID_CPU_ARM_FEATURE_ARMv7) == ANDROID_CPU_ARM_FEATURE_ARMv7;
}

jboolean android_os_cts_CpuFeatures_isMipsCpu(JNIEnv* env, jobject thiz)
{
    AndroidCpuFamily cpuFamily = android_getCpuFamily();
    return cpuFamily == ANDROID_CPU_FAMILY_MIPS;
}

jboolean android_os_cts_CpuFeatures_isX86Cpu(JNIEnv* env, jobject thiz)
{
    AndroidCpuFamily cpuFamily = android_getCpuFamily();
    return cpuFamily == ANDROID_CPU_FAMILY_X86;
}

static JNINativeMethod gMethods[] = {
    {  "isArmCpu", "()Z",
            (void *) android_os_cts_CpuFeatures_isArmCpu  },
    {  "isArm7Compatible", "()Z",
            (void *) android_os_cts_CpuFeatures_isArm7Compatible  },
    {  "isMipsCpu", "()Z",
            (void *) android_os_cts_CpuFeatures_isMipsCpu  },
    {  "isX86Cpu", "()Z",
            (void *) android_os_cts_CpuFeatures_isX86Cpu  },
};

int register_android_os_cts_CpuFeatures(JNIEnv* env)
{
    jclass clazz = env->FindClass("android/os/cts/CpuFeatures");

    return env->RegisterNatives(clazz, gMethods,
            sizeof(gMethods) / sizeof(JNINativeMethod));
}
