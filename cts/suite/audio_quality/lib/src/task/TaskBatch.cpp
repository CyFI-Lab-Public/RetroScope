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
#include "Report.h"

#include "task/TaskBatch.h"

static const android::String8 STR_NAME("name");
static const android::String8 STR_VERSION("version");
static const android::String8 STR_DESCRIPTION("description");

TaskBatch::TaskBatch()
    :TaskGeneric(TaskGeneric::ETaskBatch)
{
    const android::String8* list[] = {&STR_NAME, &STR_VERSION, &STR_DESCRIPTION, NULL};
    registerSupportedStringAttributes(list);
}

TaskBatch::~TaskBatch()
{

}

bool TaskBatch::addChild(TaskGeneric* child)
{
    if (child->getType() != TaskGeneric::ETaskCase) {
        LOGE("TaskBatch::addChild wrong child type %d", child->getType());
        return false;
    }
    return TaskGeneric::addChild(child);
}

bool runAlways(TaskGeneric* child, void* data)
{
    child->run();
    return true;
}

TaskGeneric::ExecutionResult TaskBatch::run()
{
    android::String8 name;
    android::String8 version;

    if (!findStringAttribute(STR_NAME, name) || !findStringAttribute(STR_VERSION, version)) {
        LOGW("TaskBatch::run no name or version information");
    }
    MSG("= Test batch %s version %s started. =", name.string(),
            version.string());
    bool result = TaskGeneric::forEachChild(runAlways, NULL);
    MSG("= Finished Test batch =");
    return TaskGeneric::EResultOK;
}


