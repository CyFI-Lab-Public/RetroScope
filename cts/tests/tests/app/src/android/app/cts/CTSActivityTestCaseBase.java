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

package android.app.cts;

import android.test.InstrumentationTestCase;

public class CTSActivityTestCaseBase extends InstrumentationTestCase implements CTSResult {

    private Sync mSync;
    static class Sync {
        public boolean mHasNotify;
        public int mResult;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mSync = new Sync();
    }

    public void setResult(int resultCode) {
        synchronized (mSync) {
            mSync.mHasNotify = true;
            mSync.mResult = resultCode;
            mSync.notify();
        }
    }

    protected void waitForResult() throws InterruptedException {
        synchronized (mSync) {
            while (!mSync.mHasNotify) {
                mSync.wait();
            }
            assertEquals(CTSResult.RESULT_OK, mSync.mResult);
        }
    }
}
