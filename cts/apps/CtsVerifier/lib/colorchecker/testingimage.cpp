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

#define LOG_TAG "TestingImage"
#include <utils/Log.h>
#include <utils/Timers.h>
#include <string.h>
#include <cmath>
#include <vector>
#include <assert.h>
#include "vec3.h"

#include "testingimage.h"

const float GAMMA_CORRECTION = 2.2f;

// Constructs an instance with the given image byte array.
TestingImage::TestingImage(const unsigned char* inputImage,
                           int inputHeight, int inputWidth,
                           int inputChannel, int inputRowSpan) {
    mImage = new unsigned char[inputRowSpan * inputHeight];

    ALOGV("mImage format created! with size as %d, %d, %d",
         inputRowSpan, inputHeight, inputChannel);
    mWidth = inputWidth;
    mHeight = inputHeight;
    mChannels = inputChannel;
    mRowSpan = mWidth * mChannels;

    for (int i = 0; i < mHeight; ++i) {
        for (int j = 0; j < mWidth; ++j) {
            for (int k = 0; k < mChannels; ++k) {
                mImage[i * mRowSpan + j* mChannels + k] =
                        inputImage[i * inputRowSpan + j * inputChannel + k];
            }
        }
    }
    ALOGV("mImage converted!");
}

// Constructs an instance with the given image and resize it to a new size.
TestingImage::TestingImage(const unsigned char* inputImage,
                           int inputHeight, int inputWidth,
                           int inputChannel, int inputRowSpan,
                           int newHeight, int newWidth) {
    mImage = new unsigned char[newHeight * newWidth * inputChannel];

    ALOGV("mImage format created! with size as %d, %d, %d",
         newHeight, newWidth, inputChannel);
    mHeight = newHeight;
    mWidth = newWidth;
    mChannels = inputChannel;
    mRowSpan = mWidth * mChannels;

    // Computes how many pixels in the original image corresponds to one pixel
    // in the new image.
    int heightScale = inputHeight / newHeight;
    int widthScale = inputWidth / newWidth;

    // Average the corresponding pixels in the original image to compute the
    // pixel value of the new image.
    for (int i = 0; i < mHeight; ++i) {
        for (int j = 0; j < mWidth; ++j) {
            for (int k = 0; k < mChannels; ++k) {
                int pixelValue = 0;

                for (int l = 0; l < heightScale; ++l) {
                    for (int m = 0; m < widthScale; ++m) {
                        pixelValue += inputImage[
                                (i * heightScale + l) * inputRowSpan
                                + (j * widthScale + m) * inputChannel + k];
                    }
                }
                pixelValue = pixelValue / (heightScale * widthScale);
                mImage[i * mRowSpan + j * mChannels + k] =
                        (unsigned char) pixelValue;
            }
        }
    }
}

TestingImage::~TestingImage() {
    if (mImage!=NULL) {
        delete[] mImage;
    }
}

int TestingImage::getPixelValue(int row, int column, int channel) const {
    assert ((row >= 0) && (row < mHeight));
    assert ((column >= 0) && (column < mWidth));
    assert ((channel >= 0) && (channel < mChannels));
    return (int)mImage[row * mRowSpan + column * mChannels + channel];
}

Vec3i TestingImage::getPixelValue(int row, int column) const {
    Vec3i current_color(getPixelValue(row, column, 0),
                        getPixelValue(row, column, 1),
                        getPixelValue(row, column, 2));
    return current_color;
}

Vec3i TestingImage::getPixelValue(const Vec2i &pixelPosition) const {
    return getPixelValue(pixelPosition.x(), pixelPosition.y());
}

Vec3i TestingImage::getPixelValue(const Vec2f &pixelPosition) const {
    return getPixelValue(static_cast<int>(pixelPosition.x()),
                         static_cast<int>(pixelPosition.y()));
}

// Returns a vector of the colors in the requested block of color checkers.
// The vector is formatted by going through the block from left to right and
// from top to bottom.
const std::vector<Vec3f>* TestingImage::getColorChecker(
      int rowStart, int rowEnd, int columnStart, int columnEnd,
      const std::vector<std::vector< Vec2f > >* centerAddress,
      const std::vector<std::vector< float > >* radiusAddress) const {
    std::vector<Vec3f>* checkerColors = new std::vector<Vec3f>;

    // Average the pixel values of the pixels within the given radius to the
    // given center position.
    for (int i = rowStart; i < rowEnd; ++i) {
        for (int j = columnStart; j < columnEnd; ++j) {
            float radius = sqrt((*radiusAddress)[i][j]);
            Vec2f center((*centerAddress)[i][j].x(),
                               (*centerAddress)[i][j].y());
            Vec3f meanColor(0.f, 0.f, 0.f);
            int numPixels = 0;

            for (int ii = static_cast<int>(center.x() - radius);
                 ii < static_cast<int>(center.x() + radius); ++ii) {
                for (int jj = static_cast<int>(center.y() - radius);
                     jj < static_cast<int>(center.y() + radius); ++jj) {

                    Vec2i pixelPosition(ii,jj);
                    if (pixelPosition.squareDistance<float>(center) <
                        (*radiusAddress)[i][j]) {
                        meanColor = meanColor + getPixelValue(pixelPosition);
                        ++numPixels;
                    }
                }
            }
            meanColor = meanColor / numPixels;
            checkerColors->push_back(meanColor);
        }
    }

    return checkerColors;
}

bool TestingImage::rgbToGrayScale(unsigned char* grayLayer) const {
    if (mChannels == 4) {
        for (int i = 0; i < mWidth; i++) {
            for (int j = 0; j < mHeight; j++) {
                float redLinear = pow(getPixelValue(j, i, 0),
                                       GAMMA_CORRECTION);
                float greenLinear = pow(getPixelValue(j,i,1),
                                         GAMMA_CORRECTION);
                float blueLinear = pow(getPixelValue(j,i,2),
                                        GAMMA_CORRECTION);

                // Computes the luminance value
                grayLayer[j * mWidth + i] =
                        (unsigned char)((int)pow((0.299f * redLinear
                                                  + 0.587f * greenLinear
                                                  + 0.114f * blueLinear),
                                                  1/GAMMA_CORRECTION));
            }
        }

        return true;
    } else {

        return false;
    }
}
