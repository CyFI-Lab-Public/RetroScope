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

#ifndef ANDROID_RS_DEBUG_HELPER_H
#define ANDROID_RS_DEBUG_HELPER_H

#include "rsUtils.h"
#include "rsDefines.h"

#ifndef RS_SERVER
// This shouldn't ever be defined with RS_SERVER
#define RS_OBJECT_DEBUG 0
#endif

#if RS_OBJECT_DEBUG
#include <utils/CallStack.h>
#endif

namespace android {
namespace renderscript {

#if RS_OBJECT_DEBUG

class DebugHelper {
public:
    DebugHelper() {
        mStack.update(2);
    }

    void dump() {
        mStack.dump();
    }

private:
    CallStack mStack;
};

#else

class DebugHelper {
public:
    DebugHelper() { }
};

#endif

}  // namespace renderscript
}  // namespace android

#endif //ANDROID_RS_DEBUG_HELPER_H

