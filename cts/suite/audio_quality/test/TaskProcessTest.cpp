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


#include "TaskCaseCommon.h"

class TaskProcessTest : public testing::Test {
public:
    TaskCase* mTestCase;
    TaskSequential* mSequential;
    TaskProcess* mProcess;


    virtual void SetUp() {
        TaskGeneric* setup = NULL;
        TaskGeneric* action = NULL;
        mTestCase  = getTaskCase(setup, action);
        ASSERT_TRUE(mTestCase != NULL);
        ASSERT_TRUE(setup != NULL);
        ASSERT_TRUE(action != NULL);
        mSequential = new TaskSequential();
        const android::String8 REPEAT("repeat");
        const android::String8 N_10("10");
        const android::String8 INDEX("index");
        const android::String8 I("i");
        ASSERT_TRUE(mSequential->parseAttribute(REPEAT, N_10));
        ASSERT_TRUE(mSequential->parseAttribute(INDEX, I));
        ASSERT_TRUE(action->addChild(mSequential));
        mProcess = new TaskProcess();
        ASSERT_TRUE(mSequential->addChild(mProcess));

    }

    virtual void TearDown() {
        delete mTestCase;
    }
};


TEST_F(TaskProcessTest, AttributeTest) {

}


