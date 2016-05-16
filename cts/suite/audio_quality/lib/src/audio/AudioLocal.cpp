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
#include <Log.h>
#include "audio/Buffer.h"
#include "audio/AudioLocal.h"

bool AudioLocal::prepare(AudioHardware::SamplingRate samplingRate,  int gain, int /*mode*/)
{
    LOGV("prepare");
    // gain control not necessary in MobilePre as there is no control.
    // This means audio source itself should be adjusted to control volume
    if (mState == EStNone) {
        if (run() != android::NO_ERROR) {
            LOGE("AudioLocal cannot run");
            // cannot run thread
            return false;
        }
        mState = EStCreated;
    } else if (mState == EStRunning) {
        // wrong usage. first stop!
        return false;
    }
    mClientCompletionWait.tryWait(); // this will reset semaphore to 0 if it is 1.
    mSamplingRate = samplingRate;
    return issueCommandAndWaitForCompletion(ECmInitialize);
}

bool AudioLocal::startPlaybackOrRecord(android::sp<Buffer>& buffer, int numberRepetition)
{
    LOGV("startPlaybackOrRecord");
    if (mState != EStInitialized) {
        LOGE("startPlaybackOrRecord while not initialized");
        // wrong state
        return false;
    }
    mBuffer = buffer;
    mNumberRepetition = numberRepetition;
    mCurrentRepeat = 0;
    return issueCommandAndWaitForCompletion(ECmRun);
}

bool AudioLocal::waitForCompletion()
{
    int waitTimeInMsec = mBuffer->getSamples() / (mSamplingRate/1000);
    waitTimeInMsec += COMMAND_WAIT_TIME_MSEC;
    LOGD("waitForCompletion will wait for %d", waitTimeInMsec);
    if (!mClientCompletionWait.timedWait(waitTimeInMsec)) {
        LOGE("waitForCompletion time-out");
        return false;
    }
    return mCompletionResult;
}

void AudioLocal::stopPlaybackOrRecord()
{
    LOGV("stopPlaybackOrRecord");
    if (mState == EStRunning) {
        issueCommandAndWaitForCompletion(ECmStop);
    }

    if (mState != EStNone) { // thread alive
        requestExit();
        mCurrentCommand = ECmThreadStop;
        mAudioThreadWait.post();
        requestExitAndWait();
        mState = EStNone;
    }
}

bool AudioLocal::issueCommandAndWaitForCompletion(AudioCommand command)
{
    mCurrentCommand = command;
    mAudioThreadWait.post();
    if (!mClientCommandWait.timedWait(COMMAND_WAIT_TIME_MSEC)) {
        LOGE("issueCommandAndWaitForCompletion timeout cmd %d", command);
        return false;
    }
    return mCommandResult;
}

AudioLocal::~AudioLocal()
{
    LOGV("~AudioLocal");
}

AudioLocal::AudioLocal()
    : mState(EStNone),
      mCurrentCommand(ECmNone),
      mClientCommandWait(0),
      mClientCompletionWait(0),
      mAudioThreadWait(0),
      mCompletionResult(false)
{
    LOGV("AudioLocal");
}


bool AudioLocal::threadLoop()
{
    if (mCurrentCommand == ECmNone) {
        if (mState == EStRunning) {
            if (doPlaybackOrRecord(mBuffer)) {
                // check exit condition
                if (mBuffer->bufferHandled()) {
                    mCurrentRepeat++;
                    LOGV("repeat %d - %d", mCurrentRepeat, mNumberRepetition);
                    if (mCurrentRepeat == mNumberRepetition) {
                        LOGV("AudioLocal complete command");
                        mState = EStInitialized;
                        mCompletionResult = true;
                        mClientCompletionWait.post();
                    } else {
                        mBuffer->restart();
                    }
                }
            } else {
                mState = EStInitialized;
                //notify error
                mCompletionResult = false;
                mClientCompletionWait.post();
            }
            return true;
        }
        //LOGV("audio thread waiting");
        mAudioThreadWait.wait();
        //LOGV("audio thread waken up");
        if (mCurrentCommand == ECmNone) {
            return true; // continue to check exit condition
        }
    }

    int pendingCommand = mCurrentCommand;
    // now there is a command
    switch (pendingCommand) {
    case ECmInitialize:
        mCommandResult = doPrepare(mSamplingRate, AudioHardware::SAMPLES_PER_ONE_GO);
        if (mCommandResult) {
            mState = EStInitialized;
        }
        break;
    case ECmRun: {
        mCommandResult = doPlaybackOrRecord(mBuffer);
        if (mCommandResult) {
            mState = EStRunning;
        }
    }
        break;
    case ECmStop:
        doStop();
        mState = EStCreated;
        mCommandResult = true;
        break;
    case ECmThreadStop:
        return false;
        break;
    default:
        // this should not happen
        ASSERT(false);
        break;
    }

    mCurrentCommand = ECmNone;
    mClientCommandWait.post();

    return true;
}


