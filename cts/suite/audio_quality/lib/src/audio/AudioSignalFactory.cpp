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
#include <math.h>
#include <stdint.h>
#include <stdlib.h>

#include "Log.h"
#include "audio/AudioSignalFactory.h"

android::sp<Buffer> AudioSignalFactory::generateSineWave(AudioHardware::BytesPerSample BPS,
        int maxPositive, AudioHardware::SamplingRate samplingRate, int signalFreq,
        int samples,  bool stereo)
{
    int bufferSize = samples * (stereo? 2 : 1) * BPS;
    android::sp<Buffer> buffer(new Buffer(bufferSize));
    // only 16bit signed
    ASSERT(BPS == AudioHardware::E2BPS);
    int16_t* data = reinterpret_cast<int16_t*>(buffer->getData());
    double multiplier = 2.0 * M_PI * (double)signalFreq / samplingRate;
    for (int i = 0; i < samples; i++) {
        double val = sin(multiplier * i) * maxPositive;
        *data = (int16_t)val;
        data++;
        if(stereo) {
            *data = (int16_t)val;
            data++;
        }
    }
    buffer->setSize(buffer->getCapacity());
    return buffer;
}
android::sp<Buffer> AudioSignalFactory::generateWhiteNoise(AudioHardware::BytesPerSample BPS,
        int maxPositive, int samples, bool stereo)
{
    int bufferSize = samples * (stereo? 2 : 1) * BPS;
    android::sp<Buffer> buffer(new Buffer(bufferSize, bufferSize));
    // only 16bit signed
    ASSERT(BPS == AudioHardware::E2BPS);
    srand(123456);
    int16_t* data = reinterpret_cast<int16_t*>(buffer->getData());
    int middle = RAND_MAX / 2;
    double multiplier = (double)maxPositive / middle;
    for (int i = 0; i < samples; i++) {
        int val =  rand();
        val = (int16_t)((val - middle) * maxPositive / middle);
        *data = val;
        data++;
        if (stereo) {
            *data = val;
            data++;
        }
    }
    buffer->setSize(buffer->getCapacity());
    return buffer;
}

android::sp<Buffer> AudioSignalFactory::generateZeroSound(AudioHardware::BytesPerSample BPS,
        int samples, bool stereo)
{
    int bufferSize = samples * (stereo? 2 : 1) * BPS;
    android::sp<Buffer> buffer(new Buffer(bufferSize, bufferSize));
    // only 16bit signed
    ASSERT(BPS == AudioHardware::E2BPS);
    int16_t* data = reinterpret_cast<int16_t*>(buffer->getData());
    for (int i = 0; i < samples; i++) {
        *data = 0;
        data++;
        if (stereo) {
            *data = 0;
            data++;
        }
    }
    buffer->setSize(buffer->getCapacity());
    return buffer;
}


