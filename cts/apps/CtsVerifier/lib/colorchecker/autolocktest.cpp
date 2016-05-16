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

#define LOG_TAG "AutoLockTest"
#include <utils/Log.h>
#include <utils/Timers.h>
#include <cmath>
#include <string>

#include "vec2.h"
#include "vec3.h"
#include "autolocktest.h"

const float kOverExposure = 230.f;
const float kEqThreshold = 0.05f;
// Processes the color checker values and compare the two values from
// the same individual test.
void AutoLockTest::processData() {
    ALOGV("Start Processing Auto Lock Test Data!");

    int numTests = mCheckerColors.size() / 2;
    mNumPatches = 0;

    if (numTests > 0) {
        mNumPatches = mCheckerColors[0].size();
    }

    for (int i = 0; i < numTests; ++i) {
        mComparisonResults.push_back(
                IsBrighterThan((&mCheckerColors[i * 2]),
                               (&mCheckerColors[i * 2 + 1])));
        mComparisonResults.push_back(
                IsEquivalentTo((&mCheckerColors[i * 2]),
                               (&mCheckerColors[i * 2 + 1])));
    }
}

// Compares whether one array of gray color patches is brighter than
// another one.
bool AutoLockTest::IsBrighterThan(
        const std::vector<Vec3f>* colorCheckers1,
        const std::vector<Vec3f>* colorCheckers2) const {
    float meanRatio = 0.f;
    int meanNumCount = 0;

    for (int i = 0; i < mNumPatches; ++i) {
        float luminance1 = (*colorCheckers1)[i].convertToLuminance();
        float luminance2 = (*colorCheckers2)[i].convertToLuminance();

        // Consider a 5% raise as a considerably large increase.
        if ((luminance1 < kOverExposure) && (luminance2 != 0.f)) {
            meanRatio += luminance1 / luminance2;
            ++meanNumCount;
        }
    }
    meanRatio = meanRatio / meanNumCount;

    return (meanRatio > 1 + kEqThreshold);
}

// Compares whether one array of gray color patches is within a small range
// of the other one to be considered equivalent.
bool AutoLockTest::IsEquivalentTo(
        const std::vector<Vec3f>* colorCheckers1,
        const std::vector<Vec3f>* colorCheckers2) const {
    float meanRatio = 0.f;
    int meanNumCount = 0;

    for (int i = 0; i < mNumPatches; ++i) {
        float luminance1 = (*colorCheckers1)[i].convertToLuminance();
        float luminance2 = (*colorCheckers2)[i].convertToLuminance();
        ALOGV("Luma_1 and Luma_2 is %f, %f", luminance1, luminance2);

        if ((luminance1 < kOverExposure) && (luminance2 < kOverExposure)) {
              meanRatio += luminance2 / luminance1;
              ++meanNumCount;
        }
    }
    meanRatio = meanRatio / meanNumCount;

    return ((meanRatio >= 1 - kEqThreshold) && (meanRatio <= 1 + kEqThreshold));
}

void AutoLockTest::clearData() {
    mCheckerColors.clear();
    mComparisonResults.clear();
}
