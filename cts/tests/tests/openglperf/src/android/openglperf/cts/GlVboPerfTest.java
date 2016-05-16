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

package android.openglperf.cts;

import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class GlVboPerfTest extends
        ActivityInstrumentationTestCase2<GlPlanetsActivity> {
    private static final String TAG = "GlVboPerfTest";
    private static final int NUM_FRAMES_TO_RENDER = 100;
    private static final long RENDERING_TIMEOUT = 5 * 60;
    // 30% of fps_no_vbo is allowed to compensate variations in measurement
    private static final float FPS_COMPARISON_MARGIN = 0.3f;
    // the worst case should be above 70% of the best case
    private static final float FPS_MIN_MAX_COMPARISON_PERCENTILE = 0.7f;

    private float mFps;
    private int mNumTriangles;

    public GlVboPerfTest() {
        super(GlPlanetsActivity.class);
    }

    public void testVboWithVaryingIndexBufferNumbers() throws Exception {
        final int[] numIndexBuffers = {1, 10, 100, 200, 400}; // per vertex buffer
        float[] fpsVbo = new float[numIndexBuffers.length];
        float[] fpsNonVbo = new float[numIndexBuffers.length];

        for (int i = 0; i < numIndexBuffers.length; i++) {
            runRendering(0, true, true, numIndexBuffers[i]);
            fpsVbo[i] = mFps;
            runRendering(0, true, false, numIndexBuffers[i]);
            fpsNonVbo[i] = mFps;
        }
        StringBuilder msgIndex = new StringBuilder();
        StringBuilder msgVbo = new StringBuilder();
        StringBuilder msgNonVbo = new StringBuilder();
        msgIndex.append("index buffer ");
        msgVbo.append("Vbo ");
        msgNonVbo.append("Non-Vbo ");
        for (int i = 0; i < numIndexBuffers.length; i++) {
            msgIndex.append(numIndexBuffers[i]).append(" ");
            msgVbo.append(fpsVbo[i]).append(" ");
            msgNonVbo.append(fpsNonVbo[i]).append(" ");
        }
        Log.i(TAG, msgIndex.toString());
        Log.i(TAG, msgVbo.toString());
        Log.i(TAG, msgNonVbo.toString());

        float[] minMaxVbo = findMinMax(fpsVbo);
        float[] minMaxNonVbo = findMinMax(fpsNonVbo);

        float delta = minMaxVbo[1] - (1f - FPS_COMPARISON_MARGIN)
                * minMaxNonVbo[1];
        assertTrue("VBO performance worse than non-VBO " + msgVbo + msgNonVbo, delta > 0f);
        assertTrue(
                "Too much FPS drop for VBO case " + msgVbo,
                minMaxVbo[0] > (FPS_MIN_MAX_COMPARISON_PERCENTILE * minMaxVbo[1]));
        assertTrue(
                "Too much FPS drop for No VBO case " + msgNonVbo,
                minMaxNonVbo[0] > (FPS_MIN_MAX_COMPARISON_PERCENTILE * minMaxNonVbo[1]));
    }

    public void testVboVsNonVboPerfGeometry0() throws Exception {
        doRunVboVsNonVboPerfTest(0);
    }

    public void testVboVsNonVboPerfGeometry1() throws Exception {
        doRunVboVsNonVboPerfTest(4);
    }

    private void runRendering(int numPlanets, boolean useVboVertex, boolean useVboIndex,
            int numIndexBuffers) throws Exception {
        Intent intent = new Intent();
        intent.putExtra(GlPlanetsActivity.INTENT_EXTRA_NUM_FRAMES,
                NUM_FRAMES_TO_RENDER);
        intent.putExtra(GlPlanetsActivity.INTENT_EXTRA_NUM_PLANETS, numPlanets);
        intent.putExtra(GlPlanetsActivity.INTENT_EXTRA_USE_VBO_VERTICES, useVboVertex);
        intent.putExtra(GlPlanetsActivity.INTENT_EXTRA_USE_VBO_INDICES, useVboIndex);
        intent.putExtra(GlPlanetsActivity.INTENT_EXTRA_NUM_INDEX_BUFFERS, numIndexBuffers);

        setActivityIntent(intent);
        final GlPlanetsActivity activity = getActivity();
        boolean waitResult = activity
                .waitForGlPlanetsCompletionWithTimeout(RENDERING_TIMEOUT);
        assertTrue("timeout while waiting for rendering completion", waitResult);

        mFps = activity.getAverageFps();
        mNumTriangles = activity.getNumTriangles();

        cleanUpActivity();
    }

    private void cleanUpActivity()  throws Exception {
        // finish the current activity and do clean-up so that a new activity
        // can be launched in the same test run
        super.tearDown();
        super.setUp();
        // wait until clean-up / set-up finishes
        getInstrumentation().waitForIdleSync();
    }

    private void doRunVboVsNonVboPerfTest(int numPlanets) throws Exception {
        runRendering(numPlanets, true, true, 1); // VBO
        int numTrianglesVbo = mNumTriangles;
        float fpsVbo = mFps;
        runRendering(numPlanets, false, false, 1); // non-VBO

        assertTrue("Number of triangles mismatch",
                numTrianglesVbo == mNumTriangles);

        // Margin amount of error is allowed due to measuring irregularity
        float delta = fpsVbo - (1f - FPS_COMPARISON_MARGIN) * mFps;
        StringBuilder testMsg = new StringBuilder();
        testMsg.append("VBO performance worse than non-VBO ").append(fpsVbo).append(" ");
        testMsg.append(mFps);
        assertTrue(testMsg.toString(), delta > 0f);
    }

    private float[] findMinMax(float[] data) {
        float min = data[0];
        float max = data[0];

        for (int i = 1; i < data.length; i++) {
            if (data[i] > max) max = data[i];
            if (data[i] < min) min = data[i];
        }
        float[] result = {min, max};
        return result;
    }
}
