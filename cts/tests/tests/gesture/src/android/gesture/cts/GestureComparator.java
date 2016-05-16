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

import android.gesture.Gesture;
import android.gesture.GestureStroke;

import java.util.Arrays;

import junit.framework.Assert;

/**
 * Helper class to assert that two Gestures are approximately equal
 */
class GestureComparator {

    /**
     * Compare the contents of two (@link Gesture}'s
     *
     * @throws {@link junit.framework.AssertionFailedError} if Gesture's are not equal
     */
    void assertGesturesEquals(Gesture expectedGesture, Gesture observedGesture) {
        Assert.assertEquals(expectedGesture.getID(), observedGesture.getID());
        Assert.assertEquals(expectedGesture.getStrokesCount(), observedGesture.getStrokesCount());

        // now compare gesture strokes. Order is important
        for (int i=0; i < expectedGesture.getStrokesCount(); i++) {
            GestureStroke expectedStroke = expectedGesture.getStrokes().get(i);
            GestureStroke observedStroke = observedGesture.getStrokes().get(i);
            assertGestureStrokesEqual(expectedStroke, observedStroke);
        }
    }

    /**
     * Compare the contents of two {@link GestureStroke}
     *
     * @throws {@link junit.framework.AssertionFailedError} if GestureStroke's are not
     * equal
     */
    void assertGestureStrokesEqual(GestureStroke expectedStroke, GestureStroke observedStroke) {
        Assert.assertEquals(expectedStroke.length, observedStroke.length);
        Assert.assertTrue(Arrays.equals(expectedStroke.points, observedStroke.points));
    }
}
