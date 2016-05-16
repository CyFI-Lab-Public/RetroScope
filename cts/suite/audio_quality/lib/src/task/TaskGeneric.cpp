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

#include "task/TaskAll.h"


TaskGeneric::TaskGeneric(TaskType type):
    mType(type),
    mParent(NULL)
{

}

bool deleteChildInstance(TaskGeneric* child, void* /*data*/)
{
    delete child;
    return true;
}

TaskGeneric::~TaskGeneric()
{
    forEachChild(deleteChildInstance, NULL);
    //mChildren.clear();
}

bool TaskGeneric::addChild(TaskGeneric* child)
{
    mChildren.push_back(child);
    child->setParent(this);
    return true;
}

bool TaskGeneric::forEachChild(bool (*runForEachChild)(TaskGeneric* child, void* data), void* data)
{
    std::list<TaskGeneric*>::iterator i = mChildren.begin();
    std::list<TaskGeneric*>::iterator end = mChildren.end();
    for (; i != end; i++) {
        if (!(*runForEachChild)(*i, data)) {
            return false;
        }
    }
    return true;
}

TaskGeneric* TaskGeneric::getParent()
{
    return mParent;
}

TaskCase* TaskGeneric::getTestCase()
{
    TaskGeneric* task = this;

    while (task != NULL) {
        if (task->getType() == ETaskCase) {
            // do not use dynamic_cast intentionally
            return reinterpret_cast<TaskCase*>(task);
        }
        task = task->getParent();
    }
    LOGE("TaskGeneric::getTestCase no TaskCase found!");
    return NULL;
}

void TaskGeneric::setParent(TaskGeneric* parent)
{
    LOGD("TaskGeneric::setParent self %x, parent %x", this, parent);
    mParent = parent;
}

bool runChild(TaskGeneric* child, void* data)
{
    TaskGeneric::ExecutionResult* result = reinterpret_cast<TaskGeneric::ExecutionResult*>(data);
    *result = child->run();
    if (*result != TaskGeneric::EResultOK) {
        LOGE("child type %d returned %d", child->getType(), *result);
        return false;
    }
    return true;
}

TaskGeneric::ExecutionResult TaskGeneric::run()
{
    ExecutionResult result = EResultOK;
    forEachChild(runChild, &result);
    return result;
}

bool TaskGeneric::parseAttribute(const android::String8& name, const android::String8& value)
{
    // default implementation only handles registered string attributes
    if (!addStringAttribute(name, value)) {
        LOGE("parseAttribute unknown attribute %s %s for type %d",
                name.string(), value.string(), getType());
        return false;
    }
    return true;
}


void TaskGeneric::registerSupportedStringAttributes(const android::String8* keys[])
{
    int i = 0;
    while (keys[i] != NULL) {
        mAllowedStringAttributes.insert(*keys[i]);
        i++;
    }
}

bool TaskGeneric::addStringAttribute(const android::String8& key, const android::String8& value)
{
    std::set<android::String8, android::String8>::iterator it = mAllowedStringAttributes.find(key);
    if (it == mAllowedStringAttributes.end()) {
        return false; // not allowed
    }
    mStringAttributes[key] = value;
    return true;
}

bool TaskGeneric::findStringAttribute(const android::String8& key, android::String8& value) const
{
    std::map<android::String8, android::String8>::const_iterator it = mStringAttributes.find(key);
    if (it == mStringAttributes.end()) {
        return false; // not found
    }
    value = it->second;
    return true;
}

