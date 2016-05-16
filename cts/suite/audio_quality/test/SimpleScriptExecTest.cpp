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
#include <SimpleScriptExec.h>


class ScriptExecTest : public testing::Test {

};

TEST_F(ScriptExecTest, PythonVersionTest) {
    ASSERT_TRUE(SimpleScriptExec::checkPythonEnv());
}


TEST_F(ScriptExecTest, checkIfPassedTest) {
    android::String8 pass1("___CTS_AUDIO_PASS___");
    android::String8 match1;
    ASSERT_TRUE(SimpleScriptExec::checkIfPassed(pass1, match1));

    android::String8 fail1;
    ASSERT_TRUE(!SimpleScriptExec::checkIfPassed(fail1, match1));
}

