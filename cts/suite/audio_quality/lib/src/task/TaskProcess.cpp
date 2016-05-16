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
#include <stdlib.h>
#include <string.h>

#include <vector>

#include "Log.h"
#include "StringUtil.h"
#include "task/TaskProcess.h"
#include "SignalProcessingImpl.h"

TaskProcess::TaskProcess()
    : TaskGeneric(TaskGeneric::ETaskProcess)
{

}

TaskProcess::~TaskProcess()
{
}

TaskGeneric::ExecutionResult TaskProcess::run()
{
    if (mType == EBuiltin) {
        return doRun(true);
    } else {
        if (mSp.get() == NULL) {
            mSp.reset(new SignalProcessingImpl());
            if (!mSp->init(SignalProcessingImpl::MAIN_PROCESSING_SCRIPT)) {
                mSp.reset(NULL);
                return TaskGeneric::EResultError;
            }
        }
        return doRun(false);
    }
}

// Allocate Buffers and Values to pass to builtin functions
bool TaskProcess::prepareParams(std::vector<TaskProcess::Param>& list,
        const bool* paramTypes,
        UniquePtr<void_ptr, DefaultDelete<void_ptr[]> > & ptrs,
        UniquePtr<UniqueValue, DefaultDelete<UniqueValue[]> > & values,
        UniquePtr<UniqueBuffer, DefaultDelete<UniqueBuffer[]> > & buffers,
        bool isInput)
{
    size_t N = list.size();

    LOGD("TaskProcess::prepareParams N = %d", N);
    ptrs.reset(new void_ptr[N]);
    if (ptrs.get() == NULL) {
        LOGE("alloc failed");
        return false;
    }
    // set to NULL to detect illegal access
    bzero(ptrs.get(), N * sizeof(void_ptr));
    values.reset(new UniqueValue[N]);
    if (values.get() == NULL) {
        LOGE("alloc failed");
        return false;
    }
    buffers.reset(new UniqueBuffer[N]);
    if (buffers.get() == NULL) {
        LOGE("alloc failed");
        return false;
    }

    void_ptr* voidPtrs = ptrs.get();
    UniqueValue* valuesPtr = values.get();
    UniqueBuffer* buffersPtr = buffers.get();
    for (size_t i = 0; i < N; i++) {
        if ((paramTypes != NULL) && paramTypes[i] && (list[i].getType() != EId)) {
            LOGE("mismatching types %d %d", paramTypes[i], list[i].getType());
            return false;
        }
        if ((paramTypes != NULL) && !paramTypes[i] && (list[i].getType() == EId)) {
            LOGE("mismatching types %d %d", paramTypes[i], list[i].getType());
            return false;
        }
        switch(list[i].getType()) {
        case EId: {
            UniquePtr<android::sp<Buffer> > buffer(new android::sp<Buffer>());
            if (buffer.get() == NULL) {
                LOGE("alloc failed");
                return false;
            }
            if (isInput) {
                *(buffer.get()) = getTestCase()->findBuffer(list[i].getParamString());
                if (buffer.get()->get() == NULL) {
                    LOGE("find failed");
                    return false;
                }
                LOGD("input buffer len %d stereo %d", (*buffer.get())->getSize(),
                        (*buffer.get())->isStereo());
            }
            buffersPtr[i].reset(buffer.release());
            voidPtrs[i] = buffersPtr[i].get();
        }
        break;
        case EVal: {
            valuesPtr[i].reset(new TaskCase::Value());
            if (isInput) {
                if (!getTestCase()->findValue(list[i].getParamString(), *(valuesPtr[i].get()))) {
                    LOGE("find %s failed", list[i].getParamString().string());
                    return false;
                }
            }
            voidPtrs[i] = valuesPtr[i].get();
        }
        break;
        case EConst: {
            if (!isInput) {
                LOGE("const for output");
                return false;
            }
            voidPtrs[i] = list[i].getValuePtr();

            if (list[i].getValue().getType() == TaskCase::Value::ETypeDouble) {
                LOGD(" %f", list[i].getValue().getDouble());
            } else {
                LOGD(" %lld", list[i].getValue().getInt64());
            }
        }
        break;
        }
        LOGD("TaskProcess::prepareParams %d-th, const 0x%x", i, voidPtrs[i]);
    }
    return true;
}

// run builtin function by searching BuiltinProcessing::BUINTIN_FN_TABLE
TaskGeneric::ExecutionResult TaskProcess::doRun(bool builtIn)
{
    BuiltinProcessing::BuiltinInfo* info = NULL;
    if (builtIn) {
        for (int i = 0; i < BuiltinProcessing::N_BUILTIN_FNS; i++) {
            if (StringUtil::compare(mName, BuiltinProcessing::BUINTIN_FN_TABLE[i].mName) == 0) {
                info = &BuiltinProcessing::BUINTIN_FN_TABLE[i];
                break;
            }
        }
        if (info == NULL) {
            LOGE("TaskProcess::runBuiltin no match for %s", mName.string());
            return TaskGeneric::EResultError;
        }
        if (mInput.size() != info->mNInput) {
            LOGE("TaskProcess::runBuiltin size mismatch %d vs %d", mInput.size(), info->mNInput);
            return TaskGeneric::EResultError;
        }
        if (mOutput.size() != info->mNOutput) {
            LOGE("TaskProcess::runBuiltin size mismatch %d vs %d", mOutput.size(), info->mNOutput);
            return TaskGeneric::EResultError;
        }
    }
    // This is for passing to builtin fns. Just void pts will be cleared in exit
    UniquePtr<void_ptr, DefaultDelete<void_ptr[]> > inputs;
    // This is for holding Value instances. Will be destroyed in exit
    UniquePtr<UniqueValue, DefaultDelete<UniqueValue[]> > inputValues;
    // This is for holding android::sp<Buffer>. Buffer itself is from the global map.
    UniquePtr<UniqueBuffer, DefaultDelete<UniqueBuffer[]> > inputBuffers;

    UniquePtr<void_ptr, DefaultDelete<void_ptr[]> > outputs;
    // Value is created here. Builtin function just need to set it.
    UniquePtr<UniqueValue, DefaultDelete<UniqueValue[]> > outputValues;
    // Buffer itself should be allocated by the builtin function itself.
    UniquePtr<UniqueBuffer, DefaultDelete<UniqueBuffer[]> > outputBuffers;

    if (!prepareParams(mInput, builtIn ? info->mInputTypes : NULL, inputs, inputValues,
            inputBuffers, true)) {
        return TaskGeneric::EResultError;
    }

    if (!prepareParams(mOutput, builtIn ? info->mOutputTypes : NULL, outputs, outputValues,
            outputBuffers, false)) {
        return TaskGeneric::EResultError;
    }

    TaskGeneric::ExecutionResult result;
    if (builtIn) {
        result = (mBuiltin.*(info->mFunction))(inputs.get(), outputs.get());
    } else {
        UniquePtr<bool, DefaultDelete<bool[]> > inputTypes(new bool[mInput.size()]);
        for (size_t i = 0; i < mInput.size(); i++) {
            (inputTypes.get())[i] = mInput[i].isIdType();
        }
        UniquePtr<bool, DefaultDelete<bool[]> > outputTypes(new bool[mOutput.size()]);
        for (size_t i = 0; i < mOutput.size(); i++) {
            (outputTypes.get())[i] = mOutput[i].isIdType();
        }
        result = mSp->run( mName,
                mInput.size(), inputTypes.get(), inputs.get(),
                mOutput.size(), outputTypes.get(), outputs.get());
    }
    if ((result == TaskGeneric::EResultOK) || (result == TaskGeneric::EResultFail)
            || (result == TaskGeneric::EResultPass)) {
        // try to save result
        bool saveResultFailed = false;
        for (size_t i = 0; i < mOutput.size(); i++) {
            if (mOutput[i].isIdType()) { // Buffer
                android::sp<Buffer>* bufferp =
                        reinterpret_cast<android::sp<Buffer>*>((outputs.get())[i]);
                if (!getTestCase()->registerBuffer(mOutput[i].getParamString(), *bufferp)) {
                    // maybe already there, try update
                    if (!getTestCase()->updateBuffer(mOutput[i].getParamString(), *bufferp)) {
                        LOGE("cannot register / update %d-th output Buffer for builtin fn %s",
                                i, mName.string());
                        saveResultFailed = true; // mark failure, but continue
                    }
                }
            } else { // Value
                TaskCase::Value* valuep =
                        reinterpret_cast<TaskCase::Value*>((outputs.get())[i]);
                if (!getTestCase()->registerValue(mOutput[i].getParamString(), *valuep)) {
                    if (!getTestCase()->updateValue(mOutput[i].getParamString(), *valuep)) {
                        LOGE("cannot register / update %d-th output Value for builtin fn %s",
                                i, mName.string());
                        saveResultFailed = true; // mark failure, but continue
                    }
                }
            }
        }
        if (saveResultFailed) {
            LOGE("TaskProcess::runBuiltin cannot save result");
            return TaskGeneric::EResultError;
        }
    }
    LOGV("TaskProcess::runBuiltin return %d", result);
    return result;
}

bool TaskProcess::parseParams(std::vector<TaskProcess::Param>& list, const char* str, bool isInput)
{
    LOGV("TaskProcess::parseParams will parse %s", str);
    android::String8 paramStr(str);
    UniquePtr<std::vector<android::String8> > paramTokens(StringUtil::split(paramStr, ','));
    if (paramTokens.get() == NULL) {
        LOGE("split failed");
        return false;
    }
    std::vector<android::String8>& tokens = *(paramTokens.get());
    for (size_t i = 0; i < tokens.size(); i++) {
        UniquePtr<std::vector<android::String8> > itemTokens(StringUtil::split(tokens[i], ':'));
        if (itemTokens.get() == NULL) {
            LOGE("split failed");
            return false;
        }
        if (itemTokens->size() != 2) {
            LOGE("size mismatch %d", itemTokens->size());
            return false;
        }
        std::vector<android::String8>& item = *(itemTokens.get());
        if (StringUtil::compare(item[0], "id") == 0) {
            Param param(EId, item[1]);
            list.push_back(param);
            LOGD(" id %s", param.getParamString().string());
        } else if (StringUtil::compare(item[0], "val") == 0) {
            Param param(EVal, item[1]);
            list.push_back(param);
            LOGD(" val %s", param.getParamString().string());
        } else if (isInput && (StringUtil::compare(item[0], "consti") == 0)) {
            long long value = atoll(item[1].string());
            TaskCase::Value v(value);
            Param param(v);
            list.push_back(param);
            LOGD("consti %lld", value);
        } else if (isInput && (StringUtil::compare(item[0], "constf") == 0)) {
            double value = atof(item[1].string());
            TaskCase::Value v(value);
            Param param(v);
            list.push_back(param);
            LOGD("constf %f", value);
        } else {
            LOGE("unrecognized word %s", item[0].string());
            return false;
        }
        LOGV("TaskProcess::parseParams %d-th type %d", i, list[i].getType());
    }
   return true;
}

bool TaskProcess::parseAttribute(const android::String8& name, const android::String8& value)
{
    if (StringUtil::compare(name, "method") == 0) {
        UniquePtr<std::vector<android::String8> > tokenPtr(StringUtil::split(value, ':'));
        std::vector<android::String8>* tokens = tokenPtr.get();
        if (tokens == NULL) {
            LOGE("split failed");
            return false;
        }
        if (tokens->size() != 2) {
            LOGE("cannot parse attr %s %s", name.string(), value.string());
            return false;
        }
        if (StringUtil::compare(tokens->at(0), "builtin") == 0) {
            mType = EBuiltin;
        } else if (StringUtil::compare(tokens->at(0), "script") == 0) {
            mType = EScript;
        } else {
            LOGE("cannot parse attr %s %s", name.string(), value.string());
            return false;
        }
        mName.append(tokens->at(1));
        return true;
    } else if (StringUtil::compare(name, "input") == 0) {
        return parseParams(mInput, value, true);
    } else if (StringUtil::compare(name, "output") == 0) {
        return parseParams(mOutput, value, false);
    } else {
        LOGE("cannot parse attr %s %s", name.string(), value.string());
        return false;
    }
}

TaskProcess::Param::Param(TaskProcess::ParamType type, android::String8& string)
    : mType(type),
      mString(string)
{
    ASSERT((type == TaskProcess::EId) || (type == TaskProcess::EVal));

}

TaskProcess::Param::Param(TaskCase::Value& val)
    : mType(TaskProcess::EConst),
      mValue(val)
{

}

TaskProcess::ParamType TaskProcess::Param::getType()
{
    return mType;
}

android::String8& TaskProcess::Param::getParamString()
{
    ASSERT((mType == TaskProcess::EId) || (mType == TaskProcess::EVal));
    return mString;
}

TaskCase::Value& TaskProcess::Param::getValue()
{
    ASSERT(mType == TaskProcess::EConst);
    return mValue;
}

TaskCase::Value* TaskProcess::Param::getValuePtr()
{
    ASSERT(mType == TaskProcess::EConst);
    return &mValue;
}
