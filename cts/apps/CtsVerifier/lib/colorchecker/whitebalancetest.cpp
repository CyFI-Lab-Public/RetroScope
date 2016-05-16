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

#define LOG_TAG "WhiteBalanceTest"
#include <utils/Log.h>
#include <utils/Timers.h>
#include <cmath>
#include <string>

#include "vec2.h"
#include "vec3.h"
#include "whitebalancetest.h"

// White point in XYZ color space under 5200k illumination.
const Vec3f kDaylightWhitePoint(0.9781f, 1.f, 0.9021f);

// Process the data of checker colors collected under different white balance.
// Assuming the Daylight CCT is set to 5200k, compute the CCT of other white
// balance modes.
void WhiteBalanceTest::processData() {
    ALOGV("Start Processing White Balance Test Data!");

    int numPatches = mCheckerColors.size();
    ALOGV("Processing %d tests with %d patches", 2, numPatches);

    std::vector<Vec3f> xyzColors(numPatches);
    for (int j = 0; j < numPatches; ++j) {
        Vec3f xyzCheckerColor = initializeFromRGB(mCheckerColors[j]);
        xyzColors[j] = xyzCheckerColor;
        ALOGV("XYZ coordinate is %f, %f, %f", xyzCheckerColor.r(),
              xyzCheckerColor.g(), xyzCheckerColor.b());
    }

    Vec3f meanScale(0.f, 0.f, 0.f);

    if (mMode == "daylight") {
        mXyzColorsDaylight = xyzColors;
        // For testing the auto white balance mode. Compute a CCT that would
        // map the gray checkers to a white point.
        for (int j = 1; j < numPatches; ++j) {
            meanScale = meanScale +
                    (mXyzColorsDaylight[j] / kDaylightWhitePoint);
        }
    } else {
        for (int j = 1; j < numPatches; ++j) {
            meanScale = meanScale + (mXyzColorsDaylight[j] / xyzColors[j]);
        }
    }

    meanScale = meanScale / (numPatches - 1);
    ALOGV("Scale: %f, %f, %f", meanScale.r(), meanScale.g(), meanScale.b());

    Vec3f whitePoint;
    whitePoint = meanScale * kDaylightWhitePoint;

    ALOGV("White point is %f, %f, %f", whitePoint.r(),
         whitePoint.g(), whitePoint.b());

    mCorrelatedColorTemp = findCorrelatedColorTemp(whitePoint);
    ALOGV("CCT is %d", mCorrelatedColorTemp);
}

// Given a white point, find the correlated color temperature.
// Formula taken from the paper "Calculating Correlated Color Temperatures
// Across the Entire Gamut of Daylight and Skylight Chromaticities" by Hernandez
// Andres et al. in 1999. The numbers are fitting parameters.
int WhiteBalanceTest::findCorrelatedColorTemp(const Vec3f &whitePoint) {
    Vec2f chromaOfWhitePoint(
        whitePoint.r() / (whitePoint.r() + whitePoint.g() + whitePoint.b()),
        whitePoint.g() / (whitePoint.r() + whitePoint.g() + whitePoint.b()));

    float n = (chromaOfWhitePoint.x() - 0.3366f)
                / (chromaOfWhitePoint.y() - 0.1735f);
    float y = -949.86315f + 6253.80338f * exp(-n / 0.92159f)
               + 28.70599f * exp(-n / 0.20039f) + 0.00004f * exp(-n / 0.07125f);

    return static_cast<int>(y);
}

// Converts a RGB pixel value to XYZ color space.
Vec3f WhiteBalanceTest::initializeFromRGB(const Vec3f &rgb) {
    float linearRed = convertToLinear(rgb.r());
    float linearGreen = convertToLinear(rgb.g());
    float linearBlue = convertToLinear(rgb.b());

    float x = 0.4124f * linearRed + 0.3576f * linearGreen +
            0.1805f * linearBlue;
    float y = 0.2126f * linearRed + 0.7152f * linearGreen +
            0.0722f * linearBlue;
    float z = 0.0193f * linearRed + 0.1192f * linearGreen +
            0.9505f * linearBlue;

    return Vec3f(x, y, z);
}

float WhiteBalanceTest::convertToLinear(float color) {
    float norm = color/ 255.0f;
    float linearColor;

    // Convert from sRGB space to linear RGB value
    if (norm > 0.04045f) {
        linearColor = pow(((norm + 0.055f) / 1.055f), 2.4f);
    } else {
        linearColor = norm / 12.92f;
    }

    return linearColor;
}
