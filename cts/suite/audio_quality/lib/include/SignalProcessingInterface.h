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

#ifndef CTSAUDIO_SIGNALPROCESSINGINTERFACE_H
#define CTSAUDIO_SIGNALPROCESSINGINTERFACE_H

#include <utils/String8.h>

#include "task/TaskGeneric.h"

/**
 * Interface to Signal processing module to run signal processing script and retrieve the result
 * After construction, init() should be called before doing anything else.
 */
class SignalProcessingInterface {
public:
    virtual ~SignalProcessingInterface() {};

    virtual bool init(const android::String8& script) = 0;
    /**
     * run the script with given input / output parameters. Note that this function does not
     * do any type check.
     * @param functionScript function name (python script name to run for this call)
     * @param nInputs number of inputs. This is the length of inputTypes and inputs array
     * @param inputTypes represent types of each input.
     *              when true: android::sp<Buffer>*, false: Value*
     * @param inputs pointer to input. Either android::sp<Buffer>* or Value*
     * @param nOutputs
     * @param outputTypes
     * @param outputs
     */
    virtual TaskGeneric::ExecutionResult run(const android::String8& functionScript,
            int nInputs, bool* inputTypes, void** inputs,
            int nOutputs, bool* outputTypes, void** outputs) = 0;
};

#endif //CTSAUDIO_SIGNALPROCESSINGINTERFACE_H
