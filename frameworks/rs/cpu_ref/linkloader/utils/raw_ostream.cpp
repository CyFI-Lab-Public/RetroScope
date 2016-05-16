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

#include "raw_ostream.h"

#include <llvm/Support/raw_ostream.h>
#include <llvm/Support/Format.h>
#include <cstdarg>
#include <cstring>

llvm::raw_ostream &out() {
  static llvm::raw_ostream &singleton = llvm::outs();
  return singleton;
}

MyFormat const fillformat(char const fill_char, // Fill character.
                          int const length,     // Fill Width.
                          char const *format_s, // Format string.
                          ...) {                // Format variable.
  using namespace std;
  struct MyFormat t_format;
  va_list valist;
  va_start(valist, format_s);
  t_format.ptr = new char[length+1];
  t_format.ptr[length] = '\0';
  vsnprintf(t_format.ptr, length, format_s, valist);
  int real_len = strlen(t_format.ptr);
  int fill_len = length;
  memmove(t_format.ptr + fill_len, t_format.ptr, real_len);
  memset(t_format.ptr, fill_char, fill_len);
  return t_format;
}

llvm::raw_ostream &operator<<(llvm::raw_ostream &os, MyFormat const &mf) {
  os << mf.ptr;
  delete mf.ptr;
  return os;
}
