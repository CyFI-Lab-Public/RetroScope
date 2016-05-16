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

#include <UniquePtr.h>
#include <tinyalsa/asoundlib.h>
#include <Log.h>
#include <audio/AudioHardware.h>

class MixerTest : public testing::Test {
public:

    virtual void SetUp() {

    }

    virtual void TearDown() {

    }
};


TEST_F(MixerTest, tryTinyAlsaTest) {
    int hwId = AudioHardware::detectAudioHw();
    ASSERT_TRUE(hwId >= 0);
    struct mixer* mixerp = mixer_open(hwId);
    ASSERT_TRUE(mixerp != NULL);
    int num_ctls = mixer_get_num_ctls(mixerp);
    LOGI("Number of mixel control %d", num_ctls);
    for (int i = 0; i < num_ctls; i++) {
        struct mixer_ctl* control = mixer_get_ctl(mixerp, i);
        ASSERT_TRUE(control != NULL);
        LOGI("Mixer control %s type %s value %d", mixer_ctl_get_name(control),
                mixer_ctl_get_type_string(control), mixer_ctl_get_num_values(control));
        free(control);
    }
    // no mixer control for MobilePre. If this assumption fails,
    // mixer control should be added.
    ASSERT_TRUE(num_ctls == 0);
    mixer_close(mixerp);
}

