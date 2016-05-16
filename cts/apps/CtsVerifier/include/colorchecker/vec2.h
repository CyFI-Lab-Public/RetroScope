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

#ifndef VEC2_H
#define VEC2_H

#include <assert.h>
#include <cmath>

// Implements a class to represent the location of a pixel.
template <class T>
class Vec2{
  public:
    Vec2(T inputX, T inputY) {
        mX = inputX;
        mY = inputY;
    }

    Vec2() {}

    inline Vec2<T> operator+ (const Vec2<T> &param) const {
        Vec2<T> temp(mX + param.x(), mY + param.y());
        return temp;
    }

    inline Vec2<T> operator- (const Vec2<T> &param) const {
        Vec2<T> temp(mX - param.x(), mY - param.y());
        return temp;
    }

    inline Vec2<float> operator/ (const int param) const {
        assert(param != 0);
        return Vec2<float>(static_cast<float>(mX) / static_cast<float>(param),
                           static_cast<float>(mY) / static_cast<float>(param));
    }

    template <class U>
    float squareDistance(const Vec2<U> &param) const {
        int difference = 0.f;
        difference = (static_cast<float>(mX) - static_cast<float>(param.x())) *
              (static_cast<float>(mX) - static_cast<float>(param.x())) +
              (static_cast<float>(mY) - static_cast<float>(param.y())) *
              (static_cast<float>(mY) - static_cast<float>(param.y()));
        return difference;
    }

    inline T x() const { return mX; }
    inline T y() const { return mY; }

    inline void set(const T inputX, const T inputY) {
        mX = inputX;
        mY = inputY;
    }

  private:
    T mX;
    T mY;
};

typedef Vec2<int> Vec2i;
typedef Vec2<float> Vec2f;
#endif
