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

package com.android.gallery3d.app;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.android.gallery3d.util.ThreadPool;

public class BatchService extends Service {

    public class LocalBinder extends Binder {
        BatchService getService() {
            return BatchService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();
    private ThreadPool mThreadPool = new ThreadPool(1, 1);

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // The threadpool returned by getThreadPool must have only 1 thread
    // running at a time, as MenuExecutor (atrociously) depends on this
    // guarantee for synchronization.
    public ThreadPool getThreadPool() {
        return mThreadPool;
    }
}
