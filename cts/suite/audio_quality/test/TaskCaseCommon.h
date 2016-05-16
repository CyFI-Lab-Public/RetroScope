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


#ifndef CTSAUDIO_TASKCASECOMMON_H
#define CTSAUDIO_TASKCASECOMMON_H

#include <gtest/gtest.h>

#include <Log.h>
#include <GenericFactory.h>
#include <task/TaskAll.h>

/**
 * Create TaskCase with setup and action as children
 * No need to destroy setup and action
 */
inline TaskCase* getTaskCase(TaskGeneric*& setup, TaskGeneric*& action)
{
    GenericFactory factory;
    TaskCase* taskCase = new TaskCase();
    setup = factory.createTask(TaskGeneric::ETaskSetup);
    taskCase->addChild(setup);
    action = factory.createTask(TaskGeneric::ETaskAction);
    taskCase->addChild(action);
    return taskCase;
}


#endif // CTSAUDIO_TASKCASECOMMON_H
