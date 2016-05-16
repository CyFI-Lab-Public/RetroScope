/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

#include <stdint.h>
#include <math.h>
#include <utils/StrongPointer.h>
#include "audio/Buffer.h"
#include "BuiltinProcessing.h"
#include "Log.h"
#include "task/TaskCase.h"

// Buffer, Value, Value
static const bool RMS_MVA_INPUT_TYPE[] = {true, false, false};
// Value
static const bool RMS_MVA_OUTPUT_TYPE[] = {false};

BuiltinProcessing::BuiltinInfo BuiltinProcessing::BUINTIN_FN_TABLE[N_BUILTIN_FNS] =
{
    {
        "rms_mva", &BuiltinProcessing::rms_mva,
        sizeof(RMS_MVA_INPUT_TYPE)/sizeof(bool), RMS_MVA_INPUT_TYPE,
        sizeof(RMS_MVA_OUTPUT_TYPE)/sizeof(bool), RMS_MVA_OUTPUT_TYPE,
    }
};

BuiltinProcessing::BuiltinProcessing()
    : mRMSPasses(0)
{

}

// pass for 5 consecutive passes
TaskGeneric::ExecutionResult BuiltinProcessing::rms_mva(void** inputs, void** outputs)
{
    LOGD("BuiltinProcessing::rms_mva in %x %x %x out %x",
            inputs[0], inputs[1], inputs[2], outputs[0]);
    android::sp<Buffer>& data(*reinterpret_cast<android::sp<Buffer>*>(inputs[0]));

    int64_t passMin = (reinterpret_cast<TaskCase::Value*>(inputs[1]))->getInt64();
    int64_t passMax = (reinterpret_cast<TaskCase::Value*>(inputs[2]))->getInt64();

    int64_t rms = 0;
    size_t samples = data->getSize()/2;
    int16_t* rawData = reinterpret_cast<int16_t*>(data->getData());
    double energy = 0.0f;
    for (size_t i = 0; i < samples; i++) {
        energy += (rawData[i] * rawData[i]);
    }
    rms = (int64_t)sqrt(energy/samples);

    TaskGeneric::ExecutionResult result = TaskGeneric::EResultOK;
    if (rms < passMin) {
        MSG("Volume %lld low compared to min %lld max %lld", rms, passMin, passMax);
        mRMSPasses = 0;
    } else if (rms <= passMax) {
        MSG("Volume %lld OK compared to min %lld max %lld", rms, passMin, passMax);
        mRMSPasses++;
        if (mRMSPasses >= RMS_CONTINUOUS_PASSES) {
            //mRMSPasses = 0;
            result = TaskGeneric::EResultPass;
        }
    } else {
        LOGW("Volume %lld high compared to min %lld max %lld", rms, passMin, passMax);
        mRMSPasses = 0;
    }
    TaskCase::Value* rmsVal = reinterpret_cast<TaskCase::Value*>(outputs[0]);
    rmsVal->setInt64(rms);

    return result;
}


