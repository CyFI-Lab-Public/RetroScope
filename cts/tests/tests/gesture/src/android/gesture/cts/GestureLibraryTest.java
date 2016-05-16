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
import android.gesture.GestureLibrary;
import android.gesture.Prediction;

import java.util.ArrayList;
import java.util.Set;

/**
 * Tests for {@link GestureLibrary}.
 * <p/>
 * Most {@link GestureLibrary} methods are tested by {@link GestureStorageTester}
 */
public class GestureLibraryTest extends GestureStorageTester {

    private GestureLibrary mLibrary = null;

    private static class GestureLibraryFacade implements GestureStorageAccessor {

        private GestureLibrary mGestureLibrary;

        public GestureLibraryFacade(GestureLibrary gestureLibrary) {
            mGestureLibrary = gestureLibrary;
        }

        public void addGesture(String entryName, Gesture gesture) {
            mGestureLibrary.addGesture(entryName, gesture);
        }

        public Set<String> getGestureEntries() {
            return mGestureLibrary.getGestureEntries();
        }

        public ArrayList<Gesture> getGestures(String entryName) {
            return mGestureLibrary.getGestures(entryName);
        }

        public int getOrientationStyle() {
            return mGestureLibrary.getOrientationStyle();
        }

        public int getSequenceType() {
            return mGestureLibrary.getSequenceType();
        }

        public ArrayList<Prediction> recognize(Gesture gesture) {
            return mGestureLibrary.recognize(gesture);
        }

        public void removeEntry(String entryName) {
            mGestureLibrary.removeEntry(entryName);
        }

        public void removeGesture(String entryName, Gesture gesture) {
            mGestureLibrary.removeGesture(entryName, gesture);
        }

        public void setOrientationStyle(int style) {
            mGestureLibrary.setOrientationStyle(style);
        }

        public void setSequenceType(int type) {
            mGestureLibrary.setSequenceType(type);
        }
    }

    private static class GestureLibraryStub extends GestureLibrary {

        @Override
        public boolean load() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean save() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    protected GestureStorageAccessor createFixture() {
        if (mLibrary == null) {
            mLibrary = new GestureLibraryStub();
        }
        return new GestureLibraryFacade(mLibrary);
    }

    /**
     * Tests {@link GestureLibrary#isReadOnly()}.
     * <p/>
     * Verifies default is false.
     */
    public void testIsReadOnly() {
        assertFalse(mLibrary.isReadOnly());
    }
}
