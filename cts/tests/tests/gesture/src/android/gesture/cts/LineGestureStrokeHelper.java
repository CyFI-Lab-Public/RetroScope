/*
 * Copyright (C) 2009 The Android Open Source Project
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
package android.gesture.cts;

import android.gesture.GesturePoint;
import android.gesture.GestureStroke;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.ArrayList;

import junit.framework.Assert;

/**
 * Helper class that generates a simple line GestureStroke, and provides common methods
 * to validate values calculated from it.
 */
class LineGestureStrokeHelper {

    // point constants for 45 degree line gesture
    // points have identical x and y coordinates
    final static float LINE_START_POINT = 0;
    final static int START_TIMESTAMP = 0;
    final static float LINE_END_POINT = 20;
    final static float LINE_MIDWAY_POINT = LINE_END_POINT/2;
    final static float LINE_QUARTER_POINT = LINE_MIDWAY_POINT/2;
    final static int END_TIMESTAMP = 1;
    final static float LINE_ANGLE = 45;

    /**
     * Creates a standard straight line gesture starting from the {@link LINE_START_POINT} and
     * {@link LINE_END_POINT}
     */
    GestureStroke createLineGesture() {
        return createGestureStroke(
                new GesturePoint(LINE_START_POINT, LINE_START_POINT, START_TIMESTAMP),
                new GesturePoint(LINE_END_POINT, LINE_END_POINT, END_TIMESTAMP));
    }

    /**
     * Helper method to assert expected values for a path derived from createLineGesture
     *
     * @throws {@link junit.framework.AssertionFailedError} if linePath attributes differs from
     * expected values
     */
    void assertLineBoundingBox(Path linePath) {
        Assert.assertFalse(linePath.isEmpty());
        RectF bounds = new RectF();
        linePath.computeBounds(bounds, true);
        // TODO: is it a compatability requirement that these numbers be consistent?
        // expect a square bounding box, starting at LINE_START_POINT and bounded by
        // LINE_MIDWAY_POINT
        Assert.assertEquals(LINE_MIDWAY_POINT, bounds.bottom);
        Assert.assertEquals(LINE_START_POINT, bounds.left);
        Assert.assertEquals(LINE_MIDWAY_POINT, bounds.right);
        Assert.assertEquals(LINE_START_POINT, bounds.top);
    }

    /**
     * Helper method to assert expected values for a bounds derived from createLineGesture
     *
     * @throws AssertionFailedError if linePath attributes differs from expected values
     */
    void assertLineBoundingBox(RectF bounds) {
        // expect a square bounding box, starting at LINE_START_POINT and bounded by
        // LINE_END_POINT
        Assert.assertEquals(LINE_END_POINT, bounds.bottom);
        Assert.assertEquals(LINE_START_POINT, bounds.left);
        Assert.assertEquals(LINE_END_POINT, bounds.right);
        Assert.assertEquals(LINE_START_POINT, bounds.top);
    }

    /**
     * Helper method for creating a gesture stroke
     */
    GestureStroke createGestureStroke(GesturePoint... points) {
        ArrayList<GesturePoint> list = new ArrayList<GesturePoint>(points.length);
        for (GesturePoint point : points) {
            list.add(point);
        }
        return new GestureStroke(list);
    }
}
