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


#include <gtest/gtest.h>
#include <task/ModelBuilder.h>


class ModelBuilderTest : public testing::Test {
public:
    ModelBuilder mModelBuilder;
};

TEST_F(ModelBuilderTest, ParsingCaseNoAttribTest) {
    android::String8 xmlFile("test_description/test/no_attrib.xml");
    TaskGeneric* testCase = mModelBuilder.parseTestDescriptionXml(xmlFile);
    ASSERT_TRUE(testCase != NULL);
    //TODO verify TestCase
    delete testCase;
}

TEST_F(ModelBuilderTest, ParsingCaseTest) {
    android::String8 xmlFile("test_description/host_speaker_calibration.xml");
    TaskGeneric* testCase = mModelBuilder.parseTestDescriptionXml(xmlFile);
    ASSERT_TRUE(testCase != NULL);
    //TODO verify TestCase
    delete testCase;
}

TEST_F(ModelBuilderTest, ParsingBatchTest) {
    android::String8 xmlFile("test_description/all_playback.xml");
    TaskGeneric* testBatch = mModelBuilder.parseTestDescriptionXml(xmlFile);
    ASSERT_TRUE(testBatch != NULL);
    //TODO verify TestCase
    delete testBatch;
}

TEST_F(ModelBuilderTest, CaseOnlyTest) {
    android::String8 xmlFile("test_description/all_playback.xml");
    TaskGeneric* task = mModelBuilder.parseTestDescriptionXml(xmlFile, true);
    ASSERT_TRUE(task == NULL);

    delete task;
}

TEST_F(ModelBuilderTest, MissingMandatoryTest) {
    android::String8 xmlFile("test_description/test/missing_mandatory.xml");
    TaskGeneric* task = mModelBuilder.parseTestDescriptionXml(xmlFile);
    ASSERT_TRUE(task == NULL);
    delete task;
}

TEST_F(ModelBuilderTest, UnknownElementTest) {
    android::String8 xmlFile("test_description/test/unknown_element.xml");
    TaskGeneric* task = mModelBuilder.parseTestDescriptionXml(xmlFile);
    ASSERT_TRUE(task == NULL);
    delete task;
}

TEST_F(ModelBuilderTest, WrongAttributeTest) {
    android::String8 xmlFile("test_description/test/wrong_attrib.xml");
    TaskGeneric* task = mModelBuilder.parseTestDescriptionXml(xmlFile);
    ASSERT_TRUE(task == NULL);
    delete task;
}

TEST_F(ModelBuilderTest, BuiltinRMSTest) {
    android::String8 xmlFile("test_description/test/test_rms_vma.xml");
    TaskGeneric* task = mModelBuilder.parseTestDescriptionXml(xmlFile);
    ASSERT_TRUE(task != NULL);
    TaskGeneric::ExecutionResult result = task->run();
    ASSERT_TRUE((result == TaskGeneric::EResultOK) || (result == TaskGeneric::EResultPass));
    delete task;
}

