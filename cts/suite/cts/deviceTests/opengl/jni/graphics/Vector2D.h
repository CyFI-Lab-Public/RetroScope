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
#ifndef VECTOR2D_H
#define VECTOR2D_H

class Vector2D {
public:
    Vector2D();
    Vector2D(float x, float y);
    Vector2D copy();
    void normalize();
    void add(const Vector2D& v);
    void sub(const Vector2D& v);
    void scale(float s);
    void limit(float max);
    void limit(float maxX, float maxY);
    float magnitude();
    float distance(const Vector2D& v);
    float mX;
    float mY;
};
#endif
