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
import android.gesture.GestureStore;
import android.gesture.Prediction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

/**
 * Compatibility unit tests for {@link GestureStore}
 * <p/>
 * Inherits from GestureStorageTester to test common methods.
 */
public class GestureStoreTest extends GestureStorageTester {

    private GestureStore mGestureStore = null;

    private static class GestureStoreFacade implements GestureStorageAccessor {

        private GestureStore mGestureStore;

        public GestureStoreFacade(GestureStore gestureStore) {
            mGestureStore = gestureStore;
        }

        public void addGesture(String entryName, Gesture gesture) {
            mGestureStore.addGesture(entryName, gesture);
        }

        public Set<String> getGestureEntries() {
            return mGestureStore.getGestureEntries();
        }

        public ArrayList<Gesture> getGestures(String entryName) {
            return mGestureStore.getGestures(entryName);
        }

        public int getOrientationStyle() {
            return mGestureStore.getOrientationStyle();
        }

        public int getSequenceType() {
            return mGestureStore.getSequenceType();
        }

        public ArrayList<Prediction> recognize(Gesture gesture) {
            return mGestureStore.recognize(gesture);
        }

        public void removeEntry(String entryName) {
            mGestureStore.removeEntry(entryName);
        }

        public void removeGesture(String entryName, Gesture gesture) {
            mGestureStore.removeGesture(entryName, gesture);
        }

        public void setOrientationStyle(int style) {
            mGestureStore.setOrientationStyle(style);
        }

        public void setSequenceType(int type) {
            mGestureStore.setSequenceType(type);
        }
    }

    @Override
    protected GestureStorageAccessor createFixture() {
        if (mGestureStore == null) {
            mGestureStore = new GestureStore();
        }
        return new GestureStoreFacade(mGestureStore);
    }


    /**
     * Verify that adding a gesture is flagged as change.
     * Tests {@link android.gesture.GestureStore#hasChanged()}.
     */
    public void testHasChanged_add() {
        assertFalse(mGestureStore.hasChanged());
        mGestureStore.addGesture(TEST_GESTURE_NAME, mLineGesture);
        assertTrue(mGestureStore.hasChanged());
    }

    /**
     * Verify that removing a gesture is flagged as a change.
     * Tests {@link android.gesture.GestureStore#hasChanged()}.
     */
    public void testHasChanged_removeGesture() throws IOException {
        mGestureStore.addGesture(TEST_GESTURE_NAME, mLineGesture);
        // save gesture to clear flag
        mGestureStore.save(new ByteArrayOutputStream());
        assertFalse(mGestureStore.hasChanged());
        mGestureStore.removeGesture(TEST_GESTURE_NAME, mLineGesture);
        assertTrue(mGestureStore.hasChanged());
    }

    /**
     * Verify that removing an entry is flagged as a change.
     * Tests {@link android.gesture.GestureStore#hasChanged()}.
     */
    public void testHasChanged_removeEntry() throws IOException {
        mGestureStore.addGesture(TEST_GESTURE_NAME, mLineGesture);
        // save gesture to clear flag
        mGestureStore.save(new ByteArrayOutputStream());
        assertFalse(mGestureStore.hasChanged());
        mGestureStore.removeEntry(TEST_GESTURE_NAME);
        assertTrue(mGestureStore.hasChanged());
    }


    /**
     * Test method for {@link android.gesture.GestureStore#save(java.io.OutputStream)} and
     * {@link android.gesture.GestureStore#load(java.io.InputStream)}.
     * <p/>
     * Verifies that a simple GestureStore can be stored and retrieved from a stream
     */
    public void testSaveLoadOutputStream() throws IOException {
        ByteArrayOutputStream outStream = null;
        ByteArrayInputStream inStream = null;

        try {
            mGestureStore.addGesture(TEST_GESTURE_NAME, mLineGesture);
            outStream = new ByteArrayOutputStream();
            mGestureStore.save(outStream);

            // now load a store from the stream and verify its the same
            inStream = new ByteArrayInputStream(outStream.toByteArray());
            GestureStore loadStore = new GestureStore();
            loadStore.load(inStream);
            assertEquals(mGestureStore.getOrientationStyle(), loadStore.getOrientationStyle());
            assertEquals(mGestureStore.getSequenceType(), loadStore.getSequenceType());
            assertEquals(mGestureStore.getGestureEntries(), loadStore.getGestureEntries());
            Gesture loadedGesture = loadStore.getGestures(TEST_GESTURE_NAME).get(0);
            new GestureComparator().assertGesturesEquals(mLineGesture, loadedGesture);

        } finally {
            if (inStream != null) {
                inStream.close();
            }
            if (outStream != null) {
                outStream.close();
            }
        }
    }
}
