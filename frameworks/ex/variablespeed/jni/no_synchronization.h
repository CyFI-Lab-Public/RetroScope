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

#ifndef FRAMEWORKS_EX_VARIABLESPEED_JNI_NO_SYNCHRONIZATION_H_
#define FRAMEWORKS_EX_VARIABLESPEED_JNI_NO_SYNCHRONIZATION_H_

#include <macros.h>

// We don't need any synchronization at the moment.
// The sola_time_scaler (which is the code that uses this mutex class) is
// currently being used in a single-threaded manner, driven from the main
// PlayFromThisSource method in variablespeed.
// As such no locking is actually required, and so this class contains a
// fake mutex that does nothing.

class Mutex {
 public:
  Mutex() {}
  virtual ~Mutex() {}
  void Lock() {}
  void Unlock() {}

 private:
  DISALLOW_COPY_AND_ASSIGN(Mutex);
};

class MutexLock {
 public:
  explicit MutexLock(Mutex* mu) : mu_(mu) {}
  virtual ~MutexLock() {}

 private:
  Mutex* const mu_;
  DISALLOW_COPY_AND_ASSIGN(MutexLock);
};

#endif  // FRAMEWORKS_EX_VARIABLESPEED_JNI_NO_SYNCHRONIZATION_H_
