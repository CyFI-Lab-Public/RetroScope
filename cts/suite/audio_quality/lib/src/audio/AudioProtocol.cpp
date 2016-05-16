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
#include <arpa/inet.h>
#include <sys/types.h>
#include <sys/socket.h>

#include <utils/StrongPointer.h>
#include <utils/UniquePtr.h>

#include "audio/Buffer.h"
#include "Log.h"
#include "audio/AudioProtocol.h"


bool AudioProtocol::sendCommand(AudioParam& param)
{
    mBuffer[0] = htonl(mCommand);
    mBuffer[1] = 0;
    return sendData((char*)mBuffer, 8);
}

bool AudioProtocol::handleReply(const uint32_t* data, AudioParam* param)
{
    if (!checkHeaderId(data, mCommand)) {
        return false;
    }
    if (data[1] != 0) { // no endian change for 0
        LOGE("error in reply %d", ntohl(data[1]));
        return false;
    }
    if (data[2] != 0) {
        LOGE("payload length %d not zero", ntohl(data[2]));
        return false;
    }
    return true;
}

bool AudioProtocol::handleReplyHeader(ClientSocket& socket, uint32_t* data, CommandId& id)
{
    if (!socket.readData((char*)data, REPLY_HEADER_SIZE)) {
        LOGE("handleReplyHeader cannot read");
        return false;
    }
    uint32_t command = ntohl(data[0]);
    if ((command & 0xffff0000) != 0x43210000) {
        LOGE("Wrong header %x %x", command, data[0]);
        return false;
    }
    command = (command & 0xffff) | 0x12340000; // convert to id
    if (command < ECmdStart) {
        LOGE("Wrong header %x %x", command, data[0]);
        return false;
    }
    if (command > (ECmdLast - 1)) {
        LOGE("Wrong header %x %x", command, data[0]);
        return false;
    }
    id = (CommandId)command;
    LOGD("received reply with command %x", command);
    return true;
}

bool AudioProtocol::checkHeaderId(const uint32_t* data, uint32_t command)
{
    if (ntohl(data[0]) != ((command & 0xffff) | 0x43210000)) {
        LOGE("wrong reply ID 0x%x", ntohl(data[0]));
        return false;
    }
    return true;
}


/**
 * param0 u32 data id
 * param1 sp<Buffer>
 */
bool CmdDownload::sendCommand(AudioParam& param)
{
    mBuffer[0] = htonl(ECmdDownload);
    mBuffer[1] = htonl(4 + param.mBuffer->getSize());
    mBuffer[2] = htonl(param.mId);
    if(!sendData((char*)mBuffer, 12)) {
        return false;
    }
    return sendData(param.mBuffer->getData(), param.mBuffer->getSize());
}

/**
 * param0 u32 data id
 * param1 u32 sampling rate
 * param2 u32 mono / stereo(MSB) | mode
 * param3 u32 volume
 * param4 u32 repeat
 */
bool CmdStartPlayback::sendCommand(AudioParam& param)
{
    mBuffer[0] = htonl(ECmdStartPlayback);
    mBuffer[1] = htonl(20);
    mBuffer[2] = htonl(param.mId);
    mBuffer[3] = htonl(param.mSamplingF);
    uint32_t mode = param.mStereo ? 0x80000000 : 0;
    mode |= param.mMode;
    mBuffer[4] = htonl(mode);
    mBuffer[5] = htonl(param.mVolume);
    mBuffer[6] = htonl(param.mNumberRepetition);

    return sendData((char*)mBuffer, 28);
}


/**
 * param0 u32 sampling rate
 * param1 u32 mono / stereo(MSB) | mode
 * param2 u32 volume
 * param3 u32 samples
 */
bool CmdStartRecording::sendCommand(AudioParam& param)
{
    mBuffer[0] = htonl(ECmdStartRecording);
    mBuffer[1] = htonl(16);
    mBuffer[2] = htonl(param.mSamplingF);
    uint32_t mode = param.mStereo ? 0x80000000 : 0;
    mode |= param.mMode;
    mBuffer[3] = htonl(mode);
    mBuffer[4] = htonl(param.mVolume);
    uint32_t samples = param.mBuffer->getSize() / (param.mStereo ? 4 : 2);
    mBuffer[5] = htonl(samples);

    return sendData((char*)mBuffer, 24);
}

/**
 * param0 sp<Buffer>
 */
bool CmdStartRecording::handleReply(const uint32_t* data, AudioParam* param)
{
    if (!checkHeaderId(data, ECmdStartRecording)) {
        return false;
    }
    if (data[1] != 0) { // no endian change for 0
        LOGE("error in reply %d", ntohl(data[1]));
        return false;
    }
    int len = ntohl(data[2]);
    if (len > (int)param->mBuffer->getCapacity()) {
        LOGE("received data %d exceeding buffer capacity %d", len, param->mBuffer->getCapacity());
        // read and throw away
        //Buffer tempBuffer(len);
        //readData(tempBuffer.getData(), len);
        return false;
    }
    if (!readData(param->mBuffer->getData(), len)) {
        return false;
    }
    LOGI("received data %d from device", len);
    param->mBuffer->setHandled(len);
    param->mBuffer->setSize(len);
    return true;
}

bool CmdGetDeviceInfo::handleReply(const uint32_t* data, AudioParam* param)
{
    if (!checkHeaderId(data, ECmdGetDeviceInfo)) {
        return false;
    }
    if (data[1] != 0) { // no endian change for 0
        LOGE("error in reply %d", ntohl(data[1]));
        return false;
    }
    int len = ntohl(data[2]);

    UniquePtr<char, DefaultDelete<char[]> > infoString(new char[len + 1]);
    if (!readData(infoString.get(), len)) {
        return false;
    }
    (infoString.get())[len] = 0;
    LOGI("received data %s from device", infoString.get());
    android::String8* string = reinterpret_cast<android::String8*>(param->mExtra);
    string->setTo(infoString.get(), len);
    return true;
}


