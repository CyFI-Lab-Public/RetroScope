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

#define LOG_TAG "ImageTestHandler"
#include <utils/Log.h>
#include <utils/Timers.h>
#include <cmath>

#include "vec2.h"
#include "vec3.h"
#include "imagetesthandler.h"

void ImageTestHandler::initDebugImage() {
    mDebugOutput = NULL;
}

// Initializes the  debug image with a given height and width.
void ImageTestHandler::initDebugImage(int debugHeight,
                                      int debugWidth) {
    mDebugOutput = NULL;
    mDebugOutput = new unsigned char[debugHeight * debugWidth * 4];
    memset(mDebugOutput, 0, debugHeight * debugWidth * 4);

    mDebugHeight = debugHeight;
    mDebugWidth = debugWidth;
}

// Copies an existing image to the debug image.
void ImageTestHandler::copyDebugImage(int inputHeight, int inputWidth,
                                      const unsigned char* inputImage) {
    if ((inputHeight == mDebugHeight) && (inputWidth == mDebugWidth)) {
        ALOGV("Copying debug images");
        memcpy(mDebugOutput, inputImage, mDebugHeight * mDebugWidth * 4);
    }
}

void ImageTestHandler::clearDebugImage() {
    if (mDebugOutput != NULL) {
        delete[] mDebugOutput;
        mDebugOutput = new unsigned char[mDebugHeight * mDebugWidth * 4];
        memset(mDebugOutput, 0, mDebugHeight * mDebugWidth * 4);
    }
}


// Draws a point of a given color.
void ImageTestHandler::drawPoint(int row, int column, const Vec3i &color) {
    if ((row >= 0) && (column >= 0) &&
        (column < mDebugWidth) && (row < mDebugHeight)) {
        mDebugOutput[(row*mDebugWidth + column) * 4] = color.r();
        mDebugOutput[(row*mDebugWidth + column) * 4+1] = color.g();
        mDebugOutput[(row*mDebugWidth + column) * 4+2] = color.b();
        mDebugOutput[(row*mDebugWidth + column) * 4+3] = 255;
    }
}

// Draws a point in Vec2 format of a given color.
void ImageTestHandler::drawPoint(const Vec2i &point, const Vec3i &color) {
    drawPoint((int) point.y(), (int) point.x(), color);
}

// Draws a line of a given color.
void ImageTestHandler::drawLine(int angle, int radius, const Vec3i &color) {
    const int r = color.r();
    const int g = color.g();
    const int b = color.b();
    const int a = 255;

    int shiftedMin = -113;
    int shiftedMax = 83;

    float radiusDouble = static_cast<float>(radius);

    float angleRad = static_cast<float>(angle) * M_PI / 180.0;

    //ALOGV("draw line for (%d, %d)", angle, radius);
    for (int i = shiftedMin; i <= shiftedMax; ++i) {
        float j;

        assert(angle != 0);
        j = (i - radiusDouble / sin(angleRad)) * tan(angleRad);
        float x = (static_cast<float>(i) + j) / sqrt(2.0);
        float y = (j - static_cast<float>(i)) / sqrt(2.0);

        drawPoint(x, y, color);
    }
}
