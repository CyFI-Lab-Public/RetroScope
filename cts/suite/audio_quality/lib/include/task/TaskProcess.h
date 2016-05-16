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


#ifndef CTSAUDIO_TASKPROCESS_H
#define CTSAUDIO_TASKPROCESS_H

#include <vector>

#include <UniquePtr.h>
#include <utils/StrongPointer.h>

#include "audio/Buffer.h"
#include "TaskGeneric.h"
#include "TaskCase.h"
#include "BuiltinProcessing.h"
#include "SignalProcessingInterface.h"

class TaskProcess: public TaskGeneric {
public:
    TaskProcess();
    virtual ~TaskProcess();
    virtual TaskGeneric::ExecutionResult run();
    virtual bool parseAttribute(const android::String8& name, const android::String8& value);
private:
    TaskGeneric::ExecutionResult doRun(bool builtin);

    class Param;
    bool parseParams(std::vector<Param>& list, const char* str, bool isInput);
    //typedef necessary to prevent compiler's confusion
    typedef void* void_ptr;
    typedef UniquePtr<TaskCase::Value> UniqueValue;
    typedef UniquePtr<android::sp<Buffer> > UniqueBuffer;
    /// construct Buffers and Values for calling builtin functions.
    /// all constructed stuffs automatically deleted by the passed UniquePtrs
    bool prepareParams(std::vector<TaskProcess::Param>& list,
            const bool* inputTypes,
            UniquePtr<void_ptr, DefaultDelete<void_ptr[]> > & ptrs,
            UniquePtr<UniqueValue, DefaultDelete<UniqueValue[]> > & values,
            UniquePtr<UniqueBuffer, DefaultDelete<UniqueBuffer[]> > & buffers,
            bool isInput);

private:
    enum ProcessType {
        EBuiltin,
        EScript
    };
    ProcessType mType;
    android::String8 mName; // buit-in function or script name

    enum ParamType {
        EId,
        EVal,
        EConst
    };
    class Param {
    public:
        Param(ParamType type, android::String8& string);
        Param(TaskCase::Value& val);
        ParamType getType();
        android::String8& getParamString();
        TaskCase::Value& getValue();
        TaskCase::Value* getValuePtr();
        inline bool isIdType() {
            return (mType == EId);
        }
    private:
        ParamType mType;
        android::String8 mString;
        TaskCase::Value mValue;
    };

    std::vector<Param> mInput;
    std::vector<Param> mOutput;
    BuiltinProcessing mBuiltin;
    UniquePtr<SignalProcessingInterface> mSp;
};


#endif // CTSAUDIO_TASKPROCESS_H
