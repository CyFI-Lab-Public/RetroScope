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

#ifndef COLORCHECKERTEST_H
#define COLORCHECKERTEST_H

#include <vector>
#include <string>

#include "testingimage.h"
#include "imagetesthandler.h"

class ColorCheckerTest : public ImageTestHandler {
public:
    ColorCheckerTest() : ImageTestHandler() {
        mImage = NULL;
        mSuccess = false;
    }
    ColorCheckerTest(int debugHeight, int inputWidth) :
            ImageTestHandler(debugHeight, inputWidth) {
        mImage = NULL;
        mSuccess = false;
    }
    ~ColorCheckerTest();

    void addTestingImage(TestingImage* inputImage);
    void processData();

    const std::vector<std::vector<Vec2f> >* getCheckerCenterAdd() const {
        std::vector<std::vector<Vec2f> >* returnPositions =
                new std::vector<std::vector<Vec2f> >(
                        4, std::vector<Vec2f>(6, Vec2f(0.f, 0.f)));

        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 6; ++j) {
                (*returnPositions)[i][j] = (*mMatchPositions[i][j]);
            }
        }
        return (returnPositions);
    }

    const std::vector<std::vector<float> >* getCheckerRadiusAdd() const {
        std::vector<std::vector<float> >* returnRadius=
              new std::vector<std::vector<float> >(
                      4, std::vector<float>(6, 0.f));

        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 6; ++j) {
                (*returnRadius)[i][j] = mMatchRadius[i][j];
            }
        }
        return (returnRadius);
    }

    bool getSuccess() const {
        return mSuccess;
    }

private:
    void initializeRefColor();

    void edgeDetection();
    void computeGradient(unsigned char* layer, float* gradientMap);
    void houghLineDetection(bool* edgeMap, float* gradientAbsolute,
                            float* gradientDirection);

    void findCheckerBoards(std::vector<std::vector<int> > linesDir1,
                           std::vector<std::vector<int> > linesDir2);
    Vec2f findCrossing(std::vector<int> line1, std::vector<int> line2);
    void findBestMatch(int i1, int i2, int j1, int j2);

    bool verifyPointPair(Vec2f pointUpperLeft, Vec2f pointBottomRight,
                         Vec2f* pointCenter, Vec3i* color);
    void verifyColorGrid();

    void fillRefColorGrid();

    TestingImage* mImage;

    std::vector<std::vector< Vec3i* > > mCandidateColors;
    std::vector<std::vector< Vec2f* > > mCandidatePositions;

    std::vector<std::vector< Vec3i* > > mReferenceColors;
    std::vector<std::vector< Vec2f* > > mMatchPositions;
    std::vector<std::vector< Vec3f* > > mMatchColors;
    std::vector<std::vector< float > > mMatchRadius;

    bool mSuccess;
};

#endif
