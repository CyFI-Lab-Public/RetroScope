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

#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <errno.h>

#include <utils/StrongPointer.h>

#include "Log.h"
#include "audio/Buffer.h"
#include "StringUtil.h"
#include "SimpleScriptExec.h"
#include "SignalProcessingImpl.h"
#include "task/TaskCase.h"

enum ToPythonCommandType {
    EHeader             = 0x0,
    ETerminate          = 0x1,
    EFunctionName       = 0x2,
    EAudioMono          = 0x4,
    EAudioStereo        = 0x5,
    EValue64Int         = 0x8,
    EValueDouble        = 0x9,
    EExecutionResult    = 0x10
};

const android::String8 \
    SignalProcessingImpl::MAIN_PROCESSING_SCRIPT("test_description/processing_main.py");

SignalProcessingImpl::SignalProcessingImpl()
    : mChildRunning(false),
      mBuffer(1024)
{

}

SignalProcessingImpl::~SignalProcessingImpl()
{
    if (mSocket.get() != NULL) {
        int terminationCommand [] = {ETerminate, 0};
        send((char*)terminationCommand, sizeof(terminationCommand));
        mSocket->release();
    }
    if (mChildRunning) {
        waitpid(mChildPid, NULL, 0);
    }
}

#define CHILD_LOGE(x...) do { fprintf(stderr, x); \
    fprintf(stderr, " %s - %d\n", __FILE__, __LINE__); } while(0)

const int CHILD_WAIT_TIME_US = 100000;

bool SignalProcessingImpl::init(const android::String8& script)
{
    pid_t pid;
    if ((pid = fork()) < 0) {
        LOGE("SignalProcessingImpl::init fork failed %d", errno);
        return false;
    } else if (pid == 0) { // child
        if (execl(SimpleScriptExec::PYTHON_PATH, SimpleScriptExec::PYTHON_PATH,
                script.string(), NULL) < 0) {
            CHILD_LOGE("execl %s %s failed %d", SimpleScriptExec::PYTHON_PATH,
                    script.string(), errno);
            exit(EXIT_FAILURE);
        }
    } else { // parent
        mChildPid = pid;
        mChildRunning = true;
        int result = false;
        int retryCount = 0;
        // not that clean, but it takes some time for python side to have socket ready
        const int MAX_RETRY = 20;
        while (retryCount < MAX_RETRY) {
            usleep(CHILD_WAIT_TIME_US);
            mSocket.reset(new ClientSocket());
            if (mSocket.get() == NULL) {
                result = false;
                break;
            }
            if (mSocket->init("127.0.0.1", SCRIPT_PORT, true)) {
                result = true;
                break;
            }
            retryCount++;
        }
        if (!result) {
            LOGE("cannot connect to child");
            mSocket.reset(NULL);
            return result;
        }
    }
    return true;
}


TaskGeneric::ExecutionResult SignalProcessingImpl::run( const android::String8& functionScript,
        int nInputs, bool* inputTypes, void** inputs,
        int nOutputs, bool* outputTypes, void** outputs)
{
    mBuffer.reset();
    mBuffer.write <int32_t>((int32_t)EHeader);
    mBuffer.write<int32_t>(nInputs + 1);
    mBuffer.write<int32_t>((int32_t)EFunctionName);
    mBuffer.write<int32_t>((int32_t)functionScript.length());
    mBuffer.writeStr(functionScript);
    if (!send(mBuffer.getBuffer(), mBuffer.getSizeWritten())) {
        LOGE("send failed");
        return TaskGeneric::EResultError;
    }
    for (int i = 0; i < nInputs; i++) {
        mBuffer.reset();
        if (inputTypes[i]) { // android::sp<Buffer>*
            android::sp<Buffer>* buffer = reinterpret_cast<android::sp<Buffer>*>(inputs[i]);
            mBuffer.write<int32_t>((int32_t)((*buffer)->isStereo() ? EAudioStereo : EAudioMono));
            int dataLen = (*buffer)->getSize();
            mBuffer.write<int32_t>(dataLen);
            if (!send(mBuffer.getBuffer(), mBuffer.getSizeWritten())) {
                LOGE("send failed");
                return TaskGeneric::EResultError;
            }
            if (!send((*buffer)->getData(), dataLen)) {
                LOGE("send failed");
                return TaskGeneric::EResultError;
            }
            LOGD("%d-th param buffer %d, stereo:%d", i, dataLen, (*buffer)->isStereo());
        } else { //TaskCase::Value*
            TaskCase::Value* val = reinterpret_cast<TaskCase::Value*>(inputs[i]);
            bool isI64 = (val->getType() == TaskCase::Value::ETypeI64);
            mBuffer.write<int32_t>((int32_t)(isI64 ? EValue64Int : EValueDouble));
            if (isI64) {
                mBuffer.write<int64_t>(val->getInt64());
            } else  {
                mBuffer.write<double>(val->getDouble());
            }
            if (!send(mBuffer.getBuffer(), mBuffer.getSizeWritten())) {
                LOGE("send failed");
                return TaskGeneric::EResultError;
            }
            LOGD("%d-th param Value", i);
        }
    }
    int32_t header[4]; // id 0 - no of types - id 0x10 - ExecutionResult
    if (!read((char*)header, sizeof(header))) {
        LOGE("read failed");
        return TaskGeneric::EResultError;
    }
    if (header[0] != 0) {
        LOGE("wrong data");
        return TaskGeneric::EResultError;
    }
    if (header[2] != EExecutionResult) {
        LOGE("wrong data");
        return TaskGeneric::EResultError;
    }
    if (header[3] == TaskGeneric::EResultError) {
        LOGE("script returned error %d", header[3]);
        return (TaskGeneric::ExecutionResult)header[3];
    }
    if ((header[1] - 1) != nOutputs) {
        LOGE("wrong data");
        return TaskGeneric::EResultError;
    }
    for (int i = 0; i < nOutputs; i++) {
        int32_t type;
        if (!read((char*)&type, sizeof(type))) {
            LOGE("read failed");
            return TaskGeneric::EResultError;
        }
        if (outputTypes[i]) { // android::sp<Buffer>*
            int32_t dataLen;
            if (!read((char*)&dataLen, sizeof(dataLen))) {
                LOGE("read failed");
                return TaskGeneric::EResultError;
            }
            android::sp<Buffer>* buffer = reinterpret_cast<android::sp<Buffer>*>(outputs[i]);
            if (buffer->get() == NULL) { // data not allocated, this can happen for unknown-length output
                *buffer = new Buffer(dataLen, dataLen, (type == EAudioStereo) ? true: false);
                if (buffer->get() == NULL) {
                    LOGE("alloc failed");
                    return TaskGeneric::EResultError;
                }
            }
            bool isStereo = (*buffer)->isStereo();

            if (((type == EAudioStereo) && isStereo) || ((type == EAudioMono) && !isStereo)) {
                // valid
            } else {
                LOGE("%d-th output wrong type %d stereo: %d", i, type, isStereo);
                return TaskGeneric::EResultError;
            }

            if (dataLen > (int)(*buffer)->getSize()) {
                LOGE("%d-th output data too long %d while buffer size %d", i, dataLen,
                        (*buffer)->getSize());
                return TaskGeneric::EResultError;
            }
            if (!read((*buffer)->getData(), dataLen)) {
                LOGE("read failed");
                return TaskGeneric::EResultError;
            }
            LOGD("received buffer %x %x", ((*buffer)->getData())[0], ((*buffer)->getData())[1]);
            (*buffer)->setHandled(dataLen);
            (*buffer)->setSize(dataLen);
        } else { //TaskCase::Value*
            TaskCase::Value* val = reinterpret_cast<TaskCase::Value*>(outputs[i]);
            if ((type == EValue64Int) || (type == EValueDouble)) {
                if (!read((char*)val->getPtr(), sizeof(int64_t))) {
                    LOGE("read failed");
                    return TaskGeneric::EResultError;
                }
                if (type == EValue64Int) {
                    val->setType(TaskCase::Value::ETypeI64);
                } else {
                    val->setType(TaskCase::Value::ETypeDouble);
                }
            } else {
                LOGE("wrong type %d", type);
                return TaskGeneric::EResultError;
            }
        }
    }
    return (TaskGeneric::ExecutionResult)header[3];
}

bool SignalProcessingImpl::send(const char* data, int len)
{
    //LOGD("send %d", len);
    return mSocket->sendData(data, len);
}

bool SignalProcessingImpl::read(char* data, int len)
{
    const int READ_TIMEOUT_MS = 60000 * 2; // as some calculation like calc_delay takes almost 20 secs
    //LOGD("read %d", len);
    return mSocket->readData(data, len, READ_TIMEOUT_MS);
}

