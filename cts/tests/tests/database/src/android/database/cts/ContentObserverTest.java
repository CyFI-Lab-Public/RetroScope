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


import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.test.InstrumentationTestCase;

public class ContentObserverTest extends InstrumentationTestCase {
    private static final Uri CONTENT_URI = Uri.parse("content://uri");

    public void testContentObserver() throws InterruptedException {
        // Test constructor with null handler, dispatchChange will directly invoke onChange.
        MyContentObserver contentObserver;
        contentObserver = new MyContentObserver(null);
        assertFalse(contentObserver.hasChanged());
        assertFalse(contentObserver.getSelfChangeState());
        contentObserver.dispatchChange(true);
        assertTrue(contentObserver.hasChanged());
        assertTrue(contentObserver.getSelfChangeState());

        contentObserver.resetStatus();
        contentObserver.setSelfChangeState(true);
        assertFalse(contentObserver.hasChanged());
        assertTrue(contentObserver.getSelfChangeState());
        contentObserver.dispatchChange(false);
        assertTrue(contentObserver.hasChanged());
        assertFalse(contentObserver.getSelfChangeState());

        HandlerThread ht = new HandlerThread(getClass().getName());
        ht.start();
        Looper looper = ht.getLooper();
        Handler handler = new Handler(looper);
        final long timeout = 1000L;

        contentObserver = new MyContentObserver(handler);
        assertFalse(contentObserver.hasChanged());
        assertFalse(contentObserver.getSelfChangeState());
        contentObserver.dispatchChange(true);
        assertTrue(contentObserver.hasChanged(timeout));
        assertTrue(contentObserver.getSelfChangeState());

        contentObserver.resetStatus();
        contentObserver.setSelfChangeState(true);
        assertFalse(contentObserver.hasChanged());
        assertTrue(contentObserver.getSelfChangeState());
        contentObserver.dispatchChange(false);
        assertTrue(contentObserver.hasChanged(timeout));
        assertFalse(contentObserver.getSelfChangeState());

        looper.quit();
    }

    public void testContentObserverWithUri() throws InterruptedException {
        // Test constructor with null handler, dispatchChange will directly invoke onChange.
        MyContentObserverWithUri contentObserver;
        contentObserver = new MyContentObserverWithUri(null);
        assertFalse(contentObserver.hasChanged());
        assertFalse(contentObserver.getSelfChangeState());
        contentObserver.dispatchChange(true, CONTENT_URI);
        assertTrue(contentObserver.hasChanged());
        assertTrue(contentObserver.getSelfChangeState());
        assertEquals(CONTENT_URI, contentObserver.getUri());

        contentObserver.resetStatus();
        contentObserver.setSelfChangeState(true);
        assertFalse(contentObserver.hasChanged());
        assertTrue(contentObserver.getSelfChangeState());
        contentObserver.dispatchChange(false, CONTENT_URI);
        assertTrue(contentObserver.hasChanged());
        assertFalse(contentObserver.getSelfChangeState());
        assertEquals(CONTENT_URI, contentObserver.getUri());

        HandlerThread ht = new HandlerThread(getClass().getName());
        ht.start();
        Looper looper = ht.getLooper();
        Handler handler = new Handler(looper);
        final long timeout = 1000L;

        contentObserver = new MyContentObserverWithUri(handler);
        assertFalse(contentObserver.hasChanged());
        assertFalse(contentObserver.getSelfChangeState());
        contentObserver.dispatchChange(true, CONTENT_URI);
        assertTrue(contentObserver.hasChanged(timeout));
        assertTrue(contentObserver.getSelfChangeState());
        assertEquals(CONTENT_URI, contentObserver.getUri());

        contentObserver.resetStatus();
        contentObserver.setSelfChangeState(true);
        assertFalse(contentObserver.hasChanged());
        assertTrue(contentObserver.getSelfChangeState());
        contentObserver.dispatchChange(false, CONTENT_URI);
        assertTrue(contentObserver.hasChanged(timeout));
        assertFalse(contentObserver.getSelfChangeState());
        assertEquals(CONTENT_URI, contentObserver.getUri());

        looper.quit();
    }

    public void testDeliverSelfNotifications() {
        MyContentObserver contentObserver = new MyContentObserver(null);
        assertFalse(contentObserver.deliverSelfNotifications());
    }

    private static class MyContentObserver extends ContentObserver {
        private boolean mHasChanged;
        private boolean mSelfChange;

        public MyContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            synchronized(this) {
                mHasChanged = true;
                mSelfChange = selfChange;
                notifyAll();
            }
        }

        protected synchronized boolean hasChanged(long timeout) throws InterruptedException {
            if (!mHasChanged) {
                wait(timeout);
            }
            return mHasChanged;
        }

        protected boolean hasChanged() {
            return mHasChanged;
        }

        protected void resetStatus() {
            mHasChanged = false;
            mSelfChange = false;
        }

        protected boolean getSelfChangeState() {
            return mSelfChange;
        }

        protected void setSelfChangeState(boolean state) {
            mSelfChange = state;
        }
    }

    private static class MyContentObserverWithUri extends ContentObserver {
        private boolean mHasChanged;
        private boolean mSelfChange;
        private Uri mUri;

        public MyContentObserverWithUri(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            synchronized(this) {
                mHasChanged = true;
                mSelfChange = selfChange;
                mUri = uri;
                notifyAll();
            }
        }

        protected synchronized boolean hasChanged(long timeout) throws InterruptedException {
            if (!mHasChanged) {
                wait(timeout);
            }
            return mHasChanged;
        }

        protected boolean hasChanged() {
            return mHasChanged;
        }

        protected void resetStatus() {
            mHasChanged = false;
            mSelfChange = false;
            mUri = null;
        }

        protected boolean getSelfChangeState() {
            return mSelfChange;
        }

        protected void setSelfChangeState(boolean state) {
            mSelfChange = state;
        }

        protected Uri getUri() {
            return mUri;
        }
    }
}
