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


#ifndef CTSAUDIO_BUILTINPROCESSING_H
#define CTSAUDIO_BUILTINPROCESSING_H

#include "task/TaskGeneric.h"

class BuiltinProcessing {
public:
    BuiltinProcessing();

    typedef TaskGeneric::ExecutionResult \
            (BuiltinProcessing::*BuiltinProcessingMemberFn)(void**, void**);
    struct BuiltinInfo {
        const char* mName;
        BuiltinProcessingMemberFn mFunction;
        size_t mNInput;
        const bool* mInputTypes; // true: android::sp<Buffer>*, false: Value*
        size_t mNOutput;
        const bool* mOutputTypes;
    };

    static const int N_BUILTIN_FNS = 1;
    static BuiltinInfo BUINTIN_FN_TABLE[N_BUILTIN_FNS];
    /**
     * calculate RMS of given data. The rms is passed to moving average filter
     * And the averaged RMS should be within passMin to passMax to pass the test.
     * Otherwise, it will just return EResultOK.
     * Input: android::sp<Buffer> data, int64_t passMin, int64_t passMax
     * Output:int64_t rms
     */
    TaskGeneric::ExecutionResult rms_mva(void** inputs, void** outputs);
private:
    static const int RMS_CONTINUOUS_PASSES = 5;
    int mRMSPasses;
};


#endif // CTSAUDIO_BUILTINPROCESSING_H
