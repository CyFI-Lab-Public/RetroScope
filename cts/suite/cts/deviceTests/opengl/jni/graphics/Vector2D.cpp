/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
#include "Vector2D.h"

#include <math.h>

Vector2D::Vector2D() :
        mX(0), mY(0) {
}

Vector2D::Vector2D(float x, float y) :
        mX(x), mY(y) {
}

Vector2D Vector2D::copy() {
    Vector2D v(mX, mY);
    return v;
}

void Vector2D::add(const Vector2D& v) {
    mX += v.mX;
    mY += v.mY;
}

void Vector2D::sub(const Vector2D& v) {
    mX -= v.mX;
    mY -= v.mY;
}

void Vector2D::scale(float s) {
    mX *= s;
    mY *= s;
}

float Vector2D::distance(const Vector2D& v) {
    float dx = mX - v.mX;
    float dy = mY - v.mY;
    return (float) sqrt(dx * dx + dy * dy);
}

void Vector2D::normalize() {
    float m = magnitude();
    if (m > 0) {
        scale(1 / m);
    }
}

void Vector2D::limit(float max) {
    if (magnitude() > max) {
        normalize();
        scale(max);
    }
}

float Vector2D::magnitude() {
    return (float) sqrt(mX * mX + mY * mY);
}
