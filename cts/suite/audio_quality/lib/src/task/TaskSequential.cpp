/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
#include <stdlib.h>
#include <algorithm>
#include "Log.h"
#include "StringUtil.h"
#include "task/TaskSequential.h"
#include "task/TaskCase.h"
#include "task/TaskAsync.h"

TaskSequential::TaskSequential()
    : TaskGeneric(TaskGeneric::ETaskSequential),
      mRepeatCount(1),
      mRepeatIndex(-1)
{

}

TaskSequential::~TaskSequential()
{

}


TaskGeneric::ExecutionResult TaskSequential::run()
{
    mRepeatIndex = -1;
    bool storeIndex = (mIndexName.length() == 0 ? false: true);
    if (storeIndex && !getTestCase()->registerIndex(mIndexName, mRepeatIndex)) {
        if (!getTestCase()->updateIndex(mIndexName, mRepeatIndex)) {
            LOGE("register/update of index %s failed", mIndexName.string());
            return TaskGeneric::EResultError;
        }
    }

    TaskGeneric::ExecutionResult firstError(TaskGeneric::EResultOK);

    for (mRepeatIndex = 0; mRepeatIndex < mRepeatCount; mRepeatIndex++) {
        LOGI("  TaskSequential index %s loop %d-th", mIndexName.string(), mRepeatIndex);
        if (storeIndex && !getTestCase()->updateIndex(mIndexName, mRepeatIndex)) {
            return TaskGeneric::EResultError;
        }
        std::list<TaskGeneric*>::iterator i = getChildren().begin();
        std::list<TaskGeneric*>::iterator end = getChildren().end();
        for (; i != end; i++) {
            TaskGeneric* child = *i;
            TaskGeneric::ExecutionResult result = child->run();
            if ((result != TaskGeneric::EResultOK) && (firstError == TaskGeneric::EResultOK)) {
                firstError = result;
                break;
            }
        }
        TaskGeneric::ExecutionResult result = runAsyncTasksQueued();
        if ((result != TaskGeneric::EResultOK) && (firstError == TaskGeneric::EResultOK)) {
                    firstError = result;
        }
        switch (firstError) {
        case TaskGeneric::EResultOK:
        case TaskGeneric::EResultContinue:
            // continue at the last index should be treated as OK
            firstError = TaskGeneric::EResultOK;
            break; // continue for loop
        case TaskGeneric:: EResultBreakOneLoop:
            return TaskGeneric::EResultOK;
        case TaskGeneric::EResultError:
        case TaskGeneric::EResultFail:
        case TaskGeneric::EResultPass:
            mRepeatIndex = mRepeatCount; //exit for loop
            break;
        }
    }
    // update to the loop exit value
    if (storeIndex && !getTestCase()->updateIndex(mIndexName, mRepeatIndex)) {
        return TaskGeneric::EResultError;
    }
    return firstError;
}

bool TaskSequential::queueAsyncTask(TaskAsync* task)
{
    std::list<TaskAsync*>::iterator it;
    it = std::find(mAsyncTasks.begin(), mAsyncTasks.end(), task);
    if (it != mAsyncTasks.end()) { // already queued
        return true;
    }
    mAsyncTasks.push_back(task);
    return true;
}

TaskGeneric::ExecutionResult TaskSequential::runAsyncTasksQueued()
{
    std::list<TaskAsync*>::iterator i = mAsyncTasks.begin();
    std::list<TaskAsync*>::iterator end = mAsyncTasks.end();
    TaskGeneric::ExecutionResult firstError(TaskGeneric::EResultOK);

    for (; i != end; i++) {
        TaskAsync* child = *i;
        TaskGeneric::ExecutionResult result = child->complete();
        if ((result != TaskGeneric::EResultOK) && (firstError == TaskGeneric::EResultOK)) {
            firstError = result;
        }
    }
    mAsyncTasks.clear();
    return firstError;
}


bool TaskSequential::parseAttribute(const android::String8& name, const android::String8& value)
{
    if (StringUtil::compare(name, "repeat") == 0) {
        mRepeatCount = atoi(value.string());
        if (mRepeatCount <= 0) {
            LOGE("TaskSequential::parseAttribute invalid value %s for key %s",
                    value.string(), name.string());
            return false;
        }
        return true;
    } else if (StringUtil::compare(name, "index") == 0) {
        mIndexName.append(value);
        LOGD("TaskSequential::parseAttribute index %s", mIndexName.string());
        return true;
    } else {
        return false;
    }
}
