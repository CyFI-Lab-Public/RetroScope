/*
 * Copyright (C) 2013 The Android Open Source Project
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

namespace android {

class RegionTest : public testing::Test {
protected:
    void checkVertTJunction(const Rect* lhs, const Rect* rhs) {
        EXPECT_FALSE((rhs->right > lhs->left && rhs->right < lhs->right) ||
                (rhs->left > lhs->left && rhs->left < lhs->right));
    }

    void verifyNoTJunctions(const Region& r) {
        for (const Rect* current = r.begin(); current < r.end(); current++) {
            for (const Rect* other = current - 1; other >= r.begin(); other--) {
                if (other->bottom < current->top) break;
                if (other->bottom != current->top) continue;
                checkVertTJunction(current, other);
            }
            for (const Rect* other = current + 1; other < r.end(); other++) {
                if (other->top > current->bottom) break;
                if (other->top != current->bottom) continue;
                checkVertTJunction(current, other);
            }
        }
    }

    void checkTJunctionFreeFromRegion(const Region& original, int expectedCount = -1) {
        Region modified = Region::createTJunctionFreeRegion(original);
        verifyNoTJunctions(modified);
        if (expectedCount != -1) {
            EXPECT_EQ(modified.end() - modified.begin(), expectedCount);
        }
        EXPECT_TRUE((original ^ modified).isEmpty());
    }
};

TEST_F(RegionTest, MinimalDivision_TJunction) {
    Region r;
     // | x |
     // |xxx|
    r.clear();
    r.orSelf(Rect(1, 0, 2, 1));
    r.orSelf(Rect(0, 1, 3, 2));
    checkTJunctionFreeFromRegion(r, 4);

     // | x |
     // |   |
     // |xxx|
    r.clear();
    r.orSelf(Rect(1, 0, 2, 1));
    r.orSelf(Rect(0, 2, 3, 3));
    checkTJunctionFreeFromRegion(r, 2);
}

TEST_F(RegionTest, Trivial_TJunction) {
    Region r;
    checkTJunctionFreeFromRegion(r);

    r.orSelf(Rect(100, 100, 500, 500));
    checkTJunctionFreeFromRegion(r);
}

TEST_F(RegionTest, Simple_TJunction) {
    Region r;
     // | x  |
     // |xxxx|
     // |xxxx|
     // |xxxx|
    r.clear();
    r.orSelf(Rect(1, 0, 2, 1));
    r.orSelf(Rect(0, 1, 3, 3));
    checkTJunctionFreeFromRegion(r);

     // | x |
     // |xx |
     // |xxx|
    r.clear();
    r.orSelf(Rect(2,0,4,2));
    r.orSelf(Rect(0,2,4,4));
    r.orSelf(Rect(0,4,6,6));
    checkTJunctionFreeFromRegion(r);

     // |x x|
     // |xxx|
     // |x x|
    r.clear();
    r.orSelf(Rect(0,0,2,6));
    r.orSelf(Rect(4,0,6,6));
    r.orSelf(Rect(0,2,6,4));
    checkTJunctionFreeFromRegion(r);

     // |xxx|
     // | x |
     // | x |
    r.clear();
    r.orSelf(Rect(0,0,6,2));
    r.orSelf(Rect(2,2,4,6));
    checkTJunctionFreeFromRegion(r);
}

TEST_F(RegionTest, Bigger_TJunction) {
    Region r;
     // |xxxx   |
     // | xxxx  |
     // |  xxxx |
     // |   xxxx|
    for (int i = 0; i < 4; i++) {
        r.orSelf(Rect(i,i,i+4,i+1));
    }
    checkTJunctionFreeFromRegion(r, 16);
}

#define ITER_MAX 1000
#define X_MAX 8
#define Y_MAX 8

TEST_F(RegionTest, Random_TJunction) {
    Region r;
    srandom(12345);

    for (int iter = 0; iter < ITER_MAX; iter++) {
        r.clear();
        for (int i = 0; i < X_MAX; i++) {
            for (int j = 0; j < Y_MAX; j++) {
                if (random() % 2) {
                    r.orSelf(Rect(i, j, i + 1, j + 1));
                }
            }
        }
        checkTJunctionFreeFromRegion(r);
    }
}

}; // namespace android

