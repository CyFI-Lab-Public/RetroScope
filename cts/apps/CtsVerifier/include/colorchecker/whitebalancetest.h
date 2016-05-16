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

#ifndef WHITEBALANCETEST_H
#define WHITEBALANCETEST_H

#include <vector>
#include <string>

#include "imagetesthandler.h"

class WhiteBalanceTest : public ImageTestHandler {
  public:
    WhiteBalanceTest() : ImageTestHandler() {}
    WhiteBalanceTest(int debugHeight, int debugWidth) :
            ImageTestHandler(debugHeight, debugWidth) {}
    ~WhiteBalanceTest() {}

    void addDataToList(const std::string &mode,
                       const std::vector<Vec3f>* checkerColors) {
        mMode = mode;
        mCheckerColors = *checkerColors;
        delete checkerColors;
    }

    void processData();

    int getCorrelatedColorTemp() const {
        return mCorrelatedColorTemp;
    }

    int getAutoTemp();

  private:
    int findCorrelatedColorTemp(const Vec3f &whitePoint);
    Vec3f initializeFromRGB(const Vec3f &rgb);
    float convertToLinear(float color);

    std::string mMode;
    std::vector<Vec3f> mCheckerColors;
    std::vector<Vec3f> mCheckerXyzColors;
    std::vector<Vec3f> mXyzColorsDaylight;
    int mCorrelatedColorTemp;
};

#endif
