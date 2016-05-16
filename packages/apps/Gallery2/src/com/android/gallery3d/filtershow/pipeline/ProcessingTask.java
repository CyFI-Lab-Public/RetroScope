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
import android.os.Message;

public abstract class ProcessingTask {
    private ProcessingTaskController mTaskController;
    private Handler mProcessingHandler;
    private Handler mResultHandler;
    private int mType;
    private static final int DELAY = 300;

    static interface Request {}
    static interface Update {}
    static interface Result {}

    public boolean postRequest(Request message) {
        Message msg = mProcessingHandler.obtainMessage(mType);
        msg.obj = message;
        if (isPriorityTask()) {
            if (mProcessingHandler.hasMessages(getType())) {
                return false;
            }
            mProcessingHandler.sendMessageAtFrontOfQueue(msg);
        } else if (isDelayedTask()) {
            if (mProcessingHandler.hasMessages(getType())) {
                mProcessingHandler.removeMessages(getType());
            }
            mProcessingHandler.sendMessageDelayed(msg, DELAY);
        } else {
            mProcessingHandler.sendMessage(msg);
        }
        return true;
    }

    public void postUpdate(Update message) {
        Message msg = mResultHandler.obtainMessage(mType);
        msg.obj = message;
        msg.arg1 = ProcessingTaskController.UPDATE;
        mResultHandler.sendMessage(msg);
    }

    public void processRequest(Request message) {
        Object result = doInBackground(message);
        Message msg = mResultHandler.obtainMessage(mType);
        msg.obj = result;
        msg.arg1 = ProcessingTaskController.RESULT;
        mResultHandler.sendMessage(msg);
    }

    public void added(ProcessingTaskController taskController) {
        mTaskController = taskController;
        mResultHandler = taskController.getResultHandler();
        mProcessingHandler = taskController.getProcessingHandler();
        mType = taskController.getReservedType();
    }

    public int getType() {
        return mType;
    }

    public Context getContext() {
        return mTaskController.getContext();
    }

    public abstract Result doInBackground(Request message);
    public abstract void onResult(Result message);
    public void onUpdate(Update message) {}
    public boolean isPriorityTask() { return false; }
    public boolean isDelayedTask() { return false; }
}
