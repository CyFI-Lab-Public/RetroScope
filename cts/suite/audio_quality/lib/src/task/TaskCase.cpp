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

#include <sys/types.h>
#include <regex.h>
#include <stdlib.h>
#include <stdio.h>

#include "Log.h"
#include "audio/RemoteAudio.h"
#include "ClientImpl.h"
#include "Report.h"
#include "Settings.h"
#include "StringUtil.h"
#include "task/TaskCase.h"

static const android::String8 STR_NAME("name");
static const android::String8 STR_VERSION("version");
static const android::String8 STR_DESCRIPTION("description");

TaskCase::TaskCase()
    : TaskGeneric(TaskGeneric::ETaskCase),
      mClient(NULL)
{
    const android::String8* list[] = {&STR_NAME, &STR_VERSION, &STR_DESCRIPTION, NULL};
    registerSupportedStringAttributes(list);
}

TaskCase::~TaskCase()
{
    delete mClient;
}

bool TaskCase::getCaseName(android::String8& name) const
{
    if (!findStringAttribute(STR_NAME, name)) {
        LOGW("TaskCase no name");
        return false;
    }
    return true;
}

bool TaskCase::addChild(TaskGeneric* child)
{
    if ((child->getType() != TaskGeneric::ETaskSetup)
            &&  (child->getType() != TaskGeneric::ETaskAction)
            &&  (child->getType() != TaskGeneric::ETaskSave)) {
        LOGE("TestCase::addChild wrong child type %d", child->getType());
        return false;
    }
    return TaskGeneric::addChild(child);
}

template <typename T> bool registerGeneric(
        typename std::map<android::String8, T>& map,
        const android::String8& name, T& data)
{
    typename std::map<android::String8, T>::iterator it;
    it = map.find(name);
    if (it != map.end()) {
        LOGV("registerGeneric key %s already registered", name.string());
        return false;
    }
    LOGD("registerGeneric registered key %s", name.string());
    map[name] = data;
    return true;
}

template <typename T> bool findGeneric(typename std::map<android::String8, T>& map,
        const android::String8& name, T& data)
{
    LOGD("findGeneric key %s", name.string());
    typename std::map<android::String8, T>::iterator it;
    it = map.find(name);
    if (it == map.end()) {
        return false;
    }
    data = it->second;
    return true;
}

template <typename T> bool updateGeneric(typename std::map<android::String8, T>& map,
        const android::String8& name, T& data)
{
    LOGD("updateGeneric key %s", name.string());
    typename std::map<android::String8, T>::iterator it;
    it = map.find(name);
    if (it == map.end()) {
        return false;
    }
    it->second = data;
    return true;
}

// return all the matches for the given regular expression.
// name string and the data itself is copied.
template <typename T> typename std::list<std::pair<android::String8, T> >* findAllGeneric(
        typename std::map<android::String8, T>& map, const char* re)
{
    regex_t regex;
    if (regcomp(&regex, re, REG_EXTENDED | REG_NOSUB) != 0) {
        LOGE("regcomp failed");
        return NULL;
    }
    typename std::map<android::String8, T>::iterator it;
    typename std::list<std::pair<android::String8, T> >* list = NULL;
    for (it = map.begin(); it != map.end(); it++) {
        if (regexec(&regex, it->first, 0, NULL, 0) == 0) {
            if (list == NULL) { // create only when found
                list = new std::list<std::pair<android::String8, T> >();
                if (list == NULL) {
                    regfree(&regex);
                    return NULL;
                }
            }
            typename std::pair<android::String8, T> match(it->first, it->second);
            list->push_back(match);
        }
    }
    regfree(&regex);
    return list;
}


bool TaskCase::registerBuffer(const android::String8& orig, android::sp<Buffer>& buffer)
{
    android::String8 translated;
    if (!translateVarName(orig, translated)) {
        return false;
    }
    return registerGeneric<android::sp<Buffer> >(mBufferList, translated, buffer);
}

bool TaskCase::updateBuffer(const android::String8& orig, android::sp<Buffer>& buffer)
{
    android::String8 translated;
    if (!translateVarName(orig, translated)) {
        return false;
    }
    return updateGeneric<android::sp<Buffer> >(mBufferList, translated, buffer);
}

android::sp<Buffer> TaskCase::findBuffer(const android::String8& orig)
{
    android::String8 translated;
    android::sp<Buffer> result;
    if (!translateVarName(orig, translated)) {
        return result;
    }
    findGeneric<android::sp<Buffer> >(mBufferList, translated, result);
    return result;
}

std::list<TaskCase::BufferPair>* TaskCase::findAllBuffers(const android::String8& re)
{
    android::String8 translated;
    if (!translateVarName(re, translated)) {
        return NULL;
    }
    return findAllGeneric<android::sp<Buffer> >(mBufferList, translated.string());
}


bool TaskCase::registerValue(const android::String8& orig, Value& val)
{
    android::String8 translated;
    if (!translateVarName(orig, translated)) {
        return false;
    }
    LOGD("str %x", translated.string());
    return registerGeneric<Value>(mValueList, translated, val);
}

bool TaskCase::updateValue(const android::String8& orig, Value& val)
{
    android::String8 translated;
    if (!translateVarName(orig, translated)) {
        return false;
    }
    return updateGeneric<Value>(mValueList, translated, val);
}

bool TaskCase::findValue(const android::String8& orig, Value& val)
{
    android::String8 translated;
    if (!translateVarName(orig, translated)) {
        return false;
    }
    return findGeneric<Value>(mValueList, translated, val);
}

std::list<TaskCase::ValuePair>* TaskCase::findAllValues(const android::String8& re)
{
    android::String8 translated;
    if (!translateVarName(re, translated)) {
        return NULL;
    }
    return findAllGeneric<Value>(mValueList, translated.string());
}

bool TaskCase::registerIndex(const android::String8& name, int value)
{
    return registerGeneric<int>(mIndexList, name, value);
}

bool TaskCase::updateIndex(const android::String8& name, int value)
{
    return updateGeneric<int>(mIndexList, name, value);
}

bool TaskCase::findIndex(const android::String8& name, int& val)
{
    return findGeneric<int>(mIndexList, name, val);
}

std::list<TaskCase::IndexPair>* TaskCase::findAllIndices(const android::String8& re)
{
    android::String8 translated;
    if (!translateVarName(re, translated)) {
        return NULL;
    }
    return findAllGeneric<int>(mIndexList, translated.string());
}

bool TaskCase::translateVarName(const android::String8& orig, android::String8& translated)
{
    const char* src = orig.string();
    const int nmatch = 2;
    regmatch_t pmatch[nmatch];
    regex_t re;
    size_t strStart = 0;

    if (regcomp(&re, "[a-z0-9_]*[$]([a-z0-9]+)[_]*", REG_EXTENDED) != 0) {
        LOGE("regcomp failed");
        return false;
    }
    bool result = false;
    size_t matchStart = 0;
    size_t matchEnd = 0;
    while (regexec(&re, src, nmatch, pmatch, 0) == 0) {
        matchStart = strStart + pmatch[1].rm_so;
        matchEnd = strStart + pmatch[1].rm_eo;
        translated.append(StringUtil::substr(orig, strStart, pmatch[1].rm_so - 1)); //-1 for $
        android::String8 indexName;
        indexName.append(StringUtil::substr(orig, matchStart, matchEnd - matchStart));
        int val;
        if (!findIndex(indexName, val)) {
            LOGE("TaskCase::translateVarName no index with name %s", indexName.string());
            regfree(&re);
            return false;
        }
        translated.appendFormat("%d", val);
        LOGD("match found strStart %d, matchStart %d, matchEnd %d, converted str %s",
                strStart, matchStart, matchEnd, translated.string());
        src += pmatch[1].rm_eo;
        strStart += pmatch[1].rm_eo;
    }
    if (matchEnd < orig.length()) {
        //LOGD("%d %d", matchEnd, orig.length());
        translated.append(StringUtil::substr(orig, matchEnd, orig.length() - matchEnd));
    }
    LOGD("translated str %s to %s", orig.string(), translated.string());
    regfree(&re);
    return true;
}

android::sp<RemoteAudio>& TaskCase::getRemoteAudio()
{
    if (mClient == NULL) {
        mClient = new ClientImpl();
        ASSERT(mClient->init(Settings::Instance()->getSetting(Settings::EADB)));
    }
    return mClient->getAudio();
}

void TaskCase::releaseRemoteAudio()
{
    delete mClient;
    mClient = NULL;
}

void TaskCase::setDetails(android::String8 details)
{
    mDetails = details;
}

const android::String8& TaskCase::getDetails() const
{
    return mDetails;
}


TaskGeneric::ExecutionResult TaskCase::run()
{
    android::String8 name;
    android::String8 version;
    //LOGI("str %d, %d", strlen(STR_NAME), strlen(STR_VERSION));
    if (!findStringAttribute(STR_NAME, name) || !findStringAttribute(STR_VERSION, version)) {
        LOGW("TaskCase::run no name or version information");
    }
    MSG("== Test case %s version %s started ==", name.string(), version.string());
    std::list<TaskGeneric*>::iterator i = getChildren().begin();
    std::list<TaskGeneric*>::iterator end = getChildren().end();
    TaskGeneric* setup = *i;
    i++;
    TaskGeneric* action = *i;
    i++;
    TaskGeneric* save = (i == end)? NULL : *i;
    if (save == NULL) {
        LOGW("No save stage in test case");
    }
    bool testPassed = true;
    TaskGeneric::ExecutionResult result = setup->run();
    TaskGeneric::ExecutionResult resultAction(TaskGeneric::EResultOK);
    if (result != TaskGeneric::EResultOK) {
        MSG("== setup stage failed %d ==", result);
        testPassed = false;
    } else {
        resultAction = action->run();
        if (resultAction != TaskGeneric::EResultPass) {
            MSG("== action stage failed %d ==", resultAction);
            testPassed = false;
        }
        // save done even for failure if possible
        if (save != NULL) {
            result = save->run();
        }
        if (result != TaskGeneric::EResultOK) {
            MSG("== save stage failed %d ==", result);
            testPassed = false;
        }
    }
    if (testPassed) {
        result = TaskGeneric::EResultPass;
        MSG("== Case %s Passed ==", name.string());
        Report::Instance()->addCasePassed(this);
    } else {
        if (resultAction != TaskGeneric::EResultOK) {
            result = resultAction;
        }
        MSG("== Case %s Failed ==", name.string());
        Report::Instance()->addCaseFailed(this);
    }
    // release remote audio for other cases to use
    releaseRemoteAudio();
    return result;
}

