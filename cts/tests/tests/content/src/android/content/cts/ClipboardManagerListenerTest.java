/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.content.cts;

import android.content.ClipData;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.cts.util.PollingCheck;
import android.net.Uri;
import android.test.ActivityInstrumentationTestCase2;


public class ClipboardManagerListenerTest
        extends ActivityInstrumentationTestCase2<ClipboardManagerListenerActivity> {

    private ClipboardManagerListenerActivity mActivity;

    private CountingClipChangedListener mListener;

    public ClipboardManagerListenerTest() {
        super(ClipboardManagerListenerActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mListener = new CountingClipChangedListener();
    }

    public void testListener() throws Exception {
        mActivity.addPrimaryClipChangedListener(mListener);
        assertClipChangedCount(0);

        mActivity.setPrimaryClip(ClipData.newPlainText("Label", "Text"));
        assertClipChangedCount(1);

        mActivity.setPrimaryClip(ClipData.newPlainText("Label", "Text"));
        assertClipChangedCount(2);

        mActivity.removePrimaryClipChangedListener(mListener);
        mActivity.setPrimaryClip(ClipData.newRawUri("Label", Uri.parse("http://www.google.com")));
        assertNoClipChange(2);
    }

    private void assertClipChangedCount(final int expectedCount) {
        new PollingCheck() {
            @Override
            protected boolean check() {
                return expectedCount == mListener.getCount();
            }
        }.run();
    }

    private void assertNoClipChange(int expectedCount) throws InterruptedException {
        Thread.sleep(1000);
        assertEquals(expectedCount, mListener.getCount());
    }

    class CountingClipChangedListener implements OnPrimaryClipChangedListener {

        private int mCount = 0;

        @Override
        public void onPrimaryClipChanged() {
            synchronized (this) {
                mCount++;
            }
        }

        public synchronized int getCount() {
            return mCount;
        }
    }
}
