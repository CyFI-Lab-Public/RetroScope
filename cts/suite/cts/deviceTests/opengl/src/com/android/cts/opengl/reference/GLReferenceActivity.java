/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.cts.opengl.reference;

import com.android.cts.opengl.GLActivityIntentKeys;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.concurrent.Semaphore;

public class GLReferenceActivity extends Activity {

    private final static int GAME_ACTIVITY_CODE = 1;

    private volatile Exception mException;
    private int mNumFrames;
    private int mTimeout;

    public double[] mSetUpTimes;
    public double[] mUpdateTimes;
    public double[] mRenderTimes;

    private Semaphore mSemaphore = new Semaphore(0);

    @Override
    public void onCreate(Bundle data) {
        super.onCreate(data);
        Intent intent = getIntent();
        mNumFrames = intent.getIntExtra(GLActivityIntentKeys.INTENT_EXTRA_NUM_FRAMES, 0);
        mTimeout = intent.getIntExtra(GLActivityIntentKeys.INTENT_EXTRA_TIMEOUT, 0);

        // Start benchmark
        intent = new Intent(this, GLGameActivity.class);
        intent.putExtra(GLActivityIntentKeys.INTENT_EXTRA_NUM_FRAMES, mNumFrames);
        intent.putExtra(GLActivityIntentKeys.INTENT_EXTRA_TIMEOUT, mTimeout);
        startActivityForResult(intent, GAME_ACTIVITY_CODE);
    }

    public void waitForCompletion() throws Exception {
        // Wait for semiphore.
        mSemaphore.acquire();
        if (mException != null) {
            throw mException;
        }
    }

    private synchronized void setException(Exception e) {
        if (mException == null) {
            mException = e;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GAME_ACTIVITY_CODE) {
            if (resultCode == RESULT_OK) {
                // Benchmark passed
                mSetUpTimes = data.getDoubleArrayExtra(GLGameActivity.SET_UP_TIME);
                mUpdateTimes = data.getDoubleArrayExtra(GLGameActivity.UPDATE_TIMES);
                mRenderTimes = data.getDoubleArrayExtra(GLGameActivity.RENDER_TIMES);
            } else {
                setException(new Exception("Benchmark failed to run"));
            }
            // Release semiphore.
            mSemaphore.release();
            finish();
        }
    }

}
