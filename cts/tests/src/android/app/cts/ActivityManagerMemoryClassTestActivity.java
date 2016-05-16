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
package android.app.cts;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

/**
 * {@link Activity} that allocates arrays of 256k until reaching 75% of the specified memory class.
 */
public class ActivityManagerMemoryClassTestActivity extends Activity {

    private static final String TAG = "ActivityManagerMemoryClassTest";

    private static final double FREE_MEMORY_PERCENTAGE = 0.75;

    private static final int ARRAY_BYTES_SIZE = 256 * 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        int memoryClass =
                intent.getIntExtra(ActivityManagerMemoryClassLaunchActivity.MEMORY_CLASS_EXTRA, -1);
        new AllocateMemoryTask(memoryClass).execute();
    }

    private class AllocateMemoryTask extends AsyncTask<Void, Void, Void> {

        private final int mMemoryClass;

        AllocateMemoryTask(int memoryClass) {
            this.mMemoryClass = memoryClass;
        }

        @Override
        protected Void doInBackground(Void... params) {
            int targetMbs = (int) (mMemoryClass * FREE_MEMORY_PERCENTAGE);
            int numArrays = targetMbs * 1024 * 1024 / ARRAY_BYTES_SIZE;
            Log.i(TAG, "Memory class: " + mMemoryClass + "mb Target memory: "
                    + targetMbs + "mb Number of arrays: " + numArrays);

            byte[][] arrays = new byte[numArrays][];
            for (int i = 0; i < arrays.length; i++) {
                Log.i(TAG, "Allocating array " + i + " of " + arrays.length
                        + " (" + (i * ARRAY_BYTES_SIZE / 1024 / 1024) + "mb)");
                arrays[i] = new byte[ARRAY_BYTES_SIZE];
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            setResult(RESULT_OK);
            finish();
        }
    }
}
