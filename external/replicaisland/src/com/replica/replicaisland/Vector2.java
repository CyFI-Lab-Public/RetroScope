/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.replica.replicaisland;

/**
 * Simple 2D vector class.  Handles basic vector math for 2D vectors.
 */
public final class Vector2 extends AllocationGuard {
    public float x;
    public float y;

    public static final Vector2 ZERO = new Vector2(0, 0);
    
    public Vector2() {
        super();
    }

    public Vector2(float xValue, float yValue) {
        set(xValue, yValue);
    }

    public Vector2(Vector2 other) {
        set(other);
    }

    public final void add(Vector2 other) {
        x += other.x;
        y += other.y;
    }
    
    public final void add(float otherX, float otherY) {
        x += otherX;
        y += otherY;
    }

    public final void subtract(Vector2 other) {
        x -= other.x;
        y -= other.y;
    }

    public final void multiply(float magnitude) {
        x *= magnitude;
        y *= magnitude;
    }
    
    public final void multiply(Vector2 other) {
        x *= other.x;
        y *= other.y;
    }

    public final void divide(float magnitude) {
        if (magnitude != 0.0f) {
            x /= magnitude;
            y /= magnitude;
        }
    }

    public final void set(Vector2 other) {
        x = other.x;
        y = other.y;
    }

    public final void set(float xValue, float yValue) {
        x = xValue;
        y = yValue;
    }

    public final float dot(Vector2 other) {
        return (x * other.x) + (y * other.y);
    }

    public final float length() {
        return (float) Math.sqrt(length2());
    }

    public final float length2() {
        return (x * x) + (y * y);
    }
    
    public final float distance2(Vector2 other) {
        float dx = x - other.x;
        float dy = y - other.y;
        return (dx * dx) + (dy * dy);
    }

    public final float normalize() {
        final float magnitude = length();

        // TODO: I'm choosing safety over speed here.
        if (magnitude != 0.0f) {
            x /= magnitude;
            y /= magnitude;
        }

        return magnitude;
    }

    public final void zero() {
        set(0.0f, 0.0f);
    }
    
    public final void flipHorizontal(float aboutWidth) {
        x = (aboutWidth - x);
    }
    
    public final void flipVertical(float aboutHeight) {
        y = (aboutHeight - y);
    }
}
