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

#ifndef METERINGTEST_H
#define METERINGTEST_H

#include <vector>

#include "imagetesthandler.h"

// Constructs a test handler for the metering test. The instance holds the pixel
// values of the gray checkers on the color checker under different metering
// settins. It can also compare two arrays of pixel values to tell whether one
// is brighter or equivalent or darker than the other one.
class MeteringTest : public ImageTestHandler {
  public:
    MeteringTest() : ImageTestHandler() {}
    MeteringTest(const int debugHeight, const int debugWidth) :
            ImageTestHandler(debugHeight, debugWidth) {}
    ~MeteringTest() {}

    void addDataToList(const std::vector<Vec3f>* checkerColors) {
        mCheckerColors.push_back(*checkerColors);
        delete checkerColors;
    }

    void processData();

    void clearData();

    const std::vector<bool>* getComparisonResults() const {
        return (&mComparisonResults);
    }

  private:
    bool isDarkerThan(const std::vector<Vec3f>* checkerColors1,
                      const std::vector<Vec3f>* checkerColors2) const;
    bool isEquivalentTo(const std::vector<Vec3f>* checkerColors1,
                        const std::vector<Vec3f>* checkerColors2) const;

    std::vector<std::vector<Vec3f> > mCheckerColors;
    std::vector<bool> mComparisonResults;
    int mNumPatches;
};

#endif
