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

#define LOG_TAG "ExposureCompensationTest"
#include <utils/Log.h>
#include <utils/Timers.h>
#include <cmath>
#include <string>
#include <stdio.h>

#include "vec2.h"
#include "vec3.h"
#include "exposurecompensationtest.h"

const float GAMMA_CORRECTION = 2.2f;
void ExposureCompensationTest::processData() {
    ALOGV("Start Processing Exposure Compensation Test Data!");
    clearDebugImage();

    if (mDebugText != NULL) {
        delete mDebugText;
        mDebugText = NULL;
    }

    int numTests = mExposureValues.size();
    int numPatches = mCheckerColors[0].size();
    ALOGV("Processing %d tests with %d patches", numTests, numPatches);

    mDebugText = new char[320 * numTests];
    mDebugText[0] = 0;
    char* debugText = new char[50];

    Vec3i red(255, 0, 0);
    Vec3i green(0, 255, 0);
    Vec3i blue(0, 0, 255);

    float minExposure = -3.0f;
    float scale = 9.0f;
    for (int i = 0; i < numTests; ++i) {
        snprintf(debugText, 50, "Exposure is %f \n", mExposureValues[i]);
        strcat(mDebugText, debugText);
        for (int j = 0; j < numPatches; ++j) {
            int exposureRed = static_cast<int>((
                log(static_cast<float>(mReferenceColors[j].r()))
                / log(2.0f) * GAMMA_CORRECTION +
                mExposureValues[i] - minExposure) * scale);
            int exposureGreen = static_cast<int>((
                log(static_cast<float>(mReferenceColors[j].g()))
                / log(2.0f) * GAMMA_CORRECTION +
                mExposureValues[i] - minExposure) * scale);
            int exposureBlue = static_cast<int>((
                log(static_cast<float>(mReferenceColors[j].b()))
                / log(2.0f) * GAMMA_CORRECTION +
                mExposureValues[i] - minExposure) * scale);

            snprintf(debugText, 50, "%d %f %d %f %d %f \n",
                    exposureRed, mCheckerColors[i][j].r(),
                    exposureGreen, mCheckerColors[i][j].g(),
                    exposureBlue, mCheckerColors[i][j].b());

            ALOGV("%s", debugText);
            strcat(mDebugText, debugText);

            drawPoint(200 - exposureRed, mCheckerColors[i][j].r(), red);
            drawPoint(200 - exposureGreen, mCheckerColors[i][j].g(), green);
            drawPoint(200 - exposureBlue, mCheckerColors[i][j].b(), blue);
        }
    }
    mExposureValues.clear();
    mCheckerColors.clear();
}

void ExposureCompensationTest::initializeReferenceColors() {
    mReferenceColors.resize(6);

    mReferenceColors[0].set(243, 243, 242);
    mReferenceColors[1].set(200, 200, 200);
    mReferenceColors[2].set(160, 160, 160);
    mReferenceColors[3].set(122, 122, 121);
    mReferenceColors[4].set(85, 85, 85);
    mReferenceColors[5].set(52, 52, 52);
}
