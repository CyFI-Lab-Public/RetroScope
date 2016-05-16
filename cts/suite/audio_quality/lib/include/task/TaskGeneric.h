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


#ifndef CTSAUDIO_TASKGENERIC_H
#define CTSAUDIO_TASKGENERIC_H
#include <string.h>
#include <stdio.h>
#include <list>
#include <map>
#include <set>
#include <utils/String8.h>


class TaskCase;

class TaskGeneric {
public:
    // internal model for xml tags
    enum TaskType {
        ETaskInvalid        = 0,
        ETaskBatch          = 1,
        ETaskCase           = 2,
        ETaskSetup          = 3,
        ETaskAction         = 4,
        ETaskSequential     = 5,
        ETaskProcess        = 6,
        ETaskInput          = 7,
        ETaskOutput         = 8,
        ETaskSound          = 9,
        ETaskSave           = 10,
        ETaskMessage        = 11,
        ETaskDownload       = 12,
        ETaskInvalidLast    = 13,
        //no ETaskInclude include does not involve any action.
    };

    TaskGeneric(TaskType type);

    virtual ~TaskGeneric();

    inline TaskType getType() {
        return mType;
    }

    enum ExecutionResult {
        EResultOK           = 0,
        // continue in the current loop. will result in skipping all subsequent steps
        EResultContinue     = 1,
        // get out of the current loop
        EResultBreakOneLoop = 2,
        // error which cannot be continued. effect is the same as fail. stops everything.
        EResultError        = 3,
        // test failed. stops everything.
        EResultFail         = 4,
        // test passed.
        EResultPass         = 5
    };

    /**
     * default implementation for adding child action
     * Ownership of the child is passed to this instance, and child will be destroyed in parent's
     * destructor.
     * @return false on error
     */
    virtual bool addChild(TaskGeneric* child);

    virtual ExecutionResult run();

    /// can be NULL if parent does not exist
    TaskGeneric* getParent();
    /// can be NULL if TestCase does not exist (unit testing?)
    TaskCase* getTestCase();

    void setParent(TaskGeneric* parent);

    /**
     * parse attribute from XML DOM. name/value pair will be passed for all attributes.
     */
    virtual bool parseAttribute(const android::String8& name, const android::String8& value);

    bool forEachChild(bool (*runForEachChild)(TaskGeneric* child, void* data), void* data);

protected:
    /// used by child instance to register allowed attributes
    /// keys array should end with NULL
    void registerSupportedStringAttributes(const android::String8* keys[]);
    bool addStringAttribute(const android::String8& key, const android::String8& value);
    bool findStringAttribute(const android::String8& key, android::String8& value) const;
    inline std::list<TaskGeneric*>& getChildren() {
        return mChildren;
    };

private:
    TaskType mType;
    TaskGeneric* mParent;
    std::list<TaskGeneric*> mChildren;

    std::set<android::String8> mAllowedStringAttributes;
    std::map<android::String8, android::String8> mStringAttributes;

};


#endif // CTSAUDIO_TASKGENERIC_H
