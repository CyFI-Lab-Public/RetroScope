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
import android.graphics.Path;
import android.os.Parcel;

import junit.framework.TestCase;

/**
 * Compatibility unit tests for {@link android.gesture.Gesture#getStrokes()}
 *
 * Basic tests that verify expected values when a simple line gesture stroke is added.
 *
 * TODO: add tests for more extensive gestures
 */
public class GestureTest extends TestCase {

    private Gesture mGesture;
    private LineGestureStrokeHelper mLineHelper;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mGesture = new Gesture();
        mLineHelper = new LineGestureStrokeHelper();
    }

    /**
     * Test method for {@link android.gesture.Gesture#getStrokes()}.
     */
    public void testGetStrokes() {
        // expect initially empty
        assertEquals(0, mGesture.getStrokes().size());
        GestureStroke line = mLineHelper.createLineGesture();
        mGesture.addStroke(line);
        assertEquals(1, mGesture.getStrokes().size());
        assertEquals(line, mGesture.getStrokes().get(0));
    }

    /**
     * Test method for {@link android.gesture.Gesture#getStrokesCount()}.
     */
    public void testGetStrokesCount() {
        // expect initially empty
        assertEquals(0, mGesture.getStrokesCount());
        GestureStroke line = mLineHelper.createLineGesture();
        mGesture.addStroke(line);
        assertEquals(1, mGesture.getStrokesCount());
    }

    /**
     * Test method for {@link android.gesture.Gesture#getBoundingBox()}.
     */
    public void testGetBoundingBox() {
        mGesture.addStroke(mLineHelper.createLineGesture());
        // assert bounding box is same as bounding box for individual gesture stroke
        mLineHelper.assertLineBoundingBox(mGesture.getBoundingBox());
    }

    /**
     * Test method for {@link android.gesture.Gesture#toPath()}.
     */
    public void testToPath() {
        mGesture.addStroke(mLineHelper.createLineGesture());
        // assert path is same as bounding box for individual gesture
        mLineHelper.assertLineBoundingBox(mGesture.toPath());
    }

    /**
     * Test method for {@link android.gesture.Gesture#toPath(android.graphics.Path)}.
     */
    public void testToPathPath() {
        mGesture.addStroke(mLineHelper.createLineGesture());
        // assert path is valid when null argument is passed in
        mLineHelper.assertLineBoundingBox(mGesture.toPath(null));
        Path myPath = new Path();
        Path generatedPath = mGesture.toPath(myPath);
        // check that provided path argument was used in construction
        assertTrue(myPath == generatedPath);
        // assert that it was valid
        mLineHelper.assertLineBoundingBox(generatedPath);

        // TODO: check scenario where provided Path already has data
    }

    /**
     * Test method for {@link android.gesture.Gesture#writeToParcel(android.os.Parcel, int)} and
     * android.gesture.Gesture.CREATOR.createFromParcel.
     *
     * Verify Gesture can be written into and read from a Parcel
     */
    public void testReadWriteToParcel() {
        Parcel parcel = Parcel.obtain();
        mGesture.addStroke(mLineHelper.createLineGesture());
        mGesture.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Gesture readGesture = Gesture.CREATOR.createFromParcel(parcel);
        // check that all attributes are equal
        new GestureComparator().assertGesturesEquals(mGesture, readGesture);
    }
}
