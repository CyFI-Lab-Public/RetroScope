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

#ifndef ANDROID_SF_FLOAT_RECT
#define ANDROID_SF_FLOAT_RECT

#include <utils/TypeHelpers.h>

namespace android {

class FloatRect
{
public:
    float left;
    float top;
    float right;
    float bottom;

    inline FloatRect() { }
    inline FloatRect(const Rect& other)
        : left(other.left), top(other.top), right(other.right), bottom(other.bottom) { }

    inline float getWidth() const { return right - left; }
    inline float getHeight() const { return bottom - top; }
};

}; // namespace android

#endif // ANDROID_SF_FLOAT_RECT
