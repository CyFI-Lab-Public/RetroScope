/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.gallery3d.filtershow.pipeline;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;

public class ProcessingTaskController implements Handler.Callback {
    private static final String LOGTAG = "ProcessingTaskController";

    private Context mContext;
    private HandlerThread mHandlerThread = null;
    private Handler mProcessingHandler = null;
    private int mCurrentType;
    private HashMap<Integer, ProcessingTask> mTasks = new HashMap<Integer, ProcessingTask>();

    public final static int RESULT = 1;
    public final static int UPDATE = 2;

    private final Handler mResultHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ProcessingTask task = mTasks.get(msg.what);
            if (task != null) {
                if (msg.arg1 == RESULT) {
                    task.onResult((ProcessingTask.Result) msg.obj);
                } else if (msg.arg1 == UPDATE) {
                    task.onUpdate((ProcessingTask.Update) msg.obj);
                } else {
                    Log.w(LOGTAG, "received unknown message! " + msg.arg1);
                }
            }
        }
    };

    @Override
    public boolean handleMessage(Message msg) {
        ProcessingTask task = mTasks.get(msg.what);
        if (task != null) {
            task.processRequest((ProcessingTask.Request) msg.obj);
            return true;
        }
        return false;
    }

    public ProcessingTaskController(Context context) {
        mContext = context;
        mHandlerThread = new HandlerThread("ProcessingTaskController",
                android.os.Process.THREAD_PRIORITY_FOREGROUND);
        mHandlerThread.start();
        mProcessingHandler = new Handler(mHandlerThread.getLooper(), this);
    }

    public Handler getProcessingHandler() {
        return mProcessingHandler;
    }

    public Handler getResultHandler() {
        return mResultHandler;
    }

    public int getReservedType() {
        return mCurrentType++;
    }

    public Context getContext() {
        return mContext;
    }

    public void add(ProcessingTask task) {
        task.added(this);
        mTasks.put(task.getType(), task);
    }

    public void quit() {
        mHandlerThread.quit();
    }
}
