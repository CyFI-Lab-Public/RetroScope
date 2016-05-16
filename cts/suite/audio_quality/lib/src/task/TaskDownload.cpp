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

#include <UniquePtr.h>
#include "Log.h"
#include "audio/AudioSignalFactory.h"
#include "audio/RemoteAudio.h"
#include "StringUtil.h"
#include "task/TaskCase.h"
#include "task/TaskDownload.h"

static const android::String8 STR_ID("id");

TaskDownload::TaskDownload()
    : TaskGeneric(TaskGeneric::ETaskDownload)
{
    const android::String8* list[] = {&STR_ID, NULL};
    registerSupportedStringAttributes(list);
}

TaskDownload::~TaskDownload()
{

}

TaskGeneric::ExecutionResult TaskDownload::run()
{
    android::String8 id;
    if (!findStringAttribute(STR_ID, id)) {
        LOGE("TaskDownload::run %s string not found", STR_ID.string());
        return TaskGeneric::EResultError;
    }

    android::sp<Buffer> buffer = getTestCase()->findBuffer(id);
    if (buffer.get() == NULL) {
        LOGE("TaskDownload::run cannot find buffer %s", id.string());
        return TaskGeneric::EResultError;
    }
    int downloadId;
    if (!getTestCase()->getRemoteAudio()->downloadData(id, buffer, downloadId)) {
        return TaskGeneric::EResultError;
    }
    LOGI("Downloaded buffer %s to DUT with id %d", id.string(), downloadId);
    return TaskGeneric::EResultOK;
}



