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
import android.gesture.Prediction;

import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * Tests for common GestureStore/GestureLibrary APIs ie {@link GestureStorageFacade.}
 */
public abstract class GestureStorageTester extends TestCase {

    private GestureStorageAccessor mFixture;
    /** Simple straight line gesture used for basic testing */
    protected Gesture mLineGesture;
    protected Gesture mAnotherGesture;
    protected static final String TEST_GESTURE_NAME ="cts-test-gesture";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mFixture = createFixture();
        GestureStroke stroke = new LineGestureStrokeHelper().createLineGesture();
        mLineGesture = new Gesture();
        mLineGesture.addStroke(stroke);
        mAnotherGesture = new Gesture();
        mAnotherGesture.addStroke(stroke);
    }

    protected abstract GestureStorageAccessor createFixture();

    /**
     * Test method for {@link android.gesture.GestureStore#getGestureEntries()}.
     * <p/>
     * Simple check to verify an added gesture appears in set of gesture entries.
     */
    public void testGetGestureEntries() {
        // assert initially empty
        assertEquals(0, mFixture.getGestureEntries().size());
        mFixture.addGesture(TEST_GESTURE_NAME, mLineGesture);
        assertEquals(1, mFixture.getGestureEntries().size());
        assertTrue(mFixture.getGestureEntries().contains(TEST_GESTURE_NAME));
    }

    /**
     * Test method for {@link android.gesture.GestureStore#recognize(Gesture)}.
     *
     * Simple check to verify an added line gesture can be recognized
     */
    public void testRecognize() {
        mFixture.addGesture(TEST_GESTURE_NAME, mLineGesture);
        Gesture newLineGesture = new Gesture();
        GestureStroke stroke = new LineGestureStrokeHelper().createLineGesture();
        newLineGesture.addStroke(stroke);
        ArrayList<Prediction> predictions = mFixture.recognize(newLineGesture);
        assertEquals(1, predictions.size());
        assertEquals(TEST_GESTURE_NAME, predictions.get(0).name);
    }

    /**
     * Test method for {@link android.gesture.GestureStore#removeGesture(java.lang.String, android.gesture.Gesture)}.
     */
    public void testRemoveGesture() {
        mFixture.addGesture(TEST_GESTURE_NAME, mLineGesture);
        mFixture.addGesture(TEST_GESTURE_NAME, mAnotherGesture);
        mFixture.removeGesture(TEST_GESTURE_NAME, mAnotherGesture);
        // check that gesture just removed is gone
        assertFalse(mFixture.getGestures(TEST_GESTURE_NAME).contains(mAnotherGesture));

        mFixture.removeGesture(TEST_GESTURE_NAME, mLineGesture);
        // test that entry itself is removed
        assertFalse(mFixture.getGestureEntries().contains(TEST_GESTURE_NAME));
    }

    /**
     * Test method for {@link android.gesture.GestureStore#removeEntry(java.lang.String)}.
     */
    public void testRemoveEntry() {
        mFixture.addGesture(TEST_GESTURE_NAME, mLineGesture);
        mFixture.addGesture(TEST_GESTURE_NAME, mAnotherGesture);
        mFixture.removeEntry(TEST_GESTURE_NAME);
        assertFalse(mFixture.getGestureEntries().contains(TEST_GESTURE_NAME));
        assertNull(mFixture.getGestures(TEST_GESTURE_NAME));
    }

    /**
     * Test method for {@link android.gesture.GestureStore#getGestures(java.lang.String)}.
     */
    public void testGetGestures() {
        // test getting gestures for non existent entry
        assertNull(mFixture.getGestures(TEST_GESTURE_NAME));
        mFixture.addGesture(TEST_GESTURE_NAME, mLineGesture);
        assertTrue(mFixture.getGestures(TEST_GESTURE_NAME).contains(mLineGesture));
    }
}
