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

#ifndef EXPOSURECOMPENSATIONTEST_H
#define EXPOSURECOMPENSATIONTEST_H

#include <vector>

#include "imagetesthandler.h"

class ExposureCompensationTest : public ImageTestHandler {
  public:
    ExposureCompensationTest() : ImageTestHandler() {
        mDebugText = NULL;
        initializeReferenceColors();
    }
    ExposureCompensationTest(int debugHeight, int debugWidth) :
            ImageTestHandler(debugHeight, debugWidth) {
        mDebugText = NULL;
        initializeReferenceColors();
    }
    ~ExposureCompensationTest() {}

    void addDataToList(const float exposureValue,
                  const std::vector<Vec3f>* checkerColors) {
        mExposureValues.push_back(exposureValue);
        mCheckerColors.push_back(*checkerColors);
    }

    const char* getDebugText() {
        return mDebugText;
    }

    void processData();

  private:
    void initializeReferenceColors();

    std::vector<std::vector<Vec3f> > mCheckerColors;
    std::vector<Vec3i> mReferenceColors;
    std::vector<float> mExposureValues;

    char* mDebugText;
};

#endif
