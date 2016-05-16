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

#ifndef VEC3_H
#define VEC3_H

#include <assert.h>
#include <cmath>

// Implementes a class to represent the pixel value in a 3-dimensional color
// space. The colors are all represented by int as it is mostly used as RGB.
template <class T>
class Vec3{
  public:
    Vec3(T inputRed, T inputGreen, T inputBlue) {
        mRed = inputRed;
        mGreen = inputGreen;
        mBlue = inputBlue;
    }

    Vec3() {}

    template <class U>
    inline Vec3<T> operator+ (const Vec3<U> &param) const {
        Vec3<T> temp(mRed + param.r(), mGreen + param.g(), mBlue + param.b());
        return temp;
    }

    inline Vec3<T> operator- (const Vec3<T> &param) const {
        Vec3<T> temp(mRed - param.r(), mGreen - param.g(), mBlue - param.b());
        return temp;
    }

    inline Vec3<T> operator* (const int param) const {
        Vec3<T> temp(mRed * param, mGreen * param, mBlue * param);
        return temp;
    }

    template <class U>
    inline Vec3<float> operator* (const Vec3<U> &param) const {
        Vec3<float> temp(mRed * static_cast<U>(param.r()),
                         mGreen * static_cast<U>(param.g()),
                         mBlue * static_cast<U>(param.b()));
        return temp;
    }


    inline Vec3<float> operator/ (const int param) const {
        Vec3<float> temp;
        assert(param != 0);
        temp.set(static_cast<float>(mRed) / static_cast<float>(param),
                 static_cast<float>(mGreen) / static_cast<float>(param),
                 static_cast<float>(mBlue) / static_cast<float>(param));
        return temp;
    }

    template <class U>
    inline Vec3<float> operator/ (const Vec3<U> &param) const {
        Vec3<float> temp;
        assert((param.r() != 0.f) && (param.g() != 0.f) && (param.b() != 0.f));
        temp.set(static_cast<float>(mRed) / static_cast<float>(param.r()),
                 static_cast<float>(mGreen) / static_cast<float>(param.g()),
                 static_cast<float>(mBlue) / static_cast<float>(param.b()));
        return temp;
    }

    template <class U>
    float squareDistance(const Vec3<U> &param) const {
        float difference = 0.f;
        difference = static_cast<float>(mRed - param.r()) *
                static_cast<float>(mRed - param.r()) +
                static_cast<float>(mGreen - param.g()) *
                static_cast<float>(mGreen - param.g()) +
                static_cast<float>(mBlue - param.b()) *
                static_cast<float>(mBlue - param.b());
        return difference;
    }

    // Assumes conversion from sRGB to luminance.
    float convertToLuminance() const {
        return (0.299 * static_cast<float>(mRed) +
                0.587 * static_cast<float>(mGreen) +
                0.114 * static_cast<float>(mBlue));
    }

    inline T r() const { return mRed; }
    inline T g() const { return mGreen; }
    inline T b() const { return mBlue; }

    inline void set(const T inputRed, const T inputGreen, const T inputBlue){
        mRed = inputRed;
        mGreen = inputGreen;
        mBlue = inputBlue;
    }

  private:
    T mRed;
    T mGreen;
    T mBlue;
};

typedef Vec3<int> Vec3i;
typedef Vec3<float> Vec3f;
#endif
