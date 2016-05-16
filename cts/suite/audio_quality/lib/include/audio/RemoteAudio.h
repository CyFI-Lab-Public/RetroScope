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


#ifndef CTSAUDIO_REMOTEAUDIO_H
#define CTSAUDIO_REMOTEAUDIO_H

#include <map>

#include <utils/Looper.h>
#include <utils/String8.h>
#include <utils/StrongPointer.h>
#include <utils/threads.h>

#include "audio/Buffer.h"
#include "AudioProtocol.h"
#include "ClientSocket.h"
#include "Semaphore.h"

class CommandHandler;
/**
 * Tcp communication runs in a separate thread,
 * and client can communicate using public APIs
 * Assumption: only one command at a time. No other command can come
 * while a command is pending.
 */
class RemoteAudio: public android::Thread {
public:

    RemoteAudio(ClientSocket& socket);
    virtual ~RemoteAudio();

    /** launch a thread, and connect to host */
    bool init(int port);
    bool downloadData(const android::String8 name, android::sp<Buffer>& buffer, int& id);
    // <0 : not found
    int getDataId(const android::String8& name);
    bool startPlayback(bool stereo, int samplingF, int mode, int volume,
            int id, int numberRepetition);
    void stopPlayback();
    bool waitForPlaybackCompletion();
    // buffer.getSize() determines number of samples
    bool startRecording(bool stereo, int samplingF, int mode, int volume,
            android::sp<Buffer>& buffer);
    bool waitForRecordingCompletion();
    void stopRecording();

    bool getDeviceInfo(android::String8& data);
    /** should be called before RemoteAudio is destroyed */
    void release();

private:
    RemoteAudio(const RemoteAudio&);

    bool threadLoop();
    void wakeClient(bool result);
    void cleanup(bool notifyClient);

    bool handlePacket();
    static int socketRxCallback(int fd, int events, void* data);

    class CommandHandler;
    void sendCommand(android::sp<android::MessageHandler>& command);

    // this is just semaphore wait without any addition
    bool waitForCompletion(android::sp<android::MessageHandler>& command, int timeInMSec);
    // common code for waitForXXXCompletion
    bool waitForPlaybackOrRecordingCompletion(
            android::sp<android::MessageHandler>& commandHandler);
    // common code for stopXXX
    void doStop(android::sp<android::MessageHandler>& commandHandler, AudioProtocol::CommandId id);

    CommandHandler* toCommandHandler(android::sp<android::MessageHandler>& command) {
        return reinterpret_cast<CommandHandler*>(command.get());
    };

private:
    bool mExitRequested;
    bool mInitResult;
    // used only for notifying successful init
    Semaphore mInitWait;


    enum EventId {
        EIdSocket = 1,
    };
    static const int CLIENT_WAIT_TIMEOUT_MSEC = 2000;
    int mPort;
    ClientSocket& mSocket;


    android::sp<android::Looper> mLooper;

    friend class CommandHandler;

    class CommandHandler: public android::MessageHandler {
    public:
        enum ClientCommands {
            EExit = 1,
        };
        CommandHandler(RemoteAudio& thread, int command)
            : mThread(thread),
              mMessage(command),
              mNotifyOnReply(false),
              mActive(false) {};
        virtual ~CommandHandler() {};
        void handleMessage(const android::Message& message);
        bool timedWait(int timeInMSec) {
            return mClientWait.timedWait(timeInMSec);
        };
        AudioParam& getParam() {
            return mParam;
        };
        android::Message& getMessage() {
            return mMessage;
        };

    private:
        RemoteAudio& mThread;
        AudioParam mParam;
        Semaphore mClientWait;
        android::Mutex mStateLock;
        android::Message mMessage;
        bool mResult;
        bool mNotifyOnReply;
        bool mActive;
        friend class RemoteAudio;
    };
    android::sp<android::MessageHandler> mDownloadHandler;
    android::sp<android::MessageHandler> mPlaybackHandler;
    android::sp<android::MessageHandler> mRecordingHandler;
    android::sp<android::MessageHandler> mDeviceInfoHandler;

    AudioProtocol* mCmds[AudioProtocol::ECmdLast - AudioProtocol::ECmdStart];
    int mDownloadId;
    std::map<int, android::sp<Buffer> > mBufferList;
    std::map<android::String8, int> mIdMap;
};



#endif // CTSAUDIO_REMOTEAUDIO_H
