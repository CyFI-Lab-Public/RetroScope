/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.gallery3d.filtershow.imageshow;

public class ControlPoint implements Comparable {
    public float x;
    public float y;

    public ControlPoint(float px, float py) {
        x = px;
        y = py;
    }

    public ControlPoint(ControlPoint point) {
        x = point.x;
        y = point.y;
    }

    public boolean sameValues(ControlPoint other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }

        if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x)) {
            return false;
        }
        if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y)) {
            return false;
        }
        return true;
    }

    public ControlPoint copy() {
        return new ControlPoint(x, y);
    }

    @Override
    public int compareTo(Object another) {
        ControlPoint p = (ControlPoint) another;
        if (p.x < x) {
            return 1;
        } else if (p.x > x) {
            return -1;
        }
        return 0;
    }
}
