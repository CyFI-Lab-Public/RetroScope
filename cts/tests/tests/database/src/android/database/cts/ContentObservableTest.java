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


import android.database.ContentObservable;
import android.database.ContentObserver;
import android.net.Uri;
import android.test.InstrumentationTestCase;

public class ContentObservableTest extends InstrumentationTestCase {
    private static final Uri CONTENT_URI = Uri.parse("content://uri");

    ContentObservable mContentObservable;
    MyContentObserver mObserver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContentObservable = new ContentObservable();
        mObserver = new MyContentObserver();
    }

    public void testNotifyChange() {
        mContentObservable.registerObserver(mObserver);
        mObserver.resetStatus();
        assertFalse(mObserver.hasChanged());
        mContentObservable.notifyChange(false);
        assertTrue(mObserver.hasChanged());

        try {
            mContentObservable.registerObserver(mObserver);
            fail("Re-registering observer did not cause exception.");
        } catch (IllegalStateException expected) {
            // expected
        }

        try {
            mContentObservable.registerObserver(null);
            fail("Registering null observer did not cause exception.");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        MyContentObserver second = new MyContentObserver();
        mContentObservable.registerObserver(second);

        mContentObservable.unregisterObserver(mObserver);
        mObserver.resetStatus();
        assertFalse(mObserver.hasChanged());
        mContentObservable.notifyChange(false);
        assertFalse(mObserver.hasChanged());
        assertTrue(second.hasChanged());
    }

    public void testDispatchChange() {
        mContentObservable.registerObserver(mObserver);
        mObserver.resetStatus();
        assertFalse(mObserver.hasChanged());
        mContentObservable.dispatchChange(false);
        assertTrue(mObserver.hasChanged());
        assertNull(mObserver.getUri());

        mObserver.resetStatus();
        assertFalse(mObserver.hasChanged());
        mContentObservable.dispatchChange(true);
        assertFalse(mObserver.hasChanged());
        mObserver.setDeliverSelfNotifications(true);
        mContentObservable.dispatchChange(true);
        assertTrue(mObserver.hasChanged());
        assertNull(mObserver.getUri());

        mContentObservable.unregisterObserver(mObserver);
        mObserver.resetStatus();
        assertFalse(mObserver.hasChanged());
        mContentObservable.dispatchChange(false);
        assertFalse(mObserver.hasChanged());
    }

    public void testDispatchChangeWithUri() {
        mContentObservable.registerObserver(mObserver);
        mObserver.resetStatus();
        assertFalse(mObserver.hasChanged());
        mContentObservable.dispatchChange(false, CONTENT_URI);
        assertTrue(mObserver.hasChanged());
        assertEquals(CONTENT_URI, mObserver.getUri());

        mObserver.resetStatus();
        assertFalse(mObserver.hasChanged());
        mContentObservable.dispatchChange(true, CONTENT_URI);
        assertFalse(mObserver.hasChanged());
        mObserver.setDeliverSelfNotifications(true);
        mContentObservable.dispatchChange(true, CONTENT_URI);
        assertTrue(mObserver.hasChanged());
        assertEquals(CONTENT_URI, mObserver.getUri());

        mContentObservable.unregisterObserver(mObserver);
        mObserver.resetStatus();
        assertFalse(mObserver.hasChanged());
        mContentObservable.dispatchChange(false, CONTENT_URI);
        assertFalse(mObserver.hasChanged());
    }

    private static class MyContentObserver extends ContentObserver {
        private boolean mHasChanged = false;
        private boolean mDeliverSelfNotifications = false;
        private Uri mUri;

        public MyContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            mHasChanged = true;
            mUri = uri;
        }

        public boolean deliverSelfNotifications() {
            return mDeliverSelfNotifications ;
        }

        protected boolean hasChanged() {
            return mHasChanged;
        }

        protected void resetStatus() {
            mHasChanged = false;
            mUri = null;
        }

        protected void setDeliverSelfNotifications(boolean b) {
            mDeliverSelfNotifications = b;
        }

        protected Uri getUri() {
            return mUri;
        }
    }
}
