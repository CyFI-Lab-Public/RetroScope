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

#include "helper.h"
#include "raw_ostream.h"

#include <stdlib.h>
#include <ctype.h>

using namespace llvm;

void dump_hex(unsigned char const *data,
              size_t size, size_t begin, size_t end) {
  if (end <= begin) {
    // Nothing to print now.  Return directly.
    return;
  }

  size_t lower = begin & (~0xfUL);
  size_t upper = (end & (~0xfUL)) ? end : ((end + 16UL) & (~0xfUL));

  for (size_t i = lower; i < upper; i += 16) {
    out() << format("%08x", i) << ':';

    if (i < begin) {
      out().changeColor(raw_ostream::MAGENTA);
    }

    for (size_t j = i, k = i + 16; j < k; ++j) {
      if (j == begin) {
        out().resetColor();
      }

      if (j == end) {
        out().changeColor(raw_ostream::MAGENTA);
      }

      if (j < size) {
        out() << ' ' << format("%02x", (unsigned)data[j]);
      }
    }

    out().resetColor();
    out() << "  ";

    for (size_t j = i, k = i + 16; j < k; ++j) {
      if (data[j] > 0x20 && data[j] < 0x7f) {
        out() << (char)data[j];
      } else {
        out() << '.';
      }
    }

    out() << '\n';
  }
}
