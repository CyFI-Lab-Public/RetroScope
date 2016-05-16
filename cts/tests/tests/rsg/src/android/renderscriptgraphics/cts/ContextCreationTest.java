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

package android.renderscriptgraphics.cts;

import android.renderscript.*;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.util.Log;

/**
 * Tests for the RenderScriptGL class.
 */
public class ContextCreationTest extends
        ActivityInstrumentationTestCase2<RenderscriptGLStubActivity> {

    private static final int NUM_RECREATE_ITERATIONS_WITHOUT_DELAY = 50;

    private static final int NUM_RECREATE_ITERATIONS_WITH_DELAY = 10;

    private static final int RECREATE_DELAY = 100;

    private static final boolean LOG_RECREATE = true;

    private static final String TAG = "ContextCreationTest";

    private RenderscriptGLStubActivity mActivity;

    public ContextCreationTest() {
        super(RenderscriptGLStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        mActivity.destroyAll();
        super.tearDown();
    }

    /**
     * Test repeated recreation of the renderscript context with a
     * delay in between to detect hangs in the graphics drivers
     */
    @UiThreadTest
    public void testCreationWithDelay() throws InterruptedException {
        for (int i = 0; i < NUM_RECREATE_ITERATIONS_WITH_DELAY; i++) {
            Thread.sleep(RECREATE_DELAY);
            if (LOG_RECREATE) {
                Log.w(TAG, "Recreate (w/ delay) step " + i + " - pause");
            }
            mActivity.recreateView();
        }
    }

    /**
     * Test repeated recreation of the renderscript context to
     * detect hangs in the graphics drivers
     */
    @UiThreadTest
    public void testCreationWithoutDelay() {
        for (int i = 0; i < NUM_RECREATE_ITERATIONS_WITHOUT_DELAY; i++) {
            if (LOG_RECREATE) {
                Log.w(TAG, "Recreate (no delay) step " + i);
            }
            mActivity.recreateView();
        }
    }

    /**
     * Test repeated recreation of the renderscript context with two
     * created at a time to detect hangs in the graphics drivers
     */
    @UiThreadTest
    public void testMultiviewCreationWithoutDelay() {
        for (int i = 0; i < NUM_RECREATE_ITERATIONS_WITHOUT_DELAY; i++) {
            if (LOG_RECREATE) {
                Log.w(TAG, "Recreate multiview (no delay) step " + i + " - pause");
            }
            mActivity.recreateMultiView();
        }
    }
}
