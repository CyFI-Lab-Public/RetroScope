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


#ifndef CTSAUDIO_MODELBUILDER_H
#define CTSAUDIO_MODELBUILDER_H

#include <utils/String8.h>
#include "TaskAll.h"

class TiXmlElement;


class GenericFactory;

/**
 * Class to parse Test description XML and generate test model with TestCase in top
 */

class ModelBuilder {
public:
    ModelBuilder();
    ModelBuilder(GenericFactory* factory);
    virtual ~ModelBuilder();

    /**
     * parse given xml with test case or batch. When caseOnly is true, only test case can be in.
     */
    virtual TaskGeneric* parseTestDescriptionXml(const android::String8& xmlFileName,
            bool caseOnly = false);

    struct ChildInfo {
        TaskGeneric::TaskType type;
        bool mandatory; // whether the child is mandatory or not
    };

private:
    virtual bool parseAttributes(const TiXmlElement& elem, TaskGeneric& task);
    virtual TaskGeneric* parseGeneric(const TiXmlElement& elem, int tableIndex);
    virtual TaskCase* parseCase(const TiXmlElement& root);
    virtual TaskBatch* parseBatch(const TiXmlElement& root, const android::String8& xmlFileName);
    virtual TaskCase* parseInclude(const TiXmlElement& elem, const android::String8& path);

    struct ParsingInfo {
        const char* name; // XML element name
        TaskGeneric::TaskType type;
        const ChildInfo* allowedChildren;
        int Nchildren;
    };
    // no table for batch, and ETaskInvalidLast is not in either (-2)
    static const int PARSING_TABLE_SIZE = TaskGeneric::ETaskInvalidLast - 2;
    static ParsingInfo mParsingTable[PARSING_TABLE_SIZE];

    GenericFactory* mFactory;

};


#endif // CTSAUDIO_MODELBUILDER_H
