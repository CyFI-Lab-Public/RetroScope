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
import android.os.Bundle;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * {@link Activity} that just launches {@link ActivityManagerMemoryClassTestActivity} and
 * returns the result of that activity.
 */
public class ActivityManagerMemoryClassLaunchActivity extends Activity {

    public static final String MEMORY_CLASS_EXTRA = "activityMemoryClass";

    private static final int TEST_ACTIVITY_REQUEST_CODE = 1337;

    private final CountDownLatch mLatch = new CountDownLatch(1);

    private int mChildResult = RESULT_CANCELED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start the activity that runs in a separate process to do the actual testing,
        // since the test itself cannot start an activity in a separate process. A separate
        // process is used to avoid including the overhead of the test instrumentation process.

        Intent intent = getIntent();
        int memoryClass = intent.getIntExtra(MEMORY_CLASS_EXTRA, -1);

        Intent testIntent = new Intent(this, ActivityManagerMemoryClassTestActivity.class);
        testIntent.putExtra(MEMORY_CLASS_EXTRA, memoryClass);
        startActivityForResult(testIntent, TEST_ACTIVITY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1337) {
            synchronized (this) {
                mChildResult = resultCode;
            }
        } else {
            throw new IllegalStateException("Request code: " + requestCode);
        }
    }

    public int getResult() throws InterruptedException {
        mLatch.await(5, TimeUnit.SECONDS);
        synchronized (this) {
            return mChildResult;
        }
    }
}
