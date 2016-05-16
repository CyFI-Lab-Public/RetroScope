/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <gtest/gtest.h>
#include <audio/AudioHardware.h>
#include <task/TaskAll.h>


class AudioHardwareTest : public testing::Test {

};

TEST_F(AudioHardwareTest, DetectTest) {
    int hwId = AudioHardware::detectAudioHw();
    ASSERT_TRUE(hwId >= 0);
}

TEST_F(AudioHardwareTest, LocalFactoryTest) {
    android::sp<AudioHardware> playback = AudioHardware::createAudioHw(true, true);
    ASSERT_TRUE(playback.get() != NULL);
    android::sp<AudioHardware> recording = AudioHardware::createAudioHw(true, false);
    ASSERT_TRUE(recording.get() != NULL);
}

TEST_F(AudioHardwareTest, RemoteFactoryTest) {
    TaskCase* testCase = new TaskCase();
    ASSERT_TRUE(testCase != NULL);
    android::sp<AudioHardware> playback = AudioHardware::createAudioHw(false, true, testCase);
    ASSERT_TRUE(playback.get() != NULL);
    android::sp<AudioHardware> recording = AudioHardware::createAudioHw(false, false, testCase);
    ASSERT_TRUE(recording.get() != NULL);
    delete testCase;
}
