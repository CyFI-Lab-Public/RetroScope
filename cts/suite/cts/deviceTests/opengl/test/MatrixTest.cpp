/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

#include <gtest/gtest.h>
#include <math.h>

#include "Matrix.h"

class MatrixTest: public testing::Test {
public:

};

void checkValues(const float* arr1, const float* arr2, const int size) {
    for (int i = 0; i < size; i++) {
        ASSERT_FLOAT_EQ(arr1[i], arr2[i]);
    }
}

TEST(MatrixTest, matrixEqualityTest) {
    // Create two identity matrixes.
    Matrix m1;
    Matrix m2;
    // Change some random values.
    m1.mData[4] = 9;
    m2.mData[4] = 9;
    // Check they are the same.
    ASSERT_TRUE(m1.equals(m2));
    Matrix* clone = new Matrix(m1);
    ASSERT_TRUE(clone != NULL);
    ASSERT_TRUE(m1.equals(*clone));
    delete clone;
}

TEST(MatrixTest, matrixIdentityTest) {
    // Create an identity matrix.
    Matrix m;
    float expected[] = {
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f};
    // Check values
    checkValues(m.mData, expected, Matrix::MATRIX_SIZE);
}

TEST(MatrixTest, matrixLoadWithTest) {
    // Create a matrix.
    Matrix m1;
    float* d1 = m1.mData;
    float data[Matrix::MATRIX_SIZE];

    // Fill with rubbish
    for (int i = 0; i < Matrix::MATRIX_SIZE; i++) {
        d1[i] = i;
        data[i] = i;
    }

    // Create another matrix
    Matrix m2;

    // Load second matrix with first
    m2.loadWith(m1);

    // Check values
    checkValues(m2.mData, data, Matrix::MATRIX_SIZE);
}

TEST(MatrixTest, matrixTranslateTest) {
    Matrix m1;
    m1.translate(10, 5, 6);
    Matrix* m2 = Matrix::newTranslate(10, 5, 6);
    ASSERT_TRUE(m2 != NULL);
    ASSERT_TRUE(m1.equals(*m2));
    delete m2;
}

TEST(MatrixTest, matrixScaleTest) {
    Matrix m1;
    m1.scale(10, 5, 6);
    Matrix* m2 = Matrix::newScale(10, 5, 6);
    ASSERT_TRUE(m2 != NULL);
    ASSERT_TRUE(m1.equals(*m2));
    delete m2;
}

TEST(MatrixTest, matrixRotateTest) {
    Matrix m1;
    m1.rotate(180, 1, 0, 1);
    Matrix* m2 = Matrix::newRotate(180, 1, 0, 1);
    ASSERT_TRUE(m2 != NULL);
    ASSERT_TRUE(m1.equals(*m2));
    delete m2;
}

TEST(MatrixTest, matrixMultiplyTest) {
    // Create three identity matrixes.
    Matrix m1;
    Matrix m2;
    Matrix m3;
    float* d1 = m1.mData;
    float* d2 = m2.mData;

    m3.multiply(m1, m2);
    // Multiplication of identity matrixes should give identity
    ASSERT_TRUE(m3.equals(m1));

    // Fill with ascending numbers
    for (int i = 0; i < Matrix::MATRIX_SIZE; i++) {
        d1[i] = i;
        d2[i] = i;
    }
    m3.multiply(m1, m2);

    // Check against expected
    float expected[] = {
        56, 62, 68, 74,
        152, 174, 196, 218,
        248, 286, 324, 362,
        344, 398, 452, 506};
    checkValues(m3.mData, expected, Matrix::MATRIX_SIZE);
}

TEST(MatrixTest, matrixNewLookAtTest) {
    // Position the eye in front of the origin.
    float eyeX = 0.0f;
    float eyeY = 0.0f;
    float eyeZ = 6.0f;

    // We are looking at the origin
    float centerX = 0.0f;
    float centerY = 0.0f;
    float centerZ = 0.0f;

    // Set our up vector. This is where our head would be pointing were we holding the camera.
    float upX = 0.0f;
    float upY = 1.0f;
    float upZ = 0.0f;

    // Set the view matrix. This matrix can be said to represent the camera position.
    Matrix* m = Matrix::newLookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ,
            upX, upY, upZ);
    ASSERT_TRUE(m != NULL);
    float expected[] = {
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, -6.0f, 1.0f};
    // Check values
    checkValues(m->mData, expected, Matrix::MATRIX_SIZE);
    delete m;
}

TEST(MatrixTest, matrixNewFrustumTest) {
    float ratio = (float) 800 / 600;
    float left = -ratio;
    float right = ratio;
    float bottom = -1.0f;
    float top = 1.0f;
    float near = 1.0f;
    float far = 8.0f;

    Matrix* m = Matrix::newFrustum(left, right, bottom, top, near, far);
    ASSERT_TRUE(m != NULL);
    float expected[] = {
        0.75f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 9.0f / -7.0f, -1.0f,
        0.0f, 0.0f, 16.0f / -7.0f, 0.0f};
    // Check values
    checkValues(m->mData, expected, Matrix::MATRIX_SIZE);
    delete m;
}

TEST(MatrixTest, matrixNewTranslateTest) {
    Matrix* m = Matrix::newTranslate(5, 6, 8);
    ASSERT_TRUE(m != NULL);
    float expected[] = {
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        5.0f, 6.0f, 8.0f, 1.0f};
    // Check values
    checkValues(m->mData, expected, Matrix::MATRIX_SIZE);
    delete m;
}

TEST(MatrixTest, matrixNewScaleTest) {
    Matrix* m = Matrix::newScale(3, 7, 2);
    ASSERT_TRUE(m != NULL);
    float expected[] = {
        3.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 7.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 2.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f};
    // Check values
    checkValues(m->mData, expected, Matrix::MATRIX_SIZE);
    delete m;
}

TEST(MatrixTest, matrixNewRotateTest) {
    Matrix* m = Matrix::newRotate(45.0f, 0.0f, 1.0f, 0.0f);
    ASSERT_TRUE(m != NULL);
    float radians = 45.0f * (M_PI / 180.0f);
    float sin = sinf(radians);
    float cos = cosf(radians);
    float expected[] = {
        cos, 0.0f, -sin, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        sin, 0.0f, cos, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f};
    // Check values
    checkValues(m->mData, expected, Matrix::MATRIX_SIZE);
    delete m;
}

TEST(MatrixTest, matrixMultiplyVectorTest) {
    float in[] = {2, 4, 6, 8};
    float out[4];
    Matrix m;
    float* d = m.mData;
    // Fill with rubbish
    for (int i = 0; i < Matrix::MATRIX_SIZE; i++) {
        d[i] = i;
    }
    float expected[] = {40, 120, 200, 280};
    Matrix::multiplyVector(out, m, in);
    checkValues(out, expected, 4);
}
