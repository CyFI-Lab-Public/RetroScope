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


#ifndef CTSAUDIO_AUDIOPROTOCOL_H
#define CTSAUDIO_AUDIOPROTOCOL_H

#include <stdint.h>

#include <utils/StrongPointer.h>
#include "Log.h"
#include "audio/Buffer.h"
#include "ClientSocket.h"

#define U32_ENDIAN_SWAP(x) ( ((x) & 0x000000ff)<<24 | ((x) & 0x0000ff00)<<8 | \
        ((x) & 0x00ff0000)>>8 | ((x) & 0xff000000)>>24 )

class AudioParam {
public:
    bool mStereo;
    uint32_t mSamplingF;
    uint32_t mMode;
    uint32_t mNumberRepetition; // only for playback
    uint32_t mVolume;
    uint32_t mId;
    android::sp<Buffer> mBuffer;
    void* mExtra; // extra data for whatever purpose
};

class AudioProtocol {
public:
    enum CommandId {
        ECmdStart               = 0x12340001, //not actual command
        ECmdDownload            = 0x12340001,
        ECmdStartPlayback       = 0x12340002,
        ECmdStopPlayback        = 0x12340003,
        ECmdStartRecording      = 0x12340004,
        ECmdStopRecording       = 0x12340005,
        ECmdGetDeviceInfo       = 0x12340006,
        ECmdLast                = 0x12340007, // not actual command
    };

    static const uint32_t REPLY_HEADER_SIZE = 12;
    // up to 5 parameters for command / reply
    class ProtocolParam {
    public:
        void* param[5];
    };

    virtual ~AudioProtocol() {
        //LOGD("~AudioProtocol %x", this);
    };

    /// default implementation, no param, no payload
    virtual bool sendCommand(AudioParam& param);
    /// default implementation, no param, no payload
    virtual bool handleReply(const uint32_t* data, AudioParam* param);

    /**
     * read header of reply and returns CommandId of reply.
     * @param socket socket to read
     * @param data pointer to buffer to store header, it should be uint32_t[3]
     * @param id types of reply
     * @return true if everything OK
     */
    static bool handleReplyHeader(ClientSocket& socket, uint32_t* data, CommandId& id);

protected:
    AudioProtocol(ClientSocket& socket, uint32_t command)
        : mCommand(command),
          mSocket(socket) {};

    bool sendData(const char* data, int len) {
        return mSocket.sendData(data, len);
    };

    bool checkHeaderId(const uint32_t* data, uint32_t command);
    bool readData(char* data, int len) {
        return mSocket.readData(data, len);
    };

protected:
    int mBuffer[8];
private:
    uint32_t mCommand;
    ClientSocket& mSocket;

};

class CmdDownload: public AudioProtocol {
public:
    CmdDownload(ClientSocket& socket)
        : AudioProtocol(socket, ECmdDownload) {};
    virtual ~CmdDownload() {};
    virtual bool sendCommand(AudioParam& param);
};


class CmdStartPlayback: public AudioProtocol {
public:
    CmdStartPlayback(ClientSocket& socket)
        : AudioProtocol(socket, ECmdStartPlayback) {};
    virtual ~CmdStartPlayback() {};
    virtual bool sendCommand(AudioParam& param);
};

class CmdStopPlayback: public AudioProtocol {
public:
    CmdStopPlayback(ClientSocket& socket)
        : AudioProtocol(socket, ECmdStopPlayback) {};
    virtual ~CmdStopPlayback() {};
};

class CmdStartRecording: public AudioProtocol {
public:
    CmdStartRecording(ClientSocket& socket)
        : AudioProtocol(socket, ECmdStartRecording) {};
    virtual ~CmdStartRecording() {};

    virtual bool sendCommand(AudioParam& param);

    virtual bool handleReply(const uint32_t* data, AudioParam* param);
};

class CmdStopRecording: public AudioProtocol {
public:
    CmdStopRecording(ClientSocket& socket)
        : AudioProtocol(socket, ECmdStopRecording) {};
    virtual ~CmdStopRecording() {};
};

class CmdGetDeviceInfo: public AudioProtocol {
public:
    CmdGetDeviceInfo(ClientSocket& socket)
        : AudioProtocol(socket, ECmdGetDeviceInfo) {};
    virtual ~CmdGetDeviceInfo() {};

    virtual bool handleReply(const uint32_t* data, AudioParam* param);
};

#endif // CTSAUDIO_AUDIOPROTOCOL_H
