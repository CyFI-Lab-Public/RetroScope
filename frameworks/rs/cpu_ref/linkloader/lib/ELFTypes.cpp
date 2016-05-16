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

#include "ELFTypes.h"
#include <llvm/Support/raw_ostream.h>
#include <llvm/Support/Format.h>

namespace detail {

#define ELF_TYPE_PRINT_OPERATOR(TYPE, FORMAT_WIDTH)                         \
  llvm::raw_ostream &operator<<(llvm::raw_ostream &os, TYPE const &val) {   \
    os << llvm::format("%0*x", FORMAT_WIDTH, val.value);                    \
    return os;                                                              \
  }

  ELF_TYPE_PRINT_OPERATOR(ELF32Address, 8)
  ELF_TYPE_PRINT_OPERATOR(ELF32Offset,  8)
  ELF_TYPE_PRINT_OPERATOR(ELF64Address, 16)
  ELF_TYPE_PRINT_OPERATOR(ELF64Offset,  16)

#undef ELF_TYPE_PRINT_OPERATOR

} // end namespace detail
