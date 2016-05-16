/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef FRAMEWORKS_EX_VARIABLESPEED_JNI_PROFILE_TIMER_H_
#define FRAMEWORKS_EX_VARIABLESPEED_JNI_PROFILE_TIMER_H_

#include <hlogging.h>
#include <time.h>

#include <string>

// Simple profiler for debugging method call duration.
class Timer {
 public:
  Timer() : startTime_(clock()) {
  }

  virtual ~Timer() {
    PrintElapsed("destructor");
  }

  void PrintElapsed(const char* message) {
    clock_t endTime(clock());
    LOGD("Timer(%s): %d ms", message,
        static_cast<int>((endTime - startTime_) * 1000 / CLOCKS_PER_SEC));
  }

  size_t GetElapsed() {
    clock_t endTime(clock());
    return (endTime - startTime_) * 1000 / CLOCKS_PER_SEC;
  }

 private:
  clock_t startTime_;

  DISALLOW_COPY_AND_ASSIGN(Timer);
};

#endif  // FRAMEWORKS_EX_VARIABLESPEED_JNI_PROFILE_TIMER_H_
