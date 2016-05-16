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

#include <arpa/inet.h>
#include <strings.h>
#include <sys/types.h>
#include <sys/socket.h>

#include <utils/Looper.h>

#include "Log.h"
#include "audio/AudioProtocol.h"
#include "audio/RemoteAudio.h"


RemoteAudio::RemoteAudio(ClientSocket& socket)
    : mExitRequested(false),
      mSocket(socket),
      mDownloadHandler(new CommandHandler(*this, (int)AudioProtocol::ECmdDownload)),
      mPlaybackHandler(new CommandHandler(*this, (int)AudioProtocol::ECmdStartPlayback)),
      mRecordingHandler(new CommandHandler(*this, (int)AudioProtocol::ECmdStartRecording)),
      mDeviceInfoHandler(new CommandHandler(*this, (int)AudioProtocol::ECmdGetDeviceInfo)),
      mDownloadId(0)
{
    mCmds[AudioProtocol::ECmdDownload - AudioProtocol::ECmdStart] = new CmdDownload(socket);
    mCmds[AudioProtocol::ECmdStartPlayback - AudioProtocol::ECmdStart] =
            new CmdStartPlayback(socket);
    mCmds[AudioProtocol::ECmdStopPlayback - AudioProtocol::ECmdStart] =
            new CmdStopPlayback(socket);
    mCmds[AudioProtocol::ECmdStartRecording - AudioProtocol::ECmdStart] =
            new CmdStartRecording(socket);
    mCmds[AudioProtocol::ECmdStopRecording - AudioProtocol::ECmdStart] =
            new CmdStopRecording(socket);
    mCmds[AudioProtocol::ECmdGetDeviceInfo - AudioProtocol::ECmdStart] =
                new CmdGetDeviceInfo(socket);
}

RemoteAudio::~RemoteAudio()
{
    for (int i = 0; i < (AudioProtocol::ECmdLast - AudioProtocol::ECmdStart); i++) {
        delete mCmds[i];
    }
    //mBufferList.clear();
}

bool RemoteAudio::init(int port)
{
    mPort = port;
    if (run() != android::NO_ERROR) {
        LOGE("RemoteAudio cannot run");
        // cannot run thread
        return false;
    }

    if (!mInitWait.timedWait(CLIENT_WAIT_TIMEOUT_MSEC)) {
        return false;
    }
    return mInitResult;
}

bool RemoteAudio::threadLoop()
{
    // initial action until socket connection done by init
    mLooper = new android::Looper(false);
    if (mLooper.get() == NULL) {
        wakeClient(false);
        return false;
    }
    android::Looper::setForThread(mLooper);

    if (!mSocket.init("127.0.0.1", mPort)) {
        wakeClient(false);
        return false;
    }
    LOGD("adding fd %d to polling", mSocket.getFD());
    mLooper->addFd(mSocket.getFD(), EIdSocket, ALOOPER_EVENT_INPUT, socketRxCallback, this);
    wakeClient(true);
    while(!mExitRequested) {
        mLooper->pollOnce(10000);
    }
    return false; // exit without requestExit()
}

void RemoteAudio::wakeClient(bool result)
{
    mInitResult = result;
    mInitWait.post();
}

bool RemoteAudio::handlePacket()
{
    uint32_t data[AudioProtocol::REPLY_HEADER_SIZE/sizeof(uint32_t)];
    AudioProtocol::CommandId id;
    if (!AudioProtocol::handleReplyHeader(mSocket, data, id)) {
        return false;
    }
    CommandHandler* handler = NULL;
    if (id == AudioProtocol::ECmdDownload) {
        handler = reinterpret_cast<CommandHandler*>(mDownloadHandler.get());
    } else if (id == AudioProtocol::ECmdStartPlayback) {
        handler = reinterpret_cast<CommandHandler*>(mPlaybackHandler.get());
    } else if (id == AudioProtocol::ECmdStartRecording) {
        handler = reinterpret_cast<CommandHandler*>(mRecordingHandler.get());
    } else if (id == AudioProtocol::ECmdGetDeviceInfo) {
        handler = reinterpret_cast<CommandHandler*>(mDeviceInfoHandler.get());
    }
    AudioParam* param = NULL;
    if (handler != NULL) {
        param = &(handler->getParam());
    }
    bool result = mCmds[id - AudioProtocol::ECmdStart]->handleReply(data, param);
    if (handler != NULL) {
        LOGD("handler present. Notify client");
        android::Mutex::Autolock lock(handler->mStateLock);
        if (handler->mNotifyOnReply) {
            handler->mNotifyOnReply = false;
            handler->mResult = result;
            handler->mClientWait.post();
        }
        handler->mActive = false;
    }
    return result;
}

int RemoteAudio::socketRxCallback(int fd, int events, void* data)
{
    RemoteAudio* self = reinterpret_cast<RemoteAudio*>(data);
    if (events & ALOOPER_EVENT_INPUT) {
        //LOGD("socketRxCallback input");
        if (!self->handlePacket()) { //error, stop polling
            LOGE("socketRxCallback, error in packet, stopping polling");
            return 0;
        }
    }
    return 1;
}

void RemoteAudio::sendCommand(android::sp<android::MessageHandler>& command)
{
    mLooper->sendMessage(command, toCommandHandler(command)->getMessage());
}

bool RemoteAudio::waitForCompletion(android::sp<android::MessageHandler>& command, int timeInMSec)
{
    LOGV("waitForCompletion %d", timeInMSec);
    return toCommandHandler(command)->timedWait(timeInMSec);
}

bool RemoteAudio::waitForPlaybackOrRecordingCompletion(
        android::sp<android::MessageHandler>& commandHandler)
{
    CommandHandler* handler = reinterpret_cast<CommandHandler*>(commandHandler.get());
    handler->mStateLock.lock();
    if(!handler->mActive) {
        handler->mStateLock.unlock();
        return true;
    }
    int runTime = handler->getParam().mBuffer->getSize() /
            (handler->getParam().mStereo ? 4 : 2) * 1000 / handler->getParam().mSamplingF;
    handler->mNotifyOnReply = true;
    handler->mStateLock.unlock();
    return waitForCompletion(commandHandler, runTime + CLIENT_WAIT_TIMEOUT_MSEC);
}

void RemoteAudio::doStop(android::sp<android::MessageHandler>& commandHandler,
        AudioProtocol::CommandId id)
{
    CommandHandler* handler = reinterpret_cast<CommandHandler*>(commandHandler.get());
    handler->mStateLock.lock();
    if (!handler->mActive) {
        handler->mStateLock.unlock();
       return;
    }
    handler->mActive = false;
    handler->mNotifyOnReply = false;
    handler->mStateLock.unlock();
    android::sp<android::MessageHandler> command(new CommandHandler(*this, (int)id));
    sendCommand(command);
    waitForCompletion(command, CLIENT_WAIT_TIMEOUT_MSEC);
}


bool RemoteAudio::downloadData(const android::String8 name, android::sp<Buffer>& buffer, int& id)
{
    CommandHandler* handler = reinterpret_cast<CommandHandler*>(mDownloadHandler.get());
    id = mDownloadId;
    mDownloadId++;
    handler->mStateLock.lock();
    handler->getParam().mId = id;
    handler->getParam().mBuffer = buffer;
    handler->mNotifyOnReply = true;
    handler->mStateLock.unlock();
    sendCommand(mDownloadHandler);

    // assume 1Mbps ==> 1000 bits per msec ==> 125 bytes per msec
    int maxWaitTime = CLIENT_WAIT_TIMEOUT_MSEC + buffer->getSize() / 125;
    // client blocked until reply comes from DUT
    if (!waitForCompletion(mDownloadHandler, maxWaitTime)) {
        LOGE("timeout");
        return false;
    }
    mBufferList[id] = buffer;
    mIdMap[name] = id;
    return handler->mResult;
}

int RemoteAudio::getDataId(const android::String8& name)
{
    std::map<android::String8, int>::iterator it;
    it = mIdMap.find(name);
    if (it == mIdMap.end()) {
        LOGE("Buffer name %s not registered", name.string());
        return -1;
    }
    return it->second;
}

bool RemoteAudio::startPlayback(bool stereo, int samplingF, int mode, int volume,
        int id, int numberRepetition)
{
    CommandHandler* handler = reinterpret_cast<CommandHandler*>(mPlaybackHandler.get());
    handler->mStateLock.lock();
    if (handler->mActive) {
        LOGE("busy");
        handler->mStateLock.unlock();
        return false;
    }
    std::map<int, android::sp<Buffer> >::iterator it;
    it = mBufferList.find(id);
    if (it == mBufferList.end()) {
        LOGE("Buffer id %d not registered", id);
        return false;
    }
    LOGD("RemoteAudio::startPlayback stereo %d mode %d", stereo, mode);
    handler->mActive = true;
    handler->getParam().mStereo = stereo;
    handler->getParam().mSamplingF = samplingF;
    handler->getParam().mMode = mode;
    handler->getParam().mVolume = volume;
    handler->getParam().mId = id;
    // for internal tracking
    handler->getParam().mBuffer = it->second;
    handler->getParam().mNumberRepetition = numberRepetition;
    handler->mStateLock.unlock();
    sendCommand(mPlaybackHandler);
    if (!waitForCompletion(mPlaybackHandler, CLIENT_WAIT_TIMEOUT_MSEC)) {
        LOGE("timeout");
        return false;
    }
    return handler->mResult;
}

void RemoteAudio::stopPlayback()
{
    doStop(mPlaybackHandler, AudioProtocol::ECmdStopPlayback);
}

bool RemoteAudio::waitForPlaybackCompletion()
{
    return waitForPlaybackOrRecordingCompletion(mPlaybackHandler);
}

bool RemoteAudio::startRecording(bool stereo, int samplingF, int mode, int volume,
        android::sp<Buffer>& buffer)
{
    CommandHandler* handler = reinterpret_cast<CommandHandler*>(mRecordingHandler.get());
    handler->mStateLock.lock();
    if (handler->mActive) {
        LOGE("busy");
        handler->mStateLock.unlock();
        return false;
    }
    handler->mActive = true;
    handler->getParam().mStereo = stereo;
    handler->getParam().mSamplingF = samplingF;
    handler->getParam().mMode = mode;
    handler->getParam().mVolume = volume;
    handler->getParam().mBuffer = buffer;
    handler->mStateLock.unlock();
    sendCommand(mRecordingHandler);
    if (!waitForCompletion(mRecordingHandler, CLIENT_WAIT_TIMEOUT_MSEC)) {
        LOGE("timeout");
        return false;
    }
    return handler->mResult;
}

bool RemoteAudio::waitForRecordingCompletion()
{
    return waitForPlaybackOrRecordingCompletion(mRecordingHandler);
}

void RemoteAudio::stopRecording()
{
    doStop(mRecordingHandler, AudioProtocol::ECmdStopRecording);
}

bool RemoteAudio::getDeviceInfo(android::String8& data)
{
    CommandHandler* handler = reinterpret_cast<CommandHandler*>(mDeviceInfoHandler.get());
    handler->mStateLock.lock();
    handler->mNotifyOnReply = true;
    handler->getParam().mExtra = &data;
    handler->mStateLock.unlock();
    sendCommand(mDeviceInfoHandler);

    // client blocked until reply comes from DUT
    if (!waitForCompletion(mDeviceInfoHandler, CLIENT_WAIT_TIMEOUT_MSEC)) {
        LOGE("timeout");
        return false;
    }
    return handler->mResult;
}

/** should be called before RemoteAudio is destroyed */
void RemoteAudio::release()
{
    android::sp<android::MessageHandler> command(new CommandHandler(*this, CommandHandler::EExit));
    sendCommand(command);
    join(); // wait for exit
    mSocket.release();
}

void RemoteAudio::CommandHandler::handleMessage(const android::Message& message)
{
    switch(message.what) {
    case EExit:
        LOGD("thread exit requested, will exit ");
        mResult = true;
        mThread.mExitRequested = true;
        mClientWait.post(); // client will not wait, but just do it.
        break;
    case AudioProtocol::ECmdDownload:
    case AudioProtocol::ECmdStartPlayback:
    case AudioProtocol::ECmdStopPlayback:
    case AudioProtocol::ECmdStartRecording:
    case AudioProtocol::ECmdStopRecording:
    case AudioProtocol::ECmdGetDeviceInfo:
    {
        mResult = (mThread.mCmds[message.what - AudioProtocol::ECmdStart]) \
                ->sendCommand(mParam);
        // no post for download and getdeviceinfo. Client blocked until reply comes with time-out
        if ((message.what != AudioProtocol::ECmdDownload) &&
            (message.what != AudioProtocol::ECmdGetDeviceInfo)    ) {
            mClientWait.post();
        }

    }
        break;

    }
}
