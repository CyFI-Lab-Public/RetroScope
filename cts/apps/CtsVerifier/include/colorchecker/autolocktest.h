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

#ifndef AUTOLOCKTEST_H
#define AUTOLOCKTEST_H

#include <vector>

#include "imagetesthandler.h"

class AutoLockTest : public ImageTestHandler {
  public:
    AutoLockTest() : ImageTestHandler() {}
    AutoLockTest(int debugHeight, int debugWidth) :
        ImageTestHandler(debugHeight, debugWidth) {}
    ~AutoLockTest() {}

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
    bool IsBrighterThan(const std::vector<Vec3f>* colorCheckers1,
                        const std::vector<Vec3f>* colorCheckers2) const;
    bool IsEquivalentTo(const std::vector<Vec3f>* colorCheckers1,
                        const std::vector<Vec3f>* colorCheckers2) const;

    std::vector<std::vector<Vec3f> > mCheckerColors;
    std::vector<bool> mComparisonResults;
    int mNumPatches;
};

#endif
