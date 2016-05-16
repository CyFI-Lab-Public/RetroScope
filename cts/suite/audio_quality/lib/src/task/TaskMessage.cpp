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

#include "task/TaskMessage.h"


TaskMessage::TaskMessage()
    : TaskGeneric(TaskGeneric::ETaskMessage)
{}
TaskMessage::~TaskMessage()
{

}
TaskGeneric::ExecutionResult TaskMessage::run()
{
    //TODO
    return TaskGeneric::EResultError;
}
bool TaskMessage::parseAttribute(const android::String8& name, const android::String8& value)
{
    //TODO
    return false;
}
