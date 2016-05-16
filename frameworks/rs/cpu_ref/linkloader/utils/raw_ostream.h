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

#ifndef RAW_OSTREAM_H
#define RAW_OSTREAM_H

#include <llvm/Support/raw_ostream.h>
#include <llvm/Support/Format.h>

extern llvm::raw_ostream &out();
struct MyFormat {
  char *ptr;
};
extern MyFormat const fillformat(char const,        // Fill character.
                                 int const,         // Fill Width.
                                 char const * = "", // Format string.
                                 ...);              // Format variable.
extern llvm::raw_ostream &operator<<(llvm::raw_ostream &, MyFormat const &);

#endif // RAW_OSTREAM_H
