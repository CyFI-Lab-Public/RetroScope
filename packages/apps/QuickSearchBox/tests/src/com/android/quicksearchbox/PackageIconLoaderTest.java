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

package com.android.quicksearchbox;

import com.android.quicksearchbox.tests.CrashingIconProvider;
import com.android.quicksearchbox.util.NamedTaskExecutor;
import com.android.quicksearchbox.util.PriorityThreadFactory;
import com.android.quicksearchbox.util.SingleThreadNamedTaskExecutor;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.test.suitebuilder.annotation.MediumTest;

/**
 * Tests for {@link PackageIconLoader}.
 *
 */
@MediumTest
public class PackageIconLoaderTest extends IconLoaderTest {

    private ConsumerThread mThread;

    @Override
    public void setUp() throws Exception {
        mThread = new ConsumerThread();
        mThread.start();
        // we do this afterwards, as we need to have the thread set up (it calls create()).
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        mThread.exit();
        super.tearDown();
    }

    @Override
    protected IconLoader create() throws Exception {
        NamedTaskExecutor executor = new SingleThreadNamedTaskExecutor(
                new PriorityThreadFactory(Process.THREAD_PRIORITY_DEFAULT));
        return new PackageIconLoader(mContext, mContext.getPackageName(), mThread.getHandler(),
                executor);
    }

    public void testGetIconCrashingProvider() {
        String uri = "content://" + CrashingIconProvider.AUTHORITY + "/icon";
        assertNull(mLoader.getIcon(uri));
    }

    private class ConsumerThread extends Thread {
        private Handler mHandler;
        private final Object mSync = new Object();
        public ConsumerThread() {
        }
        @Override
        public void run() {
            Looper.prepare();
            synchronized (mSync) {
                mHandler = new Handler();
                mSync.notifyAll();
            }
            Looper.loop();
        }
        public Handler getHandler() {
            synchronized (mSync) {
                if (mHandler == null) {
                    try {
                        mSync.wait();
                    } catch (InterruptedException e) {
                    }
                }
                return mHandler;
            }
        }
        public void exit() {
            getHandler().post(new Runnable(){
                public void run() {
                    Looper.myLooper().quit();
                }});
        }
    }

}
