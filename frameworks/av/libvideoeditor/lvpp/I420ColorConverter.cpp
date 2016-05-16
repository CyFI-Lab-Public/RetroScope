/*
 * Copyright (C) 2011 The Android Open Source Project
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

#include <I420ColorConverter.h>
#include <cutils/log.h>
#include <dlfcn.h>

I420ColorConverter::I420ColorConverter() {
    // Open the shared library
    mHandle = dlopen("libI420colorconvert.so", RTLD_NOW);

    if (mHandle == NULL) {
        ALOGW("I420ColorConverter: cannot load libI420colorconvert.so");
        return;
    }

    // Find the entry point
    void (*getI420ColorConverter)(I420ColorConverter *converter) =
        (void (*)(I420ColorConverter*)) dlsym(mHandle, "getI420ColorConverter");

    if (getI420ColorConverter == NULL) {
        ALOGW("I420ColorConverter: cannot load getI420ColorConverter");
        dlclose(mHandle);
        mHandle = NULL;
        return;
    }

    // Fill the function pointers.
    getI420ColorConverter(this);

    ALOGI("I420ColorConverter: libI420colorconvert.so loaded");
}

bool I420ColorConverter::isLoaded() {
    return mHandle != NULL;
}

I420ColorConverter::~I420ColorConverter() {
    if (mHandle) {
        dlclose(mHandle);
    }
}
