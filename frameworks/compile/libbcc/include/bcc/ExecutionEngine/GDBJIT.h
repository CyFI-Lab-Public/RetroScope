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
//===-------------- GDBJIT.h - Register debug symbols for JIT -------------===//
//
//                     The LLVM Compiler Infrastructure
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
//
// This file defines the data structures used by JIT engines to register object
// files (ideally containing debug info) with GDB.
//
//===----------------------------------------------------------------------===//

#ifndef BCC_EXECUTION_ENGINE_GDB_JIT_H
#define BCC_EXECUTION_ENGINE_GDB_JIT_H

#include <llvm/Support/DataTypes.h>
#include <llvm/Support/Compiler.h>

// This must be kept in sync with gdb/gdb/jit.h .
extern "C" {

  typedef enum {
    JIT_NOACTION = 0,
    JIT_REGISTER_FN,
    JIT_UNREGISTER_FN
  } jit_actions_t;

  struct jit_code_entry {
    struct jit_code_entry *next_entry;
    struct jit_code_entry *prev_entry;
    const char *symfile_addr;
    uint64_t symfile_size;
  };

  struct jit_descriptor {
    uint32_t version;
    // This should be jit_actions_t, but we want to be specific about the
    // bit-width.
    uint32_t action_flag;
    struct jit_code_entry *relevant_entry;
    struct jit_code_entry *first_entry;
  };

  // GDB 7.0+ puts a (silent) breakpoint in this function.
  LLVM_ATTRIBUTE_NOINLINE void __jit_debug_register_code();

}

#endif // BCC_EXECUTION_ENGINE_GDB_JIT_H
