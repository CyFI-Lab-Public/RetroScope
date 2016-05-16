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

package android.database.cts;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.test.AndroidTestCase;

public class DataSetObservableTest extends AndroidTestCase {

    public void testNotifyMethods() {
        DataSetObservable dataSetObservalbe = new DataSetObservable();
        MockDataSetObserver dataSetObserver1 = new MockDataSetObserver();
        MockDataSetObserver dataSetObserver2 = new MockDataSetObserver();

        dataSetObservalbe.registerObserver(dataSetObserver1);
        dataSetObservalbe.registerObserver(dataSetObserver2);

        assertFalse(dataSetObserver1.hasChanged());
        assertFalse(dataSetObserver2.hasChanged());
        dataSetObservalbe.notifyChanged();
        assertTrue(dataSetObserver1.hasChanged());
        assertTrue(dataSetObserver2.hasChanged());

        assertFalse(dataSetObserver1.hasInvalidated());
        assertFalse(dataSetObserver2.hasInvalidated());
        dataSetObservalbe.notifyInvalidated();
        assertTrue(dataSetObserver1.hasInvalidated());
        assertTrue(dataSetObserver2.hasInvalidated());

        // After unregistering, all the observers can not be notified.
        dataSetObservalbe.unregisterAll();
        dataSetObserver1.resetStatus();
        dataSetObserver2.resetStatus();

        // notifyChanged is not working on dataSetObserver1 & 2 anymore.
        dataSetObservalbe.notifyChanged();
        assertFalse(dataSetObserver1.hasChanged());
        assertFalse(dataSetObserver2.hasChanged());

        // notifyInvalidated is not working on dataSetObserver1 & 2 anymore.
        dataSetObservalbe.notifyInvalidated();
        assertFalse(dataSetObserver1.hasInvalidated());
        assertFalse(dataSetObserver2.hasInvalidated());
    }

    private class MockDataSetObserver extends DataSetObserver {
        private boolean mHasChanged = false;
        private boolean mHasInvalidated = false;

        @Override
        public void onChanged() {
            super.onChanged();
            mHasChanged = true;
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            mHasInvalidated = true;
        }

        protected boolean hasChanged() {
            return mHasChanged;
        }

        protected boolean hasInvalidated() {
            return mHasInvalidated;
        }

        protected void resetStatus() {
            mHasChanged = false;
            mHasInvalidated = false;
        }
    }
}
