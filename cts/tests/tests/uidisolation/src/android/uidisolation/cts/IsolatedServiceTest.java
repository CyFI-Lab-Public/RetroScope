/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.uidisolation.cts;

import android.app.Activity;
import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class IsolatedServiceTest extends ActivityInstrumentationTestCase2<ServiceRunnerActivity> {

    private ServiceRunnerActivity mActivity;

    private Instrumentation mInstrumentation;

    // The time we are ready to wait for the service to be done running the tests.
    private static int SERVICE_TIMEOUT = 15000;

    public IsolatedServiceTest() {
        super(ServiceRunnerActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();

        // We create a file with some content, the test will try to access it.
        FileOutputStream fos = mActivity.openFileOutput(PermissionTestService.FILE_NAME, 0);
        byte[] content = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        fos.write(content, 0, content.length);
        fos.close();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            File f = mActivity.getFileStreamPath(PermissionTestService.FILE_NAME);
            f.delete();
            mActivity.finish();
        } finally {
            super.tearDown();
        }
    }

    private void runServiceTest(final boolean isolated) {
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                if (isolated) {
                    mActivity.startIsolatedService();
                } else {
                    mActivity.startNonIsolatedService();
                }
            }
        });
        synchronized (mActivity) {
            if (mActivity.getSuccess() == null) {
                try {
                    mActivity.wait(SERVICE_TIMEOUT);
                } catch (InterruptedException ie) {
                }
            }
        }
        Boolean success = mActivity.getSuccess();
        assertNotNull("Test error: No success status available.", success);
        assertTrue(success.booleanValue());
    }

    public void testNonIsolatedService() {
        runServiceTest(false);
    }

    public void testIsolatedService() {
        runServiceTest(true);
    }
}

