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


#ifndef CTSAUDIO_TASKSEQUENTIAL_H
#define CTSAUDIO_TASKSEQUENTIAL_H

#include <utils/String8.h>
#include <list>
#include "TaskGeneric.h"

class TaskAsync;

class TaskSequential: public TaskGeneric {
public:
    TaskSequential();
    virtual ~TaskSequential();
    virtual TaskGeneric::ExecutionResult run();
    virtual bool parseAttribute(const android::String8& name, const android::String8& value);
    /**
     * Queue async task for asynchronous execution (= call complete later)
     * If the task is already queued, it will not be queued again ,but will just return true.
     */
    bool queueAsyncTask(TaskAsync* task);

private:
    /**
     * Run all async tasks queued (= call complete) and dequeue them.
     * Execution will be continued even for error, and the 1st error result will be returned.
     */
    TaskGeneric::ExecutionResult runAsyncTasksQueued();

private:
    int mRepeatCount;
    android::String8 mIndexName;
    int mRepeatIndex;
    std::list<TaskAsync*> mAsyncTasks;
};


#endif // CTSAUDIO_TASKSEQUENTIAL_H
