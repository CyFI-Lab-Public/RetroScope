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
#include <binder/IServiceManager.h>
#include <media/IAudioFlinger.h>
#include <media/AudioEffect.h>


using namespace android;


/*
 * Native method used by
 * cts/tests/tests/security/src/android/security/cts/LoadEffectLibraryTest.java
 *
 * Checks that no IAudioFlinger binder transaction manages to load an effect library
 * as LOAD_EFFECT_LIBRARY did in gingerbread.
 */

jboolean android_security_cts_LoadEffectLibraryTest_doLoadLibraryTest(JNIEnv* env, jobject thiz)
{
    sp<IServiceManager> sm = defaultServiceManager();
    if (sm == 0) {
        return false;
    }

    sp<IBinder> binder = sm->getService(String16("media.audio_flinger"));
    if (binder == 0) {
        return false;
    }

    Parcel data, reply;
    sp<IAudioFlinger> af = interface_cast<IAudioFlinger>(binder);

    data.writeInterfaceToken(af->getInterfaceDescriptor());
    // test library path defined in cts/tests/tests/security/testeffect/Android.mk
    data.writeCString("/system/lib/soundfx/libctstesteffect.so");

    // test 100 IAudioFlinger binder transaction values and check that none corresponds
    // to LOAD_EFFECT_LIBRARY and successfully loads our test library
    for (uint32_t i = IBinder::FIRST_CALL_TRANSACTION;
            i < IBinder::FIRST_CALL_TRANSACTION + 100;
            i++) {
        status_t status = binder->transact(i, data, &reply);
        if (status != NO_ERROR) {
            continue;
        }
        status = reply.readInt32();
        if (status != NO_ERROR) {
            continue;
        }

        // Effect UUID defined in cts/tests/tests/security/testeffect/CTSTestEffect.cpp
        effect_uuid_t uuid =
                    {0xff93e360, 0x0c3c, 0x11e3, 0x8a97, {0x00, 0x02, 0xa5, 0xd5, 0xc5, 0x1b}};
        effect_descriptor_t desc;

        status = AudioEffect::getEffectDescriptor(&uuid, &desc);
        if (status == NO_ERROR) {
            return false;
        }
    }
    return true;
}

static JNINativeMethod gMethods[] = {
    {  "doLoadLibraryTest", "()Z",
            (void *) android_security_cts_LoadEffectLibraryTest_doLoadLibraryTest },
};

int register_android_security_cts_LoadEffectLibraryTest(JNIEnv* env)
{
    jclass clazz = env->FindClass("android/security/cts/LoadEffectLibraryTest");
    return env->RegisterNatives(clazz, gMethods,
            sizeof(gMethods) / sizeof(JNINativeMethod));
}
