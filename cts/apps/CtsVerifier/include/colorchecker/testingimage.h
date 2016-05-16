/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 ** Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef TESTINGIMAGE_H
#define TESTINGIMAGE_H

#include <vector>
#include "vec3.h"
#include "vec2.h"

// Implements a class for image representation.
class TestingImage {
  public:
    // Constructs a new instance with the inputImage's row and column counts.
    // No change in size.
    TestingImage(const unsigned char* inputImage,
                 int inputWidth, int inputHeight,
                 int inputChannels, int inputRowSpan);
    // Constructs a new instance with an inputImage but resize it to a given
    // new size.
    TestingImage(const unsigned char* inputImage,
                 int inputHeight, int inputWidth,
                 int inputChannel, int inputRowSpan,
                 int newHeight, int newWidth);
    virtual ~TestingImage();

    // Reads the pixel value of a given location.
    int getPixelValue(int row, int column, int channel) const;
    Vec3i getPixelValue(int row, int column) const;
    Vec3i getPixelValue(const Vec2i &pixelPosition) const;
    Vec3i getPixelValue(const Vec2f &pixelPosition) const;

    inline const unsigned char* getImage() const { return mImage; }
    inline int getWidth() const { return mWidth; }
    inline int getHeight() const { return mHeight; }
    inline int getChannels() const { return mChannels; }
    inline int getRowSpan() const { return mRowSpan; }

    // Reads the colors of a color checker in the image with the given checker
    // coordinates including centers and radius.
    const std::vector<Vec3f>* getColorChecker(
            int rowStart, int rowEnd, int columnStart, int columnEnd,
            const std::vector<std::vector< Vec2f > >* centerAddress,
            const std::vector<std::vector< float > >* radiusAddress) const;

    // Computes the luminance value of a color image.
    bool rgbToGrayScale(unsigned char* grayLayer) const;

  private:
    unsigned char* mImage;
    int mWidth;
    int mHeight;
    int mRowSpan;
    int mChannels;
};

#endif
