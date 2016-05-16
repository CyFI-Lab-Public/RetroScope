/*
 * Copyright 2013 The Android Open Source Project
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

#define LOG_TAG "RegionTest"

#include <stdlib.h>
#include <ui/Region.h>
#include <ui/Rect.h>
#include <gtest/gtest.h>

#include <ui/mat4.h>

namespace android {

class MatTest : public testing::Test {
protected:
};

TEST_F(MatTest, Basics) {
    mat4 m0;
    EXPECT_EQ(sizeof(mat4), sizeof(float)*16);
}

TEST_F(MatTest, ComparisonOps) {
    mat4 m0;
    mat4 m1(2);

    EXPECT_TRUE(m0 == m0);
    EXPECT_TRUE(m0 != m1);
    EXPECT_FALSE(m0 != m0);
    EXPECT_FALSE(m0 == m1);
}

TEST_F(MatTest, Constructors) {
    mat4 m0;
    ASSERT_EQ(m0[0].x, 1);
    ASSERT_EQ(m0[0].y, 0);
    ASSERT_EQ(m0[0].z, 0);
    ASSERT_EQ(m0[0].w, 0);
    ASSERT_EQ(m0[1].x, 0);
    ASSERT_EQ(m0[1].y, 1);
    ASSERT_EQ(m0[1].z, 0);
    ASSERT_EQ(m0[1].w, 0);
    ASSERT_EQ(m0[2].x, 0);
    ASSERT_EQ(m0[2].y, 0);
    ASSERT_EQ(m0[2].z, 1);
    ASSERT_EQ(m0[2].w, 0);
    ASSERT_EQ(m0[3].x, 0);
    ASSERT_EQ(m0[3].y, 0);
    ASSERT_EQ(m0[3].z, 0);
    ASSERT_EQ(m0[3].w, 1);

    mat4 m1(2);
    mat4 m2(vec4(2));
    mat4 m3(m2);

    EXPECT_EQ(m1, m2);
    EXPECT_EQ(m2, m3);
    EXPECT_EQ(m3, m1);

    mat4 m4(vec4(1), vec4(2), vec4(3), vec4(4));
}

TEST_F(MatTest, ArithmeticOps) {
    mat4 m0;
    mat4 m1(2);
    mat4 m2(vec4(2));

    m1 += m2;
    EXPECT_EQ(mat4(4), m1);

    m2 -= m1;
    EXPECT_EQ(mat4(-2), m2);

    m1 *= 2;
    EXPECT_EQ(mat4(8), m1);

    m1 /= 2;
    EXPECT_EQ(mat4(4), m1);

    m0 = -m0;
    EXPECT_EQ(mat4(-1), m0);
}

TEST_F(MatTest, UnaryOps) {
    const mat4 identity;
    mat4 m0;

    ++m0;
    EXPECT_EQ(mat4( vec4(2,1,1,1), vec4(1,2,1,1), vec4(1,1,2,1), vec4(1,1,1,2) ), m0);
    EXPECT_EQ(mat4( -vec4(2,1,1,1), -vec4(1,2,1,1), -vec4(1,1,2,1), -vec4(1,1,1,2) ), -m0);

    --m0;
    EXPECT_EQ(identity, m0);
}

TEST_F(MatTest, MiscOps) {
    const mat4 identity;
    mat4 m0;
    EXPECT_EQ(4, trace(m0));

    mat4 m1(vec4(1,2,3,4), vec4(5,6,7,8), vec4(9,10,11,12), vec4(13,14,15,16));
    mat4 m2(vec4(1,5,9,13), vec4(2,6,10,14), vec4(3,7,11,15), vec4(4,8,12,16));
    EXPECT_EQ(m1, transpose(m2));
    EXPECT_EQ(m2, transpose(m1));
    EXPECT_EQ(vec4(1,6,11,16), diag(m1));

    EXPECT_EQ(identity, inverse(identity));

    mat4 m3(vec4(4,3,0,0), vec4(3,2,0,0), vec4(0,0,1,0), vec4(0,0,0,1));
    mat4 m3i(inverse(m3));
    EXPECT_FLOAT_EQ(-2, m3i[0][0]);
    EXPECT_FLOAT_EQ( 3, m3i[0][1]);
    EXPECT_FLOAT_EQ( 3, m3i[1][0]);
    EXPECT_FLOAT_EQ(-4, m3i[1][1]);

    mat4 m3ii(inverse(m3i));
    EXPECT_FLOAT_EQ(m3[0][0], m3ii[0][0]);
    EXPECT_FLOAT_EQ(m3[0][1], m3ii[0][1]);
    EXPECT_FLOAT_EQ(m3[1][0], m3ii[1][0]);
    EXPECT_FLOAT_EQ(m3[1][1], m3ii[1][1]);

    EXPECT_EQ(m1, m1*identity);
}

}; // namespace android
