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
//===-- GDBJITRegistrar.h - Common Implementation shared by GDB-JIT users --===//
//
//                     The LLVM Compiler Infrastructure
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
//
// This file contains declarations of the interface an ExecutionEngine would use
// to register an in-memory object file with GDB.
//
//===----------------------------------------------------------------------===//

#ifndef BCC_EXECUTION_ENGINE_GDB_JIT_REGISTRAR_H
#define BCC_EXECUTION_ENGINE_GDB_JIT_REGISTRAR_H

#include <cstddef>

// Buffer for an in-memory object file in executable memory
typedef char ObjectBuffer;

void registerObjectWithGDB(const ObjectBuffer* Object, std::size_t Size);
void deregisterObjectWithGDB(const ObjectBuffer* Object);

#endif // BCC_EXECUTION_ENGINE_GDB_JIT_REGISTRAR_H
