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

#ifndef FRAMEWORKS_EX_VARIABLESPEED_JNI_DECODE_BUFFER_H_
#define FRAMEWORKS_EX_VARIABLESPEED_JNI_DECODE_BUFFER_H_

#include <integral_types.h>
#include <macros.h>
#include <stdlib.h>
#include <vector>

// DecodeBuffer is used to store arrays of int16 values for audio.
//
// This class is not thread-safe.  You should provide your own
// synchronization if you wish to use it from multiple threads.
class DecodeBuffer {
 public:
  DecodeBuffer(size_t sizeOfOneBuffer, size_t maxSize);
  virtual ~DecodeBuffer();
  size_t GetSizeInBytes() const;
  void AddData(int8_t* pointer, size_t lengthInBytes);
  void Clear();
  void AdvanceHeadPointerShorts(size_t numberOfShorts);
  int16 GetAtIndex(size_t index);
  bool IsTooLarge() const;
  size_t GetTotalAdvancedCount() const;

 private:
  void PushValue(int16 value);

  size_t sizeOfOneBuffer_;
  size_t maxSize_;
  size_t start_;
  size_t end_;
  size_t advancedCount_;
  // This vector isn't ideal because we perform a number of queue-like
  // operations: namely removing from the front and appending at the back.
  // However we also need constant-time access to the elements of this
  // vector, and therefore it's not good enough to use a std::queue.
  // In practice this data structure choice doesn't seem to be a bottleneck.
  std::vector<int16*> data_;

  DISALLOW_COPY_AND_ASSIGN(DecodeBuffer);
};

#endif  // FRAMEWORKS_EX_VARIABLESPEED_JNI_DECODE_BUFFER_H_
