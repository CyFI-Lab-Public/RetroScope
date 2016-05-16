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

#include <stdint.h>
#include <sys/types.h>

#include <cutils/log.h>
#include <utils/Errors.h>

#include "PowerHAL.h"

namespace android {
// ---------------------------------------------------------------------------

PowerHAL::PowerHAL() : mPowerModule(0), mVSyncHintEnabled(false) {
    int err = hw_get_module(POWER_HARDWARE_MODULE_ID,
            (const hw_module_t **)&mPowerModule);
    ALOGW_IF(err, "%s module not found", POWER_HARDWARE_MODULE_ID);
}

PowerHAL::~PowerHAL() {
}

status_t PowerHAL::initCheck() const {
    return mPowerModule ? NO_ERROR : NO_INIT;
}

status_t PowerHAL::vsyncHint(bool enabled) {
    if (!mPowerModule) {
        return NO_INIT;
    }
    if (mPowerModule->common.module_api_version >= POWER_MODULE_API_VERSION_0_2) {
        if (mPowerModule->powerHint) {
            if (mVSyncHintEnabled != bool(enabled)) {
                mPowerModule->powerHint(mPowerModule,
                        POWER_HINT_VSYNC, (void*)enabled);
                mVSyncHintEnabled = bool(enabled);
            }
        }
    }
    return NO_ERROR;
}

// ---------------------------------------------------------------------------
}; // namespace android

