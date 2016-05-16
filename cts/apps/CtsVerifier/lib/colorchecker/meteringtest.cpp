/*
 * Copyright (C) 2011 The Android Open Source Project
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
#define LOG_NDEBUG 0

#define LOG_TAG "MeteringTest"
#include <utils/Log.h>
#include <utils/Timers.h>
#include <cmath>
#include <string>

#include "vec2.h"
#include "vec3.h"
#include "meteringtest.h"

const float kOverExposure = 230.f;
const float kEqThreshold = 0.05f;
// Processes the checker colors stored by comparing the pixel values from the
// two scenarios in a test.
void MeteringTest::processData() {
    ALOGV("Start Processing Metering Test Data!");

    int numTests = mCheckerColors.size() / 2;
    mNumPatches = 0;

    if (numTests > 0) {
        mNumPatches = mCheckerColors[0].size();
    }

    for (int i = 0; i < numTests; ++i) {
        mComparisonResults.push_back(
                isEquivalentTo((&mCheckerColors[i * 2]),
                               (&mCheckerColors[i * 2 + 1])));
        mComparisonResults.push_back(
                isDarkerThan((&mCheckerColors[i * 2]),
                             (&mCheckerColors[i * 2 + 1])));
    }
}

void MeteringTest::clearData() {
    mComparisonResults.clear();
    mCheckerColors.clear();
}

// Compares two given arrays of pixel values and decide whether the first one is
// significantly darker than the second one.
bool MeteringTest::isDarkerThan(
        const std::vector<Vec3f>* checkerColors1,
        const std::vector<Vec3f>* checkerColors2) const {
    float meanRatio = 0.f;
    int meanNumCount = 0;

    for (int i = 0; i < mNumPatches; ++i) {
        float luminance1 = (*checkerColors1)[i].convertToLuminance();
        float luminance2 = (*checkerColors2)[i].convertToLuminance();

        // Out of the saturation rage, define 5% as a margin for being
        // significantly brighter.
        if ((luminance2 < kOverExposure) && (luminance1 != 0.f)) {
            meanRatio += luminance2 / luminance1;
            ++meanNumCount;
        }
    }
    meanRatio = meanRatio / meanNumCount;

    return (meanRatio > 1 + kEqThreshold);
}

// Compares the two givn arrays of pixel values and decide whether they are
// equivalent within an acceptable range.
bool MeteringTest::isEquivalentTo(
        const std::vector<Vec3f>* checkerColors1,
        const std::vector<Vec3f>* checkerColors2) const {
    float meanRatio = 0.f;
    int meanNumCount = 0;

    for (int i = 0; i < mNumPatches; ++i) {
        float luminance1 = (*checkerColors1)[i].convertToLuminance();
        float luminance2 = (*checkerColors2)[i].convertToLuminance();
        ALOGV("Luma_1 and Luma_2 is %f, %f", luminance1, luminance2);

        if ((luminance1 < kOverExposure) && (luminance2 < kOverExposure)) {
              meanRatio += luminance2 / luminance1;
              ++meanNumCount;
        }
    }
    meanRatio = meanRatio / meanNumCount;

    return ((meanRatio >= 1 - kEqThreshold) && (meanRatio <= 1 + kEqThreshold));
}
