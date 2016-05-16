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

#define LOG_TAG "ColorCheckerTest"
#include <utils/Log.h>
#include <utils/Timers.h>
#include <cmath>
#include <string>

#include "vec2.h"
#include "vec3.h"
#include "colorcheckertest.h"

const float GAMMA_CORRECTION = 2.2f;
const float COLOR_ERROR_THRESHOLD = 200.f;
ColorCheckerTest::~ColorCheckerTest() {
    ALOGV("Deleting color checker test handler");

    if (mImage != NULL) {
        delete mImage;
    }
    ALOGV("Image deleted");

    int numHorizontalLines = mCandidateColors.size();
    int numVerticalLines = mCandidateColors[0].size();

    for (int i = 0; i < numHorizontalLines; ++i) {
        for (int j = 0; j < numVerticalLines; ++j) {
            if (mCandidateColors[i][j] != NULL) {
                delete mCandidateColors[i][j];
            }
            if (mCandidatePositions[i][j] != NULL) {
                delete mCandidatePositions[i][j];
            }
        }
    }
    ALOGV("Candidates deleted!");

    for (int i = 0; i < 4; ++i) {
        for (int j = 0; j < 6; ++j) {
            if (mMatchPositions[i][j] != NULL) {
                delete mMatchPositions[i][j];
            }
            if (mReferenceColors[i][j] != NULL) {
                delete mReferenceColors[i][j];
            }
            if (mMatchColors[i][j] != NULL) {
                delete mMatchColors[i][j];
            }
        }
    }
}

// Adds a new image to the test handler.
void ColorCheckerTest::addTestingImage(TestingImage* inputImage) {
    if (mImage != NULL) {
        delete mImage;
    }
    mImage = NULL;
    ALOGV("Original image deleted");
    mImage = inputImage;

    if ((mImage->getHeight() == getDebugHeight()) &&
        (mImage->getWidth() == getDebugWidth())) {
        copyDebugImage(getDebugHeight(), getDebugWidth(), mImage->getImage());
    }
}

void ColorCheckerTest::processData() {
    mSuccess = false;
    initializeRefColor();
    edgeDetection();
}

void ColorCheckerTest::initializeRefColor() {
    mReferenceColors.resize(4, std::vector<Vec3i*>(6, NULL));
    mMatchPositions.resize(4, std::vector<Vec2f*>(6, NULL));
    mMatchColors.resize(4, std::vector<Vec3f*>(6, NULL));
    mMatchRadius.resize(4, std::vector<float>(6, 0.f));

    mReferenceColors[0][0]= new Vec3i(115, 82, 68);
    mReferenceColors[0][1]= new Vec3i(194, 150, 130);
    mReferenceColors[0][2]= new Vec3i(98, 122, 157);
    mReferenceColors[0][3]= new Vec3i(87, 108, 67);
    mReferenceColors[0][4]= new Vec3i(133, 128, 177);
    mReferenceColors[0][5]= new Vec3i(103, 189, 170);
    mReferenceColors[1][0]= new Vec3i(214, 126, 44);
    mReferenceColors[1][1]= new Vec3i(80, 91, 166);
    mReferenceColors[1][2]= new Vec3i(193, 90, 99);
    mReferenceColors[1][3]= new Vec3i(94,  60, 108);
    mReferenceColors[1][4]= new Vec3i(157, 188, 64);
    mReferenceColors[1][5]= new Vec3i(224, 163, 46);
    mReferenceColors[2][0]= new Vec3i(56, 61, 150);
    mReferenceColors[2][1]= new Vec3i(70, 148, 73);
    mReferenceColors[2][2]= new Vec3i(175, 54, 60);
    mReferenceColors[2][3]= new Vec3i(231, 199, 31);
    mReferenceColors[2][4]= new Vec3i(187, 86, 149);
    mReferenceColors[2][5]= new Vec3i(8, 133, 161);
    mReferenceColors[3][0]= new Vec3i(243, 243, 242);
    mReferenceColors[3][1]= new Vec3i(200, 200, 200);
    mReferenceColors[3][2]= new Vec3i(160, 160, 160);
    mReferenceColors[3][3]= new Vec3i(122, 122, 121);
    mReferenceColors[3][4]= new Vec3i(85, 85, 85);
    mReferenceColors[3][5]= new Vec3i(52, 52, 52);
}

void ColorCheckerTest::edgeDetection() {
    int width = mImage->getWidth();
    int height = mImage->getHeight();

    bool* edgeMap = new bool[height * width];
    unsigned char* grayImage = new unsigned char[height * width];

    // If the image is a color image and can be converted to a luminance layer
    if (mImage->rgbToGrayScale(grayImage)) {
        float* gradientMap = new float[height * width * 2];

        // Computes the gradient image on the luminance layer.
        computeGradient(grayImage, gradientMap);

        float* gradientMagnitude = new float[height * width];
        int* gradientDirectionInt = new int[height * width];
        float* gradientDirection = new float[height * width];

        // Computes the absolute gradient of the image without padding.
        for (int i = 1; i < height - 1; ++i) {
            for (int j = 1; j < width - 1; ++j) {
                gradientMagnitude[i * width + j] =
                        sqrt(gradientMap[(i * width + j) * 2] *
                             gradientMap[(i * width + j) * 2] +
                             gradientMap[(i * width + j ) * 2 + 1] *
                             gradientMap[(i * width + j ) * 2 + 1]);

                // Computes the gradient direction of the image.
                if (gradientMap[(i * width + j) * 2] == 0 ) {
                    // If the vertical gradient is 0, the edge is horizontal
                    // Mark the gradient direction as 90 degrees.
                    gradientDirectionInt[i * width + j] = 2;
                    gradientDirection[i * width + j] = 90.0f;
                } else {
                    // Otherwise the atan operation is valid and can decide
                    // the gradient direction of the edge.
                    float gradient = atan(gradientMap[(i * width + j) * 2 + 1]
                            / gradientMap[(i * width + j) * 2])
                            / (M_PI / 4);

                    gradientDirection[i * width + j] = gradient * 45.0f;

                    // Maps the gradient direction to 4 major directions with
                    // 0 mapped to up and 2 mapped to right.
                    if (gradient - floor(gradient) > 0.5) {
                        gradientDirectionInt[i * width + j] =
                                (static_cast<int>(ceil(gradient)) + 4) % 4;
                    } else {
                        gradientDirectionInt[i * width + j] =
                                (static_cast<int>(floor(gradient)) + 4) % 4;
                    }
                }
            }
        }

        // Compute a boolean map to show whether a pixel is on the edge.
        for (int i = 1; i < height - 1; ++i) {
            for (int j = 1; j < width - 1; ++j) {
                edgeMap[i * width + j] = false;

                switch (gradientDirectionInt[i * width + j]) {
                    case 0:
                        // If the gradient points rightwards, the pixel is
                        // on an edge if it has a larger absolute gradient than
                        // pixels on its left and right sides.
                        if ((gradientMagnitude[i * width + j] >=
                                gradientMagnitude[i * width + j + 1]) &&
                            (gradientMagnitude[i * width + j] >=
                                gradientMagnitude[i * width + j - 1])) {
                            edgeMap[i * width + j] = true;
                        }
                        break;
                    case 1:
                        // If the gradient points right-downwards, the pixel is
                        // on an edge if it has a larger absolute gradient than
                        // pixels on its upper left and bottom right sides.
                        if ((gradientMagnitude[i * width + j] >=
                                gradientMagnitude[(i + 1) * width + j + 1]) &&
                            (gradientMagnitude[i * width + j] >=
                                gradientMagnitude[(i - 1) * width + j - 1])) {
                            edgeMap[i * width + j] = true;
                        }
                        break;
                    case 2:
                        // If the gradient points upwards, the pixel is
                        // on an edge if it has a larger absolute gradient than
                        // pixels on its up and down sides.
                        if ((gradientMagnitude[i * width + j] >=
                                gradientMagnitude[(i + 1) * width + j]) &&
                            (gradientMagnitude[i * width + j] >=
                                gradientMagnitude[(i - 1) * width + j])) {
                            edgeMap[i * width + j] = true;
                        }
                        break;
                    case 3:
                        // If the gradient points right-upwards, the pixel is
                        // on an edge if it has a larger absolute gradient than
                        // pixels on its bottom left and upper right sides.
                        if ((gradientMagnitude[i * width + j] >=
                                gradientMagnitude[(i - 1) * width + j + 1]) &&
                            (gradientMagnitude[i * width + j] >=
                                gradientMagnitude[(i + 1) * width + j - 1])) {
                            edgeMap[i * width + j] = true;
                        }
                  }

             }
        }

        houghLineDetection(edgeMap, gradientMagnitude, gradientDirection);

        // Cleans up
        delete[] gradientMap;
        delete[] gradientDirectionInt;
        delete[] gradientMagnitude;
        delete[] gradientDirection;

    } else {
        ALOGE("Not a color image!");
    }

    delete[] edgeMap;
    delete[] grayImage;
}

// Runs the hough voting algorithm to find the grid of the color checker
// with the edge map, gradient direction and gradient magnitude as inputs.
void ColorCheckerTest::houghLineDetection(bool* edgeMap,
                                          float* gradientMagnitude,
                                          float* gradientDirection) {
    // Constructs a graph for Hough voting. The vertical axis counts the vote
    // for a certain angle. The horizontal axis counts the vote for the distance
    // of a line from the origin of the image.
    int houghHeight = 180;
    int houghWidth = 200;
    int houghCounts[houghHeight][houghWidth];
    int houghSum[houghHeight][houghWidth];

    int** houghVote;
    houghVote = new int*[180];
    for (int i = 0; i < 180; ++i) {
        houghVote[i] = new int[200];
    }

    for (int i = 0; i < houghHeight; ++i) {
        for (int j = 0; j < houghWidth; ++j) {
            houghCounts[i][j] = 0;
            houghVote[i][j] = 0;
            houghSum[i][j] = 0;
        }
    }

    // Vectors to record lines in two orthogonal directions.
    // Each line is represented by its direction and its distance to the origin.
    std::vector<std::vector<int> > verticalLines;
    std::vector<std::vector<int> > horizontalLines;
    float radius;
    int height = mImage->getHeight();
    int width = mImage->getWidth();

    // Processes the signicant edge pixels and cast vote for the corresponding
    // edge passing this pixel.
    for (int i = 1; i < height - 1; ++i) {
        for (int j = 1; j < width - 1; ++j) {
            // Sets threashold for the gradient magnitude to discount noises
            if (edgeMap[i * width + j] &&
                (gradientMagnitude[i * width + j] > 15)) {
                int shiftedAngle;

                // Shifts angles for 45 degrees so the vertical and horizontal
                // direction is mapped to 45 and 135 degrees to avoid padding.
                // This uses the assumption that the color checker is placed
                // roughly parallel to the image boarders. So that the edges
                // at the angle of 45 will be rare.
                shiftedAngle = (static_cast<int>(
                        -gradientDirection[i * width + j]) + 225 % 180);
                float shiftedAngleRad = static_cast<float>(shiftedAngle)
                        * M_PI / 180.0f;

                // Computes the distance of the line from the origin.
                float a, b;
                a = static_cast<float>(i - j) / sqrt(2.0f);
                b = static_cast<float>(i + j) / sqrt(2.0f);
                radius = a * sin(shiftedAngleRad) - b * cos(shiftedAngleRad);

                // Adds one vote for the line. The line's angle is shifted by
                // 45 degrees to avoid avoid padding for the vertical lines,
                // which is more common than diagonal lines. The line's
                // distance is mapped to [0, 200] from [-200, 200].
                ++houghCounts[shiftedAngle][static_cast<int>((radius / 2.0f) +
                                                              100.0f)];

                drawPoint(i, j, Vec3i(255, 255, 255));
            }
        }
    }

    int houghAngleSum[houghHeight];
    int primaryVerticalAngle, primaryHorizontalAngle;
    int max1 = 0;
    int max2 = 0;

    // Looking for the two primary angles of the lines.
    for (int i = 5; i < houghHeight - 5; ++i) {
        houghAngleSum[i] = 0;
        for (int j = 0; j < houghWidth; ++j) {
            for (int l = -5; l <= 5; ++l) {
                houghSum[i][j] += houghCounts[i + l][j];
            }
            houghAngleSum[i] += houghSum[i][j];
        }

        if ((i < houghHeight / 2) && (houghAngleSum[i] > max1)) {
            max1 = houghAngleSum[i];
            primaryVerticalAngle = i;
        } else if ((i > houghHeight / 2) && (houghAngleSum[i] > max2)) {
            max2 = houghAngleSum[i];
            primaryHorizontalAngle = i;
        }
    }

    ALOGV("Primary angles are %d, %d",
         primaryVerticalAngle, primaryHorizontalAngle);

    int angle;

    // For each primary angle, look for the highest voted lines.
    for (int k = 0; k < 2; ++k) {
        if (k == 0) {
            angle = primaryVerticalAngle;
        } else {
            angle = primaryHorizontalAngle;
        }

        std::vector<int> line(2);
        for (int j = 2; j < houghWidth - 2; ++j) {
            houghVote[angle][j] = houghSum[angle][j];
            houghSum[angle][j] = 0;
        }

        // For each radius, average the vote with nearby ones.
        for (int j = 2; j < houghWidth - 2; ++j) {
            for (int m = -2; m <= 2; ++m) {
                houghSum[angle][j] += houghVote[angle][j + m];
            }
        }

        bool isCandidate[houghWidth];

        // Find whether a lines is a candidate by rejecting the ones that have
        // lower vote than others in the neighborhood.
        for (int j = 2; j < houghWidth - 2; ++j) {
            isCandidate[j] = true;
            for (int m = -2; ((isCandidate[j]) && (m <= 2)); ++m) {
                if ((houghSum[angle][j] < 20) ||
                    (houghSum[angle][j] < houghSum[angle][j + m])) {
                    isCandidate[j] = false;
                }
            }
        }

        int iter1 = 0;
        int iter2 = 0;
        int count = 0;

        // Finds the lines that are not too close to each other and add to the
        // detected lines.
        while (iter2 < houghWidth) {
            while ((!isCandidate[iter2]) && (iter2 < houghWidth)) {
                ++iter2;
            }
            if ((isCandidate[iter2]) && (iter2 - iter1 < 5)) {
                iter1 = (iter2 + iter1) / 2;
                ++iter2;
            } else {
                line[0] = angle;
                line[1] = (iter1 - 100) * 2;
                if (iter1 != 0) {
                    if (k == 0) {
                        verticalLines.push_back(line);
                        Vec3i color(verticalLines.size() * 20, 0, 0);
                        drawLine(line[0], line[1], color);
                    } else {
                        horizontalLines.push_back(line);
                        Vec3i color(0, horizontalLines.size() * 20, 0);
                        drawLine(line[0], line[1], color);
                    }
                }
                iter1 = iter2;
                ++iter2;
                ALOGV("pushing back line %d %d", line[0], line[1]);
            }
        }
    }

    ALOGV("Numbers of lines in each direction is %d, %d",
         verticalLines.size(), horizontalLines.size());

    for (int i = 0; i < 180; ++i) {
        delete[] houghVote[i];
    }
    delete[] houghVote;

    findCheckerBoards(verticalLines, horizontalLines);
}

// Computes the gradient in both x and y direction of a layer
void ColorCheckerTest::computeGradient(unsigned char* layer,
                                       float* gradientMap) {
    int width = mImage->getWidth();
    int height = mImage->getHeight();

    // Computes the gradient in the whole image except the image boarders.
    for (int i = 1; i < height - 1; ++i) {
        for (int j = 1; j < width - 1; ++j) {
            gradientMap[(i * width + j) * 2] =
                    0.5f * (layer[i * width + j + 1] -
                            layer[i * width + j - 1]);
            gradientMap[(i * width + j) * 2 + 1] =
                    0.5f * (layer[(i + 1) * width + j] -
                           layer[(i - 1) * width + j]);
        }
    }
}

// Tries to find the checker boards with the highest voted lines
void ColorCheckerTest::findCheckerBoards(
        std::vector<std::vector<int> > verticalLines,
        std::vector<std::vector<int> > horizontalLines) {
    ALOGV("Start looking for Color checker");

    int numHorizontalLines = mCandidateColors.size();
    int numVerticalLines;
    if (numHorizontalLines > 0) {
        numVerticalLines = mCandidateColors[0].size();
        for (int i = 0; i < numHorizontalLines; ++i) {
            for (int j = 0; j < numVerticalLines; ++j) {
                if (mCandidateColors[i][j] != NULL) {
                    delete mCandidateColors[i][j];
                }
                if (mCandidatePositions[i][j] != NULL) {
                    delete mCandidatePositions[i][j];
                }
            }
            mCandidateColors[i].clear();
            mCandidatePositions[i].clear();
        }
    }
    mCandidateColors.clear();
    mCandidatePositions.clear();

    ALOGV("Candidates deleted!");

    numVerticalLines = verticalLines.size();
    numHorizontalLines = horizontalLines.size();
    Vec2f pointUpperLeft;
    Vec2f pointBottomRight;

    mCandidateColors.resize(numHorizontalLines - 1);
    mCandidatePositions.resize(numHorizontalLines - 1);

    for (int i = numVerticalLines - 1; i >= 1; --i) {
        for (int j = 0; j < numHorizontalLines - 1; ++j) {
            // Finds the upper left and bottom right corner of each rectangle
            // formed by two neighboring highest voted lines.
            pointUpperLeft = findCrossing(verticalLines[i], horizontalLines[j]);
            pointBottomRight = findCrossing(verticalLines[i - 1],
                                            horizontalLines[j + 1]);

            Vec3i* color = new Vec3i();
            Vec2f* pointCenter = new Vec2f();
            // Verifies if they are separated by a reasonable distance.
            if (verifyPointPair(pointUpperLeft, pointBottomRight,
                                pointCenter, color)) {
                mCandidatePositions[j].push_back(pointCenter);
                mCandidateColors[j].push_back(color);
                ALOGV("Color at (%d, %d) is (%d, %d, %d)", j, i,color->r(), color->g(), color->b());

            } else {
                mCandidatePositions[j].push_back(NULL);
                mCandidateColors[j].push_back(NULL);
                delete color;
                delete pointCenter;
            }
        }
    }

    ALOGV("Candidates Number (%d, %d)", mCandidateColors.size(), mCandidateColors[0].size());
    // Verifies whether the current line candidates form a valid color checker.
    verifyColorGrid();
}

// Returns the corssing point of two lines given the lines.
Vec2f ColorCheckerTest::findCrossing(std::vector<int> line1,
                                     std::vector<int> line2) {
    Vec2f crossingPoint;
    float r1 = static_cast<float>(line1[1]);
    float r2 = static_cast<float>(line2[1]);
    float ang1, ang2;
    float y1, y2;

    ang1 = static_cast<float>(line1[0]) / 180.0f * M_PI;
    ang2 = static_cast<float>(line2[0]) / 180.0f * M_PI;

    float x, y;
    x = (r1 * cos(ang2) - r2 * cos(ang1)) / sin(ang1 - ang2);
    y = (r1 * sin(ang2) - r2 * sin(ang1)) / sin(ang1 - ang2);

    crossingPoint.set((x + y) / sqrt(2.0), (y - x) / sqrt(2.0));

    //ALOGV("Crosspoint at (%f, %f)", crossingPoint.x(), crossingPoint.y());
    return crossingPoint;
}

// Verifies whether two opposite corners on a quadrilateral actually can be
// the two corners of a color checker.
bool ColorCheckerTest::verifyPointPair(Vec2f pointUpperLeft,
                                       Vec2f pointBottomRight,
                                       Vec2f* pointCenter,
                                       Vec3i* color) {
    bool success = true;

    /** 5 and 30 are the threshold tuned for resolution 640*480*/
    if ((pointUpperLeft.x() < 0) ||
        (pointUpperLeft.x() >= mImage->getHeight()) ||
        (pointUpperLeft.y() < 0) ||
        (pointUpperLeft.y() >= mImage->getWidth()) ||
        (pointBottomRight.x() < 0) ||
        (pointBottomRight.x() >= mImage->getHeight()) ||
        (pointBottomRight.y() < 0) ||
        (pointBottomRight.y() >= mImage->getWidth()) ||
        (abs(pointUpperLeft.x() - pointBottomRight.x()) <= 5) ||
        (abs(pointUpperLeft.y() - pointBottomRight.y()) <= 5) ||
        (abs(pointUpperLeft.x() - pointBottomRight.x()) >= 30) ||
        (abs(pointUpperLeft.y() - pointBottomRight.y()) >= 30)) {

        // If any of the quadrilateral corners are out of the image or if
        // the distance between them are too large or too big, the quadrilateral
        // could not be one of the checkers
        success = false;
    } else {
        // Find the checker center if the corners of the rectangle meet criteria
        pointCenter->set((pointUpperLeft.x() + pointBottomRight.x()) / 2.0f,
                       (pointUpperLeft.y() + pointBottomRight.y()) / 2.0f);
        color->set(mImage->getPixelValue(*pointCenter).r(),
                   mImage->getPixelValue(*pointCenter).g(),
                   mImage->getPixelValue(*pointCenter).b());
        ALOGV("Color at (%f, %f) is (%d, %d, %d)", pointCenter->x(), pointCenter->y(),color->r(), color->g(), color->b());
    }
    return success;
}

// Verifies the color checker centers and finds the match between the detected
// color checker and the reference MacBeth color checker
void ColorCheckerTest::verifyColorGrid() {
    ALOGV("Start looking for Color Grid");
    int numHorizontalLines = mCandidateColors.size();
    int numVerticalLines = mCandidateColors[0].size();
    bool success = false;

    // Computes the standard deviation of one row/column of the proposed color
    // checker. Discards the row/column if the std is below a threshold.
    for (int i = 0; i < numHorizontalLines; ++i) {
        Vec3f meanColor(0.f, 0.f, 0.f);
        int numNonZero = 0;

        for (int j = 0; j < numVerticalLines; ++j) {
            if (mCandidateColors[i][j] != NULL) {
                ALOGV("candidate color (%d, %d) is (%d, %d, %d)", i, j, mCandidateColors[i][j]->r(), mCandidateColors[i][j]->g(), mCandidateColors[i][j]->b());

                meanColor = meanColor + (*mCandidateColors[i][j]);
                ++numNonZero;
            }
        }
        if (numNonZero > 0) {
            meanColor = meanColor / numNonZero;
        }
        ALOGV("Mean color for vertical direction computed!");

        float std = 0;
        for (int j = 0; j < numVerticalLines; ++j) {
            if (mCandidateColors[i][j] != NULL) {
                std += mCandidateColors[i][j]->squareDistance<float>(meanColor);
            }
        }
        if (numNonZero > 0) {
            std = sqrt(std / (3 * numNonZero));
        }
        ALOGV("st. deviation for the %d dir1 is %d", i, static_cast<int>(std));

        if ((std <= 30) && (numNonZero > 1)) {
            for (int j = 0; j < numVerticalLines; ++j) {
                if (mCandidateColors[i][j] != NULL) {
                    delete mCandidateColors[i][j];
                    mCandidateColors[i][j] = NULL;
                }
            }
        }
    }

    // Discards the column/row of the color checker if the std is below a
    // threshold.
    for (int j = 0; j < numVerticalLines; ++j) {
        Vec3f meanColor(0.f, 0.f, 0.f);
        int numNonZero = 0;

        for (int i = 0; i < numHorizontalLines; ++i) {
            if (mCandidateColors[i][j] != NULL) {
                meanColor = meanColor + (*mCandidateColors[i][j]);
                ++numNonZero;
            }
        }
        if (numNonZero > 0) {
            meanColor = meanColor / numNonZero;
        }

        float std = 0;
        for (int i = 0; i < numHorizontalLines; ++i) {
            if (mCandidateColors[i][j] != NULL) {
                std += mCandidateColors[i][j]->squareDistance<float>(meanColor);
            }
        }
        if (numNonZero > 0) {
            std = sqrt(std / (3 * numNonZero));
        }

        ALOGV("st. deviation for the %d dir2 is %d", j, static_cast<int>(std));

        if ((std <= 30) && (numNonZero > 1)) {
            for (int i = 0; i < numHorizontalLines; ++i) {
                if (mCandidateColors[i][j] != NULL) {
                    delete mCandidateColors[i][j];
                    mCandidateColors[i][j] = NULL;
                }
            }
        }
    }

    for (int i = 0; i < numHorizontalLines; ++i) {
        for (int j = 0; j < numVerticalLines; ++j) {
            if (mCandidateColors[i][j] != NULL) {
                ALOGV("position (%d, %d) is at (%f, %f) with color (%d, %d, %d)",
                     i, j,
                     mCandidatePositions[i][j]->x(),
                     mCandidatePositions[i][j]->y(),
                     mCandidateColors[i][j]->r(),
                     mCandidateColors[i][j]->g(),
                     mCandidateColors[i][j]->b());
            } else {
                ALOGV("position (%d, %d) is 0", i, j);
            }
        }
    }

    // Finds the match between the detected color checker and the reference
    // MacBeth color checker.
    int rowStart = 0;
    int rowEnd = 0;

    // Loops until all dectected color checker has been processed.
    while (!success) {
        int columnStart = 0;
        int columnEnd = 0;
        bool isRowStart = false;
        bool isRowEnd = true;

        // Finds the row start of the next block of detected color checkers.
        while ((!isRowStart) && (rowStart <  numHorizontalLines)) {
            for (int j = 0; j < numVerticalLines; ++j) {
                if (mCandidateColors[rowStart][j] != NULL) {
                    isRowStart = true;
                }
            }
            ++rowStart;
        }
        rowStart--;
        rowEnd = rowStart;
        ALOGV("rowStart is %d", rowStart);

        // Finds the row end of the next block of detected color checkers.
        while ((isRowEnd) && (rowEnd < numHorizontalLines)) {
            isRowEnd = false;
            for (int j = 0; j < numVerticalLines; ++j) {
                if (mCandidateColors[rowEnd][j] != NULL) {
                    isRowEnd= true;
                }
            }
            if (isRowEnd) {
                ++rowEnd;
            }
        }
        if ((!isRowEnd) && isRowStart) {
            rowEnd--;
        }
        if ((isRowEnd) && (rowEnd == numHorizontalLines)) {
            rowEnd--;
            isRowEnd = false;
        }
        ALOGV("rowEnd is %d", rowEnd);

        // Matches color checkers between the start row and the end row.
        bool successVertical = false;

        while (!successVertical) {
            bool isColumnEnd = true;
            bool isColumnStart = false;

            // Finds the start column of the next block of color checker
            while ((!isColumnStart) && (columnStart < numVerticalLines)) {
                if (mCandidateColors[rowStart][columnStart] != NULL) {
                    isColumnStart = true;
                }
                ++columnStart;
            }
            columnStart--;
            columnEnd = columnStart;

            // Finds the end column of the next block of color checker
            while ((isColumnEnd) && (columnEnd < numVerticalLines)) {
                isColumnEnd = false;
                if (mCandidateColors[rowStart][columnEnd] != NULL)
                    isColumnEnd = true;
                if (isColumnEnd) {
                    ++columnEnd;
                }
            }

            if ((!isColumnEnd) && isColumnStart) {
                columnEnd--;
            }
            if ((isColumnEnd) && (columnEnd == numVerticalLines)) {
                columnEnd--;
                isColumnEnd = false;
            }

            // Finds the best match on the MacBeth reference color checker for
            // the continuous block of detected color checker
            if (isRowStart && (!isRowEnd) &&
                isColumnStart && (!isColumnEnd)) {
                findBestMatch(rowStart, rowEnd, columnStart, columnEnd);
            }
            ALOGV("FindBestMatch for %d, %d, %d, %d", rowStart,
                 rowEnd, columnStart, columnEnd);

            // If the column search finishes, go out of the loop
            if (columnEnd >= numVerticalLines - 1) {
                successVertical = true;
            } else {
                columnStart = columnEnd + 1;
            }
        }
        ALOGV("Continuing to search for direction 1");

        // If the row search finishes, go out of the loop
        if (rowEnd >= numHorizontalLines - 1) {
            success = true;
        } else {
            rowStart = rowEnd + 1;
        }
    }

    for (int i = 0; i < 4; ++i) {
        for (int j = 0; j < 6; ++j) {
            if (mMatchPositions[i][j] != NULL) {
                ALOGV("Reference Match position for (%d, %d) is (%f, %f)", i, j,
                     mMatchPositions[i][j]->x(), mMatchPositions[i][j]->y());
            }
        }
    }

    fillRefColorGrid();
}

// Finds the best match on the MacBeth color checker for the continuous block of
// detected color checkers bounded by row i1, row i2 and column j1 and column j2
// Assumes that the subsample is less than 4*6.
void ColorCheckerTest::findBestMatch(int i1, int i2, int j1, int j2) {
    int numHorizontalGrid = i2 - i1 + 1;
    int numVerticalGrid = j2 - j1 + 1;

    if (((numHorizontalGrid > 1) || (numVerticalGrid > 1)) &&
        (numHorizontalGrid <= 4) && (numVerticalGrid <= 6)) {
        ALOGV("i1, j2, j1, j2 is %d, %d, %d, %d", i1, i2, j1, j2);
        float minError;
        float error = 0.f;
        int horizontalMatch, verticalMatch;

        // Finds the match start point where the error is minimized.
        for (int i = 0; i < numHorizontalGrid; ++i) {
            for (int j = 0; j < numVerticalGrid; ++j) {
                if (mCandidateColors[i1 + i][j1 + j] != NULL) {
                    error += mCandidateColors[i1 + i][j1 + j]->squareDistance<int>(
                            *mReferenceColors[i][j]);
                }
            }
        }
        ALOGV("Error is %f", error);
        minError = error;
        horizontalMatch = 0;
        verticalMatch = 0;

        for (int i = 0; i <= 4 - numHorizontalGrid; ++i) {
            for (int j = 0; j <= 6 - numVerticalGrid; ++j) {
                error = 0.f;

                for (int id = 0; id < numHorizontalGrid; ++id) {
                    for (int jd = 0; jd < numVerticalGrid; ++jd) {
                        if (mCandidateColors[i1 + id][j1 + jd] != NULL) {
                            error += mCandidateColors[i1 + id][j1 + jd]->
                                    squareDistance<int>(
                                            *mReferenceColors[i + id][j + jd]);
                        }
                    }
                }

                if (error < minError) {
                    minError = error;
                    horizontalMatch = i;
                    verticalMatch = j;
                }
                ALOGV("Processed %d, %d and error is %f", i, j, error );
            }
        }

        for (int id = 0; id < numHorizontalGrid; ++id) {
            for (int jd = 0; jd < numVerticalGrid; ++jd) {
                if (mCandidatePositions[i1 + id][j1 + jd] != NULL) {
                    mMatchPositions[horizontalMatch + id][verticalMatch + jd] =
                            new Vec2f(mCandidatePositions[i1 + id][j1 + jd]->x(),
                                      mCandidatePositions[i1 + id][j1 + jd]->y());
                }
            }
        }
        ALOGV("Grid match starts at %d, %d", horizontalMatch, verticalMatch);
    }
}

// Finds the boundary of a color checker by its color similarity to the center.
// Also predicts the location of unmatched checkers.
void ColorCheckerTest::fillRefColorGrid() {
    int rowStart = 0;
    int columnStart = 0;
    bool foundStart = true;

    for (int i = 0; (i < 4) && foundStart; ++i) {
        for (int j = 0; (j < 6) && foundStart; ++j) {
            if (mMatchPositions[i][j] != NULL) {
                rowStart = i;
                columnStart = j;
                foundStart = false;
            }
        }
    }
    ALOGV("First match found at (%d, %d)", rowStart, columnStart);

    float rowDistance, columnDistance;
    rowDistance = 0;
    columnDistance = 0;
    int numRowGrids = 0;
    int numColumnGrids = 0;

    for (int i = rowStart; i < 4; ++i) {
        for (int j = columnStart; j < 6; ++j) {
            if (mMatchPositions[i][j] != NULL) {
                if (i > rowStart) {
                    ++numRowGrids;
                    rowDistance += (mMatchPositions[i][j]->x() -
                                mMatchPositions[rowStart][columnStart]->x()) /
                                static_cast<float>(i - rowStart);
                }
                if (j > columnStart) {
                    ++numColumnGrids;
                    columnDistance += (mMatchPositions[i][j]->y() -
                                mMatchPositions[rowStart][columnStart]->y()) /
                                static_cast<float>(j - columnStart);
                }
            }
        }
    }

    if ((numRowGrids > 0) && (numColumnGrids > 0)) {
        rowDistance = rowDistance / numRowGrids;
        columnDistance = columnDistance / numColumnGrids;
        ALOGV("delta is %f, %f", rowDistance, columnDistance);

        for (int i = 0; i < 4; ++i) {
            for (int j = 0 ; j < 6; ++j) {
                if (mMatchPositions[i][j] == NULL) {
                    mMatchPositions[i][j] = new Vec2f(
                            mMatchPositions[rowStart][columnStart]->x() +
                                    (i - rowStart) * rowDistance,
                            mMatchPositions[rowStart][columnStart]->y() +
                                    (j - columnStart) * columnDistance);
                }
            }
        }
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 6; ++j) {
                float radius = 0;
                Vec3i color = mImage->getPixelValue(*mMatchPositions[i][j]);
                Vec3f meanColor(0.f , 0.f, 0.f);

                int numPixels = 0;
                for (int ii  = static_cast<int>(mMatchPositions[i][j]->x() -
                                                rowDistance/2);
                     ii <= static_cast<int>(mMatchPositions[i][j]->x() +
                                            rowDistance/2);
                     ++ii) {
                    for (int jj = static_cast<int>(mMatchPositions[i][j]->y() -
                                                   columnDistance/2);
                         jj <= static_cast<int>(mMatchPositions[i][j]->y() +
                                                columnDistance/2);
                         ++jj) {
                        if ((ii >= 0) && (ii < mImage->getHeight()) &&
                            (jj >= 0) && (jj < mImage->getWidth())) {
                            Vec3i pixelColor = mImage->getPixelValue(ii,jj);
                            float error = color.squareDistance<int>(pixelColor);

                            if (error < COLOR_ERROR_THRESHOLD) {
                                drawPoint(ii, jj, *mReferenceColors[i][j]);
                                meanColor = meanColor + pixelColor;
                                numPixels++;
                                Vec2i pixelPosition(ii, jj);

                                if (pixelPosition.squareDistance<float>(
                                        *mMatchPositions[i][j]) > radius) {
                                    radius = pixelPosition.squareDistance<float>(
                                            *mMatchPositions[i][j]);
                                }
                            }
                        }
                    }
                }

                /** Computes the radius of the checker.
                 * The above computed radius is the squared distance to the
                 * furthest point with a matching color. To be conservative, we
                 * only consider an area with radius half of the above computed
                 * value. Since radius is computed as a squared root, the one
                 * that will be recorded is 1/4 of the above computed value.
                 */
                mMatchRadius[i][j] = radius / 4.f;
                mMatchColors[i][j] = new Vec3f(meanColor / numPixels);

                ALOGV("Reference color at (%d, %d) is (%d, %d, %d)", i, j,
                     mReferenceColors[i][j]->r(),
                     mReferenceColors[i][j]->g(),
                     mReferenceColors[i][j]->b());
                ALOGV("Average color at (%d, %d) is (%f, %f, %f)", i, j,
                     mMatchColors[i][j]->r(),
                     mMatchColors[i][j]->g(),
                     mMatchColors[i][j]->b());
                ALOGV("Radius is %f", mMatchRadius[i][j]);
            }
        }

        mSuccess = true;
    }
}
