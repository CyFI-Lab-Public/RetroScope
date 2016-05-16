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
#include "task/TaskAll.h"

static const android::String8 AAA("aaa");
static const android::String8 BBB("bbb");

class TaskTest : public testing::Test {
public:
    TaskCase* mTestCase;
    // should not delete
    TaskGeneric* mTaskSetup;
    TaskGeneric* mTaskAction;
    TaskGeneric* mTaskSequential;
    TaskGeneric* mTaskProcess;
    TaskGeneric* mTaskInput;
    TaskGeneric* mTaskOutput;
    TaskGeneric* mTaskSound;

    class TestTaskDummy: public TaskGeneric {
    public:
        static int mRunCounter;
        static int mLiveInstanceCounter;

        TestTaskDummy(TaskGeneric::TaskType type)
            : TaskGeneric(type) {
            mLiveInstanceCounter++;


            const android::String8* list[] = {&AAA, &BBB, NULL};
            registerSupportedStringAttributes(list);
        };
        virtual ~TestTaskDummy(){
            mLiveInstanceCounter--;
        };

        virtual TaskGeneric::ExecutionResult run()
        {
            mRunCounter++;
            return TaskGeneric::run();
        };
        bool addStringAttributePublic(const android::String8& key, android::String8& value){
            return addStringAttribute(key, value);
        }
        bool findStringAttributePublic(const android::String8& key, android::String8& value){
            return findStringAttribute(key, value);
        }
    };

    virtual void SetUp() {
        TestTaskDummy::mRunCounter = 0;
        TestTaskDummy::mLiveInstanceCounter = 0;
        mTestCase = new TaskCase();
        mTaskSetup = new TestTaskDummy(TaskGeneric::ETaskSetup);
        mTaskAction = new TestTaskDummy(TaskGeneric::ETaskAction);
        ASSERT_TRUE(mTestCase->addChild(mTaskSetup));
        ASSERT_TRUE(mTestCase->addChild(mTaskAction));
        mTaskSequential = new TestTaskDummy(TaskGeneric::ETaskSequential);
        ASSERT_TRUE(mTaskAction->addChild(mTaskSequential));
        mTaskProcess = new TestTaskDummy(TaskGeneric::ETaskProcess);
        mTaskInput = new TestTaskDummy(TaskGeneric::ETaskInput);
        mTaskOutput = new TestTaskDummy(TaskGeneric::ETaskOutput);
        ASSERT_TRUE(mTaskSequential->addChild(mTaskOutput));
        ASSERT_TRUE(mTaskSequential->addChild(mTaskInput));
        ASSERT_TRUE(mTaskSequential->addChild(mTaskProcess));
        mTaskSound = new TestTaskDummy(TaskGeneric::ETaskSound);
        ASSERT_TRUE(mTaskSetup->addChild(mTaskSound));
        ASSERT_TRUE(TestTaskDummy::mLiveInstanceCounter == 7);
    }

    virtual void TearDown() {
        if(mTestCase != NULL) {
            delete mTestCase;
        }
        ASSERT_TRUE(TestTaskDummy::mLiveInstanceCounter == 0);
    }
};

int TaskTest::TestTaskDummy::mRunCounter = 0;
int TaskTest::TestTaskDummy::mLiveInstanceCounter = 0;

TEST_F(TaskTest, HierarchyTest) {
    // verify hierarchy
    ASSERT_TRUE(mTaskSetup->getTestCase() == mTestCase);
    ASSERT_TRUE(mTaskAction->getTestCase() == mTestCase);
    ASSERT_TRUE(mTaskSequential->getTestCase() == mTestCase);
    ASSERT_TRUE(mTaskProcess->getTestCase() == mTestCase);
    ASSERT_TRUE(mTaskInput->getTestCase() == mTestCase);
    ASSERT_TRUE(mTaskOutput->getTestCase() == mTestCase);
    ASSERT_TRUE(mTaskSound->getTestCase() == mTestCase);
}

TEST_F(TaskTest, RunTest) {
    ASSERT_TRUE(mTestCase->run() == TaskGeneric::EResultOK);
    ASSERT_TRUE(TestTaskDummy::mRunCounter == 7);
}

TEST_F(TaskTest, StringAttributeTest) {
    android::String8 aaaVal("aaa_val");
    android::String8 bbbVal("bbb_val");
    android::String8 read;
    TestTaskDummy* task = reinterpret_cast<TestTaskDummy*>(mTaskSetup);
    ASSERT_TRUE(task->addStringAttributePublic(AAA, aaaVal));
    ASSERT_TRUE(task->addStringAttributePublic(BBB, bbbVal));
    const android::String8 CCC("ccc");
    ASSERT_TRUE(!task->addStringAttributePublic(CCC, bbbVal));
    ASSERT_TRUE(task->findStringAttributePublic(AAA, read));
    ASSERT_TRUE(read == aaaVal);
    ASSERT_TRUE(task->findStringAttributePublic(BBB, read));
    ASSERT_TRUE(read == bbbVal);
    const android::String8 VERSION("version");
    const android::String8 NAME("name");
    ASSERT_TRUE(!task->findStringAttributePublic(VERSION, read));
    ASSERT_TRUE(!task->findStringAttributePublic(NAME, read));
}


