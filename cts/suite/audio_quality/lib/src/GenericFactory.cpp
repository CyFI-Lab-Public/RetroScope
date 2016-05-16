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
#include "Log.h"
#include "GenericFactory.h"
#include "ClientImpl.h"
#include "task/TaskAll.h"

ClientInterface* GenericFactory::createClientInterface()
{
    return new ClientImpl();
}

TaskGeneric* GenericFactory::createTask(TaskGeneric::TaskType type)
{
    TaskGeneric* task;
    switch(type) {
    case TaskGeneric::ETaskBatch:
        task = new TaskBatch();
        break;
    case TaskGeneric::ETaskCase:
        task = new TaskCase();
        break;
    case TaskGeneric::ETaskSequential:
        task = new TaskSequential();
        break;
    case TaskGeneric::ETaskProcess:
        task = new TaskProcess();
        break;
    case TaskGeneric::ETaskInput:
        task = new TaskInput();
        break;
    case TaskGeneric::ETaskOutput:
        task = new TaskOutput();
        break;
    case TaskGeneric::ETaskSound:
        task = new TaskSound();
        break;
    case TaskGeneric::ETaskSave:
        task = new TaskSave();
        break;
    // simple elements without its own class
    case TaskGeneric::ETaskSetup:
    case TaskGeneric::ETaskAction:
        task = new TaskGeneric(type);
        break;
    case TaskGeneric::ETaskMessage:
        task = new TaskMessage();
        break;
    case TaskGeneric::ETaskDownload:
        task = new TaskDownload();
        break;
    default:
        LOGE("GenericFactory::createTask unsupported type %d", type);
        return NULL;
    }
    LOGD("GenericFactory::createTask 0x%x, type %d", task, type);
    return task;
}
