/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.view.cts;

import android.os.Parcel;
import android.os.Parcelable;
import android.test.InstrumentationTestCase;
import android.view.AbsSavedState;

public class AbsSavedStateTest extends InstrumentationTestCase {

    // constant for test of writeToParcel
    public static final int TEST_NUMBER = 1;

    public void testConstructor() {
        MockParcelable superState = new MockParcelable();
        assertNotNull(superState);
        new MockAbsSavedState(superState);

        Parcel source = Parcel.obtain();
        new MockAbsSavedState(source);

        MockAbsSavedState savedState = new MockAbsSavedState(source);
        assertEquals(0, savedState.describeContents());
    }

    public void testGetSuperState() {
        MockParcelable superState = new MockParcelable();
        assertNotNull(superState);
        MockAbsSavedState savedState = new MockAbsSavedState(superState);

        assertSame(superState, savedState.getSuperState());
    }

    public void testWriteToParcel() {
        MockParcelable superState = new MockParcelable();
        assertNotNull(superState);
        MockAbsSavedState savedState = new MockAbsSavedState(superState);

        Parcel dest = Parcel.obtain();
        int flags = 2;
        savedState.writeToParcel(dest, flags);

        // we instantiate the writeToParcel of Parcalable
        // and give a return for test
        assertEquals(TEST_NUMBER, superState.writeToParcelRunSymbol());
        assertEquals(flags, superState.getFlags());
    }

    static class MockAbsSavedState extends AbsSavedState {

        public MockAbsSavedState(Parcelable superState) {
            super(superState);
        }

        public MockAbsSavedState(Parcel source) {
            super(source);
        }
    }

    static class MockParcelable implements Parcelable {

        // Test for writeToParcel
        private int mTest;
        private int mFlags;

        public int describeContents() {
            return 0;
        }

        // Instantiate writeToParcel
        public void writeToParcel(Parcel dest, int flags) {
            mTest = TEST_NUMBER;
            mFlags = flags;
        }

        // For test of writeToParcel
        public int writeToParcelRunSymbol() {
            return mTest;
        }

        public int getFlags() {
            return mFlags;
        }
    }
}
