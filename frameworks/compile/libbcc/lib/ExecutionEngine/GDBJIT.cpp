/*
 * Copyright 2012, The Android Open Source Project
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

// This file contains portions derived from LLVM, with the original copyright
// header below:
//===-- GDBJIT.cpp - Common Implementation shared by GDB-JIT users --------===//
//
//                     The LLVM Compiler Infrastructure
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
//
// This file is used to support GDB's JIT interface
//
//===----------------------------------------------------------------------===//

#include <llvm/Support/Compiler.h>

// This interface must be kept in sync with gdb/gdb/jit.h .
extern "C" {

  // GDB 7.0+ puts a (silent) breakpoint in this function.
  LLVM_ATTRIBUTE_NOINLINE void __jit_debug_register_code() { }

}
