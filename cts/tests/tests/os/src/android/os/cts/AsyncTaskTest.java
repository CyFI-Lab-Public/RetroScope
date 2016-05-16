/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.os.cts;


import android.cts.util.PollingCheck;
import android.os.AsyncTask;
import android.test.InstrumentationTestCase;

import java.util.concurrent.TimeUnit;

public class AsyncTaskTest extends InstrumentationTestCase {
    private static final long COMPUTE_TIME = 1000;
    private static final long RESULT = 1000;
    private static final Integer[] UPDATE_VALUE = { 0, 1, 2 };
    private static final long DURATION = 2000;
    private static final String[] PARAM = { "Test" };

    private static MyAsyncTask mAsyncTask;

    public void testAsyncTask() throws Throwable {
        doTestAsyncTask(0);
    }

    public void testAsyncTaskWithTimeout() throws Throwable {
        doTestAsyncTask(DURATION);
    }

    private void doTestAsyncTask(final long timeout) throws Throwable {
        startAsyncTask();
        if (timeout > 0) {
            assertEquals(RESULT, mAsyncTask.get(DURATION, TimeUnit.MILLISECONDS).longValue());
        } else {
            assertEquals(RESULT, mAsyncTask.get().longValue());
        }

        // wait for the task to finish completely (including onPostResult()).
        new PollingCheck(DURATION) {
            protected boolean check() {
                return mAsyncTask.getStatus() == AsyncTask.Status.FINISHED;
            }
        }.run();

        assertTrue(mAsyncTask.isOnPreExecuteCalled);
        assert(mAsyncTask.hasRun);
        assertEquals(PARAM.length, mAsyncTask.parameters.length);
        for (int i = 0; i < PARAM.length; i++) {
            assertEquals(PARAM[i], mAsyncTask.parameters[i]);
        }
        // even though the background task has run, the onPostExecute() may not have been
        // executed yet and the progress update may not have been processed. Wait until the task
        // has completed, which guarantees that onPostExecute has been called.

        assertEquals(RESULT, mAsyncTask.postResult.longValue());
        assertEquals(AsyncTask.Status.FINISHED, mAsyncTask.getStatus());

        if (mAsyncTask.exception != null) {
            throw mAsyncTask.exception;
        }

        // wait for progress update to be processed (happens asynchronously)
        new PollingCheck(DURATION) {
            protected boolean check() {
                return mAsyncTask.updateValue != null;
            }
        }.run();
        assertEquals(UPDATE_VALUE.length, mAsyncTask.updateValue.length);
        for (int i = 0; i < UPDATE_VALUE.length; i++) {
            assertEquals(UPDATE_VALUE[i], mAsyncTask.updateValue[i]);
        }

        runTestOnUiThread(new Runnable() {
            public void run() {
                try {
                    // task should not be allowed to execute twice
                    mAsyncTask.execute(PARAM);
                    fail("Failed to throw exception!");
                } catch (IllegalStateException e) {
                    // expected
                }
            }
        });
    }

    public void testCancelWithInterrupt() throws Throwable {
        startAsyncTask();
        Thread.sleep(COMPUTE_TIME / 2);
        assertTrue(mAsyncTask.cancel(true));
        // already cancelled
        assertFalse(mAsyncTask.cancel(true));
        Thread.sleep(DURATION);
        assertTrue(mAsyncTask.isCancelled());
        assertTrue(mAsyncTask.isOnCancelledCalled);
        assertNotNull(mAsyncTask.exception);
        assertTrue(mAsyncTask.exception instanceof InterruptedException);
    }

    public void testCancel() throws Throwable {
        startAsyncTask();
        Thread.sleep(COMPUTE_TIME / 2);
        assertTrue(mAsyncTask.cancel(false));
        // already cancelled
        assertFalse(mAsyncTask.cancel(false));
        Thread.sleep(DURATION);
        assertTrue(mAsyncTask.isCancelled());
        assertTrue(mAsyncTask.isOnCancelledCalled);
        assertNull(mAsyncTask.exception);
    }

    public void testCancelTooLate() throws Throwable {
        startAsyncTask();
        Thread.sleep(DURATION);
        assertFalse(mAsyncTask.cancel(false));
        assertTrue(mAsyncTask.isCancelled());
        assertFalse(mAsyncTask.isOnCancelledCalled);
        assertNull(mAsyncTask.exception);
    }

    private void startAsyncTask() throws Throwable {
        runTestOnUiThread(new Runnable() {
            public void run() {
                mAsyncTask = new MyAsyncTask();
                assertEquals(AsyncTask.Status.PENDING, mAsyncTask.getStatus());
                assertEquals(mAsyncTask, mAsyncTask.execute(PARAM));
                assertEquals(AsyncTask.Status.RUNNING, mAsyncTask.getStatus());
            }
        });
    }

    private static class MyAsyncTask extends AsyncTask<String, Integer, Long> {
        public boolean isOnCancelledCalled;
        public boolean isOnPreExecuteCalled;
        public boolean hasRun;
        public Exception exception;
        public Long postResult;
        public Integer[] updateValue;
        public String[] parameters;

        @Override
        protected Long doInBackground(String... params) {
            hasRun = true;
            parameters = params;
            try {
                publishProgress(UPDATE_VALUE);
                Thread.sleep(COMPUTE_TIME);
            } catch (Exception e) {
                exception = e;
            }
            return RESULT;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            isOnCancelledCalled = true;
        }

        @Override
        protected void onPostExecute(Long result) {
            super.onPostExecute(result);
            postResult = result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isOnPreExecuteCalled = true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            updateValue = values;
        }
    }
}
