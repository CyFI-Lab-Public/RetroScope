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

#include <tinyxml.h>

#include <UniquePtr.h>

#include "Log.h"
#include "GenericFactory.h"
#include "task/ModelBuilder.h"

static const int MAX_NO_CHILDREN = 8;
static const ModelBuilder::ChildInfo CASE_TABLE[] = {
    { TaskGeneric::ETaskSetup, true },
    { TaskGeneric::ETaskAction, true },
    { TaskGeneric::ETaskSave, false }
};
static const ModelBuilder::ChildInfo SETUP_TABLE[] = {
    { TaskGeneric::ETaskSound, false },
    { TaskGeneric::ETaskProcess, false },
    { TaskGeneric::ETaskDownload, false }
};
static const ModelBuilder::ChildInfo ACTION_TABLE[] = {
    { TaskGeneric::ETaskSequential, true }
};
static const ModelBuilder::ChildInfo SEQUENTIAL_TABLE[] = {
    { TaskGeneric::ETaskSequential, false },
    { TaskGeneric::ETaskInput, false },
    { TaskGeneric::ETaskOutput, false },
    { TaskGeneric::ETaskProcess, false },
    { TaskGeneric::ETaskMessage, false }
};


ModelBuilder::ParsingInfo ModelBuilder::mParsingTable[ModelBuilder::PARSING_TABLE_SIZE] = {
    { "case", TaskGeneric::ETaskCase, CASE_TABLE,
            sizeof(CASE_TABLE)/sizeof(ModelBuilder::ChildInfo) },
    { "setup", TaskGeneric::ETaskSetup, SETUP_TABLE,
            sizeof(SETUP_TABLE)/sizeof(ModelBuilder::ChildInfo) },
    { "action", TaskGeneric::ETaskAction, ACTION_TABLE,
            sizeof(ACTION_TABLE)/sizeof(ModelBuilder::ChildInfo) },
    { "sequential", TaskGeneric::ETaskSequential, SEQUENTIAL_TABLE,
                sizeof(SEQUENTIAL_TABLE)/sizeof(ModelBuilder::ChildInfo) },
    { "process", TaskGeneric::ETaskProcess, NULL, 0 },
    { "input", TaskGeneric::ETaskInput, NULL, 0 },
    { "output", TaskGeneric::ETaskOutput, NULL, 0 },
    { "sound", TaskGeneric::ETaskSound, NULL, 0 },
    { "save", TaskGeneric::ETaskSave, NULL, 0 },
    { "message", TaskGeneric::ETaskMessage, NULL, 0 },
    { "download", TaskGeneric::ETaskDownload, NULL, 0 }
};


ModelBuilder::ModelBuilder()
    : mFactory(new GenericFactory())
{

}

ModelBuilder::ModelBuilder(GenericFactory* factory)
    : mFactory(factory)
{

}
ModelBuilder::~ModelBuilder()
{
    delete mFactory;
}

TaskGeneric* ModelBuilder::parseTestDescriptionXml(const android::String8& xmlFileName,
        bool caseOnly)
{
    TiXmlDocument doc(xmlFileName.string());
    if (!doc.LoadFile()) {
        LOGE("ModelBuilder::parseTestDescriptionXml cannot load file %s", xmlFileName.string());
        return NULL;
    }
    const TiXmlElement* root;
    if ((root = doc.FirstChildElement("case")) != NULL) {
        return parseCase(*root);
    } else if (!caseOnly && ((root = doc.FirstChildElement("batch")) != NULL)) {
        return parseBatch(*root, xmlFileName);
    } else {
        LOGE("ModelBuilder::parseTestDescriptionXml wrong root element");
        return NULL;
    }
}

TaskGeneric* ModelBuilder::parseGeneric(const TiXmlElement& self, int tableIndex)
{
    TaskGeneric::TaskType typeSelf(mParsingTable[tableIndex].type);
    int Nchildren = mParsingTable[tableIndex].Nchildren;
    UniquePtr<TaskGeneric> taskSelf(mFactory->createTask(typeSelf));
    if (taskSelf.get() == NULL) {
        return NULL;
    }
    if (!parseAttributes(self, *taskSelf.get())) {
        return NULL;
    }
    // copy mandatory flags, and will be cleared once the item is found
    bool mandatoryAbsence[MAX_NO_CHILDREN];
    const ModelBuilder::ChildInfo* childTable = mParsingTable[tableIndex].allowedChildren;
    for (int i = 0; i < Nchildren; i++) {
        mandatoryAbsence[i] = childTable[i].mandatory;
    }

    // handle children
    const TiXmlElement* child = self.FirstChildElement();
    while (child != NULL) {
        TaskGeneric::TaskType childType(TaskGeneric::ETaskInvalid);
        int i;
        // check if type is valid
        for (i = 0; i < PARSING_TABLE_SIZE; i++) {
            if (strcmp(child->Value(), mParsingTable[i].name) == 0) {
                break;
            }
        }
        if (i == PARSING_TABLE_SIZE) {
            LOGE("ModelBuilder::parseGeneric unknown element %s", child->Value());
            return NULL;
        }
        childType = mParsingTable[i].type;
        int j;
        // check if the type is allowed as child
        for (j = 0; j < Nchildren; j++) {
            if (childTable[j].type == childType) {
                if (childTable[j].mandatory) {
                    mandatoryAbsence[j] = false;
                }
                break;
            }
        }
        if (j == Nchildren) {
            LOGE("ModelBuilder::parseGeneric unsupported child type %d for type %d", childType,
                    typeSelf);
            return NULL;
        }
        UniquePtr<TaskGeneric> taskChild(parseGeneric(*child, i));
        if (taskChild.get() == NULL) {
            LOGE("ModelBuilder::parseGeneric failed in parsing child type %d for type %d",
                    childType, typeSelf);
            return NULL;
        }
        if (!taskSelf.get()->addChild(taskChild.get())) {
            LOGE("ModelBuilder::parseGeneric cannot add child type %d to type %d", childType,
                    typeSelf);
            return NULL;
        }
        TaskGeneric* donotuse = taskChild.release();

        child = child->NextSiblingElement();
    }
    for (int i = 0; i < Nchildren; i++) {
        if (mandatoryAbsence[i]) {
            LOGE("ModelBuilder::parseGeneric mandatory child type %d not present in type %d",
                    childTable[i].type, typeSelf);
            return NULL;
        }
    }

    return taskSelf.release();
}


TaskCase* ModelBuilder::parseCase(const TiXmlElement& root)
{
    // position 0 of mParsingTable should be "case"
    return reinterpret_cast<TaskCase*>(parseGeneric(root, 0));
}


TaskBatch* ModelBuilder::parseBatch(const TiXmlElement& root, const android::String8& xmlFileName)
{
    UniquePtr<TaskBatch> batch(
            reinterpret_cast<TaskBatch*>(mFactory->createTask(TaskGeneric::ETaskBatch)));
    if (batch.get() == NULL) {
        LOGE("ModelBuilder::handleBatch cannot create TaskBatch");
        return NULL;
    }
    if (!parseAttributes(root, *batch.get())) {
        return NULL;
    }

    const TiXmlElement* inc = root.FirstChildElement("include");
    if (inc == NULL) {
        LOGE("ModelBuilder::handleBatch no include inside batch");
        return NULL;
    }
    android::String8 path = xmlFileName.getPathDir();

    UniquePtr<TaskCase> testCase;
    int i = 0;
    while (1) {
        if (inc == NULL) {
            break;
        }
        if (strcmp(inc->Value(),"include") != 0) {
            LOGE("ModelBuilder::handleBatch invalid element %s", inc->Value());
        }
        testCase.reset(parseInclude(*inc, path));
        if (testCase.get() == NULL) {
            LOGE("ModelBuilder::handleBatch cannot create test case from include");
            return NULL;
        }
        if (!batch.get()->addChild(testCase.get())) {
            return NULL;
        }
        TaskGeneric* donotuse = testCase.release(); // parent will take care of destruction.
        inc = inc->NextSiblingElement();
        i++;
    }
    if (i == 0) {
        // at least one include should exist.
        LOGE("ModelBuilder::handleBatch no include elements");
        return NULL;
    }

    return batch.release();
}

TaskCase* ModelBuilder::parseInclude(const TiXmlElement& elem, const android::String8& path)
{
    const char* fileName = elem.Attribute("file");
    if (fileName == NULL) {
        LOGE("ModelBuilder::handleBatch no include elements");
        return NULL;
    }
    android::String8 incFile = path;
    incFile.appendPath(fileName);

    // again no dynamic_cast intentionally
    return reinterpret_cast<TaskCase*>(parseTestDescriptionXml(incFile, true));
}

bool ModelBuilder::parseAttributes(const TiXmlElement& elem, TaskGeneric& task)
{
    const TiXmlAttribute* attr = elem.FirstAttribute();
    while (1) {
        if (attr == NULL) {
            break;
        }
        android::String8 name(attr->Name());
        android::String8 value(attr->Value());
        if (!task.parseAttribute(name, value)) {
            LOGE("ModelBuilder::parseAttributes cannot parse attribute %s:%s for task type %d",
                    attr->Name(), attr->Value(), task.getType());
            return false;
        }
        attr = attr->Next();
    }
    return true;
}
