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

#include <utils/String8.h>

#include <gtest/gtest.h>

#include <audio/AudioSignalFactory.h>
#include <ClientInterface.h>
#include <ClientImpl.h>
#include <GenericFactory.h>
#include <audio/RemoteAudio.h>



class ClientInterfaceTest : public testing::Test {
protected:
    ClientInterface* mClient;

protected:
    virtual void SetUp() {
        GenericFactory factory;
        mClient = factory.createClientInterface();
        ASSERT_TRUE(mClient != NULL);
        android::String8 dummyParam;
        ASSERT_TRUE(mClient->init(dummyParam));
    }

    virtual void TearDown() {
        delete mClient;
        mClient = NULL;
    }
};

TEST_F(ClientInterfaceTest, InitTest) {
    // all done in SetUp
}

TEST_F(ClientInterfaceTest, getDeviceInfoTest) {
    ClientImpl* client = reinterpret_cast<ClientImpl*>(mClient);
    android::sp<RemoteAudio>& audio(client->getAudio());
    android::String8 info;

    ASSERT_TRUE(audio->getDeviceInfo(info));
    LOGD("device info %s", info.string());
}

TEST_F(ClientInterfaceTest, PlayTest) {
    ClientImpl* client = reinterpret_cast<ClientImpl*>(mClient);
    android::sp<RemoteAudio>& audio(client->getAudio());
    const int maxPositive = 10000;
    const int signalFreq = AudioHardware::ESampleRate_44100/100;
    const int samples = 8192*2;
    android::sp<Buffer> buffer = AudioSignalFactory::generateSineWave(AudioHardware::E2BPS,
            maxPositive, AudioHardware::ESampleRate_44100, signalFreq, samples);
    int id;
    android::String8 name("1");
    ASSERT_TRUE(audio->downloadData(name, buffer, id));
    ASSERT_TRUE(audio->startPlayback(true, AudioHardware::ESampleRate_44100,
            AudioHardware::EModeVoice, 100, id, 1));
    ASSERT_TRUE(audio->waitForPlaybackCompletion());
    ASSERT_TRUE(id == audio->getDataId(name));
    android::String8 name2("2");
    ASSERT_TRUE(audio->getDataId(name2) < 0);
}

TEST_F(ClientInterfaceTest, RecordTest) {
    ClientImpl* client = reinterpret_cast<ClientImpl*>(mClient);
    android::sp<RemoteAudio>& audio(client->getAudio());
    const int maxPositive = 10000;
    const int signalFreq = AudioHardware::ESampleRate_44100 / 100;
    const int samples = 44100 * 4;
    android::sp<Buffer> buffer(new Buffer(samples * 2, samples * 2, false));

    ASSERT_TRUE(audio->startRecording(false, AudioHardware::ESampleRate_44100,
            AudioHardware::EModeVoice, 100, buffer));
    ASSERT_TRUE(audio->waitForRecordingCompletion());
    ASSERT_TRUE(buffer->amountHandled() == (samples * 2));
}
