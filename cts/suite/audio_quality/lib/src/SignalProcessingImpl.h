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

#ifndef CTSAUDIO_SIGNALPROCESSINGIMPL_H
#define CTSAUDIO_SIGNALPROCESSINGIMPL_H

#include <UniquePtr.h>
#include <utils/String8.h>

#include "SignalProcessingInterface.h"
#include "ClientSocket.h"
#include "RWBuffer.h"

/**
 * Implements SignalProcessingInterface
 */
class SignalProcessingImpl: public SignalProcessingInterface {
public:
    static const android::String8 MAIN_PROCESSING_SCRIPT;
    SignalProcessingImpl();
    virtual ~SignalProcessingImpl();
    /**
     * @param script main script to call function script
     */
    virtual bool init(const android::String8& script);

    virtual TaskGeneric::ExecutionResult run(const android::String8& functionScript,
            int nInputs, bool* inputTypes, void** inputs,
            int nOutputs, bool* outputTypes, void** outputs);
private:
    bool send(const char* data, int len);
    bool read(char* data, int len);

private:
    static const int SCRIPT_PORT = 15010;
    UniquePtr<ClientSocket> mSocket;
    pid_t mChildPid;
    bool mChildRunning;
    RWBuffer mBuffer;
};


#endif //CTSAUDIO_SIGNALPROCESSINGIMPL_H
