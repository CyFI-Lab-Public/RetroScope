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


#ifndef CTSAUDIO_AUDIOLOCAL_H
#define CTSAUDIO_AUDIOLOCAL_H

#include <utils/StrongPointer.h>
#include <utils/threads.h>

#include <Semaphore.h>

#include "AudioHardware.h"

class Buffer;

/**
 * Basic API for playback and record
 */
class AudioLocal: public android::Thread, public AudioHardware {
public:

    virtual bool prepare(AudioHardware::SamplingRate samplingRate, int gain,
            int mode = AudioHardware::EModeVoice);
    virtual bool startPlaybackOrRecord(android::sp<Buffer>& buffer, int numberRepetition = 1);
    virtual bool waitForCompletion();
    virtual void stopPlaybackOrRecord();

    virtual ~AudioLocal();
protected:
    AudioLocal();

    virtual bool doPrepare(AudioHardware::SamplingRate, int samplesInOneGo) = 0;
    virtual bool doPlaybackOrRecord(android::sp<Buffer>& buffer) = 0;
    virtual void doStop() = 0;
    virtual void releaseHw() {};

private:


    bool threadLoop();

    enum AudioCommand{
        ECmNone = 0,
        ECmInitialize,
        ECmRun,
        ECmStop,
        ECmThreadStop // terminate the thread
    };

    bool issueCommandAndWaitForCompletion(AudioCommand command);

protected:

private:
    // only one command at a time.
    // Thus, all parameters can be stored here.
    AudioHardware::SamplingRate mSamplingRate;

    android::sp<Buffer> mBuffer;
    int mNumberRepetition;
    int mCurrentRepeat;

    enum AudioState{
        EStNone,
        EStCreated,
        EStInitialized,
        EStRunning  // playing or recording
    };
    volatile AudioState mState;
    volatile AudioCommand mCurrentCommand;


    static const int COMMAND_WAIT_TIME_MSEC = 4000;

    Semaphore mClientCommandWait;
    Semaphore mClientCompletionWait;
    Semaphore mAudioThreadWait;

    bool mCommandResult;
    bool mCompletionResult;
};

#endif // CTSAUDIO_AUDIOLOCAL_H
