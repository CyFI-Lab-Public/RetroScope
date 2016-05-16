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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Activity which runs GL test and measure FPS with given parameters passed from
 * Intent. For the meaning of parameters, check {@PlanetsRenderingParam}
 */
public class GlPlanetsActivity extends Activity implements
        RenderCompletionListener {
    public static final String INTENT_EXTRA_NUM_PLANETS = "numPlanets";
    public static final String INTENT_EXTRA_USE_VBO_VERTICES = "useVboVertices";
    public static final String INTENT_EXTRA_USE_VBO_INDICES = "useVboIndiices";
    public static final String INTENT_EXTRA_NUM_FRAMES = "numFrames";
    public static final String INTENT_EXTRA_NUM_INDEX_BUFFERS = "numIndexBuffers";

    public static final String INTENT_RESULT_FPS = "fps";
    public static final String INTENT_RESULT_NUM_TRIANGLES = "numTrigngles";

    private final Semaphore mSem = new Semaphore(0);
    private float mAverageFps;
    private int mNumTriangles;
    private int[] mFrameInterval;

    private PlanetsSurfaceView mView;

    public boolean waitForGlPlanetsCompletionWithTimeout(long timeoutInSecs)
            throws InterruptedException {
        return mSem.tryAcquire(timeoutInSecs, TimeUnit.SECONDS);
    }

    public float getAverageFps() {
        return mAverageFps;
    }

    public int getNumTriangles() {
        return mNumTriangles;
    }

    /**
     * Time interval between each frame's rendering in ms.
     * The first value will be invalid, so client should discard them.
     * @return can return null if INTENT_EXTRA_NUM_FRAMES was not set in intent.
     */
    public int[] getFrameInterval() {
        return mFrameInterval;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent intent = getIntent();
        PlanetsRenderingParam param = new PlanetsRenderingParam();
        param.mNumPlanets = intent.getIntExtra(INTENT_EXTRA_NUM_PLANETS, param.mNumPlanets);
        param.mUseVboForVertices = intent.getBooleanExtra(INTENT_EXTRA_USE_VBO_VERTICES,
                param.mUseVboForVertices);
        param.mUseVboForIndices = intent.getBooleanExtra(INTENT_EXTRA_USE_VBO_INDICES,
                param.mUseVboForIndices);
        param.mNumFrames = intent.getIntExtra(INTENT_EXTRA_NUM_FRAMES, param.mNumFrames);
        param.mNumIndicesPerVertex = intent.getIntExtra(INTENT_EXTRA_NUM_INDEX_BUFFERS,
                param.mNumIndicesPerVertex);
        mView = new PlanetsSurfaceView(this, param, this);
        setContentView(mView);
    }

    @Override
    public void onRenderCompletion(float averageFps, int numTriangles,  int[] frameInterval) {
        mAverageFps = averageFps;
        mNumTriangles = numTriangles;
        mFrameInterval = frameInterval;
        mSem.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mView.onPause();
    }
}