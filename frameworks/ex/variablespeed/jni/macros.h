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

#ifndef FRAMEWORKS_EX_VARIABLESPEED_JNI_MACROS_H_
#define FRAMEWORKS_EX_VARIABLESPEED_JNI_MACROS_H_

#include <hlogging.h>

inline float min(float a, float b) {
  return (a < b) ? a : b;
}

inline float max(float a, float b) {
  return (a > b) ? a : b;
}

template <class ForwardIterator>
    ForwardIterator min_element(ForwardIterator first, ForwardIterator last) {
  ForwardIterator lowest = first;
  if (first == last) return last;
  while (++first != last)
    if (*first < *lowest)
      lowest = first;
  return lowest;
}

// A macro to disallow the copy constructor and operator= functions
// This should be used in the private: declarations for a class
#define DISALLOW_COPY_AND_ASSIGN(TypeName) \
  TypeName(const TypeName&);               \
  void operator=(const TypeName&)

#define CHECK(x) { \
  if (!(x)) { \
    LOGE("assertion failed: " #x); \
    LOGE("file: %s line: %d", __FILE__, __LINE__); \
    int* frob = NULL; \
    *frob = 5; \
  } \
}

template <class Dest, class Source>
inline Dest bit_cast(const Source& source) {
  // Compile time assertion: sizeof(Dest) == sizeof(Source)
  // A compile error here means your Dest and Source have different sizes.
  typedef char VerifySizesAreEqual [sizeof(Dest) == sizeof(Source) ? 1 : -1];  // NOLINT

  Dest dest;
  memcpy(&dest, &source, sizeof(dest));
  return dest;
}

#endif  // FRAMEWORKS_EX_VARIABLESPEED_JNI_MACROS_H_
