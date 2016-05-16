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
#include <regex.h>
#include <stdlib.h>

#include <tinyalsa/asoundlib.h>

#include "Log.h"
#include "StringUtil.h"
#include "SimpleScriptExec.h"
#include "audio/AudioHardware.h"
#include "audio/Buffer.h"
#include "audio/AudioPlaybackLocal.h"
#include "audio/AudioRecordingLocal.h"
#include "audio/AudioRemote.h"
#include "task/TaskCase.h"

int AudioHardware::mHwId = -1;

int AudioHardware::detectAudioHw()
{
    android::String8 script("test_description/conf/detect_usb_audio.py");
    /* This is the list of supported devices.
       MobilePre: M-Audio MobilePre
       Track: M-Audio FastTrack
     */
    android::String8 param("MobilePre Track");
    android::String8 resultStr;
    if (!SimpleScriptExec::runScript(script, param, resultStr)) {
        LOGE("cannot run script");
        return -1;
    }

    android::String8 match("[ \t]+([A-Za-z0-9_]+)[ \t]+([0-9]+)");
    const int nmatch = 3;
    regmatch_t pmatch[nmatch];
    if (!SimpleScriptExec::checkIfPassed(resultStr, match, nmatch, pmatch)) {
        LOGE("result not correct %s", resultStr.string());
        return -1;
    }
    LOGV("pmatch 0: %d, %d  1:%d, %d  2:%d, %d",
        pmatch[0].rm_so, pmatch[0].rm_eo,
        pmatch[1].rm_so, pmatch[1].rm_eo,
        pmatch[2].rm_so, pmatch[2].rm_eo);

    if (pmatch[1].rm_so == -1) {
        return -1;
    }
    if (pmatch[2].rm_so == -1) {
        return -1;
    }
    android::String8 product = StringUtil::substr(resultStr, pmatch[1].rm_so,
            pmatch[1].rm_eo - pmatch[1].rm_so);
    LOGI("Audio device %s found", product.string());
    android::String8 cardNumber = StringUtil::substr(resultStr, pmatch[2].rm_so,
            pmatch[2].rm_eo - pmatch[2].rm_so);
    int cardN = atoi(cardNumber.string());
    LOGI("Card number : %d", cardN);
    return cardN;
}

android::sp<AudioHardware> AudioHardware::createAudioHw(bool local, bool playback,
        TaskCase* testCase)
{
    android::sp<AudioHardware> hw;
    if (local) {
        if (mHwId < 0) {
            mHwId = detectAudioHw();
        }
        if (mHwId < 0) {
            return NULL;
        }
        if (playback) {
            hw = new AudioPlaybackLocal(mHwId);
        } else {
            hw = new AudioRecordingLocal(mHwId);
        }
    } else {
        if (testCase != NULL) {
            if (playback) {
                hw = new AudioRemotePlayback(testCase->getRemoteAudio());
            } else {
                hw = new AudioRemoteRecording(testCase->getRemoteAudio());
            }
        }
    }
    return hw;
}

AudioHardware::~AudioHardware()
{

}

bool AudioHardware::startPlaybackOrRecordById(const android::String8& id, TaskCase* testCase)
{
    if (testCase == NULL) { // default implementation only handles local buffer.
        return false;
    }
    android::sp<Buffer> buffer = testCase->findBuffer(id);
    if (buffer.get() == NULL) {
        return false;
    }
    return startPlaybackOrRecord(buffer);
}
