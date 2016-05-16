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

#include <gtest/gtest.h>

#include <audio/AudioSignalFactory.h>

class AudioSignalFactoryTest: public testing::Test {
protected:

    void testSignalBasic(android::sp<Buffer>& buffer, int maxPositive,
            AudioHardware::SamplingRate samplingRate, int signalFreq, int samples) {
        ASSERT_TRUE(buffer->getSize() == (unsigned int)(AudioHardware::E2BPS * 2 * samples));
        int16_t* data = reinterpret_cast<int16_t*>(buffer->getData());
        for(int i = 0; i < samples; i++) {
            ASSERT_TRUE(*data <= maxPositive);
            ASSERT_TRUE(*data >= -maxPositive);
            data++;
            ASSERT_TRUE(*data <= maxPositive);
            ASSERT_TRUE(*data >= -maxPositive);
            data++;
        }
    }
};

TEST_F(AudioSignalFactoryTest, SineTest) {
    const int maxPositive = 1000;
    const int signalFreq = AudioHardware::ESampleRate_44100/100;
    const int samples = 8192 * 10;
    android::sp<Buffer> buffer = AudioSignalFactory::generateSineWave(AudioHardware::E2BPS,
            maxPositive, AudioHardware::ESampleRate_44100, signalFreq, samples);
    testSignalBasic(buffer, maxPositive, AudioHardware::ESampleRate_44100, signalFreq, samples);
}

TEST_F(AudioSignalFactoryTest, WhiteNoiseTest) {
    const int maxPositive = 1000;
    const int signalFreq = AudioHardware::ESampleRate_44100/100;
    const int samples = 8192 * 10;
    android::sp<Buffer> buffer = AudioSignalFactory::generateWhiteNoise(AudioHardware::E2BPS,
            maxPositive, samples);
    testSignalBasic(buffer, maxPositive, AudioHardware::ESampleRate_44100, signalFreq, samples);
}

