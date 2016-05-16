/*
 * Copyright 2011, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef TRAITS_H
#define TRAITS_H

#include <stddef.h>

template <typename Type>
struct TypeTraits {
private:
  struct AlignmentTest {
    char pending;
    Type element;
  };

public:
  enum { size = sizeof(Type) };
  enum { align = offsetof(AlignmentTest, element) };
};

#define TYPE_TRAITS_SPECIALIZE(TYPE, SIZE, ALIGN) \
template <> \
struct TypeTraits<TYPE> { \
  enum { size = SIZE }; \
  enum { align = ALIGN }; \
};

#endif // TRAITS_H
