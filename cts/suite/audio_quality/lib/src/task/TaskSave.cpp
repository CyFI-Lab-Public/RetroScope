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

#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>

#include <UniquePtr.h>

#include "Log.h"
#include "FileUtil.h"
#include "Report.h"
#include "StringUtil.h"
#include "task/TaskCase.h"
#include "task/TaskGeneric.h"
#include "task/TaskSave.h"

static const android::String8 STR_FILE("file");
static const android::String8 STR_REPORT("report");

TaskSave::TaskSave()
    : TaskGeneric(TaskGeneric::ETaskSave)
{
    const android::String8* list[] = {&STR_FILE, &STR_REPORT, NULL};
    registerSupportedStringAttributes(list);
}

TaskSave::~TaskSave()
{

}

bool TaskSave::handleFile()
{
    android::String8 fileValue;
    if (!findStringAttribute(STR_FILE, fileValue)) {
        LOGI("no saving to file");
        return true; // true as there is no need to save
    }

    UniquePtr<std::vector<android::String8> > list(StringUtil::split(fileValue, ','));
    std::vector<android::String8>* listp = list.get();
    if (listp == NULL) {
        LOGE("alloc failed");
        return false;
    }

    android::String8 dirName;
    if (!FileUtil::prepare(dirName)) {
        LOGE("cannot prepare report dir");
        return false;
    }
    android::String8 caseName;
    if (!getTestCase()->getCaseName(caseName)) {
        return false;
    }
    dirName.appendPath(caseName);
    int result = mkdir(dirName.string(), S_IRWXU);
    if ((result == -1) && (errno != EEXIST)) {
        LOGE("mkdir of save dir %s failed, error %d", dirName.string(), errno);
        return false;
    }

    for (size_t i = 0; i < listp->size(); i++) {
        UniquePtr<std::list<TaskCase::BufferPair> > buffers(
                getTestCase()->findAllBuffers((*listp)[i]));
        std::list<TaskCase::BufferPair>* buffersp = buffers.get();
        if (buffersp == NULL) {
            LOGE("no buffer for given pattern %s", ((*listp)[i]).string());
            return false;
        }
        std::list<TaskCase::BufferPair>::iterator it = buffersp->begin();
        std::list<TaskCase::BufferPair>::iterator end = buffersp->end();
        for (; it != end; it++) {
            android::String8 fileName(dirName);
            fileName.appendPath(it->first);
            if (!it->second->saveToFile(fileName)) {
                LOGE("save failed");
                return false;
            }
        }
    }
    return true;
}

bool TaskSave::handleReport()
{
    android::String8 reportValue;
    if (!findStringAttribute(STR_REPORT, reportValue)) {
        LOGI("no saving to report");
        return true; // true as there is no need to save
    }

    UniquePtr<std::vector<android::String8> > list(StringUtil::split(reportValue, ','));
    std::vector<android::String8>* listp = list.get();
    if (listp == NULL) {
        LOGE("alloc failed");
        return false;
    }
    MSG("=== Values stored ===");
    android::String8 details;
    for (size_t i = 0; i < listp->size(); i++) {
        UniquePtr<std::list<TaskCase::ValuePair> > values(
                getTestCase()->findAllValues((*listp)[i]));
        std::list<TaskCase::ValuePair>* valuesp = values.get();
        if (valuesp == NULL) {
            LOGE("no value for given pattern %s", ((*listp)[i]).string());
            return false;
        }
        std::list<TaskCase::ValuePair>::iterator it = values->begin();
        std::list<TaskCase::ValuePair>::iterator end = values->end();

        for (; it != end; it++) {
            if (it->second.getType() == TaskCase::Value::ETypeDouble) {
                details.appendFormat("   %s: %f\n", it->first.string(), it->second.getDouble());
            } else { //64bit int
                details.appendFormat("   %s: %lld\n", it->first.string(), it->second.getInt64());
            }
        }
        MSG("%s", details.string());
    }
    getTestCase()->setDetails(details);
    return true;
}

TaskGeneric::ExecutionResult TaskSave::run()
{
    bool failed = false;
    if (!handleFile()) {
        failed = true;
    }
    if (!handleReport()) {
        failed = true;
    }
    if (failed) {
        return TaskGeneric::EResultError;
    } else {
        return TaskGeneric::EResultOK;
    }
}


