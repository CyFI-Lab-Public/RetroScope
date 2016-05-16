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
//==----- GDBJITRegistrar.cpp - Notify GDB about in-memory object files  ---==//
//
//                     The LLVM Compiler Infrastructure
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
//
// This file defines the GDBJITRegistrar object which is used by JIT engines to
// register in-memory object files with GDB for debugging.
//
//===----------------------------------------------------------------------===//

#include "bcc/ExecutionEngine/GDBJITRegistrar.h"

#include <llvm/ADT/DenseMap.h>
#include <llvm/Support/ErrorHandling.h>
#include <llvm/Support/Memory.h>
#include <llvm/Support/Mutex.h>
#include <llvm/Support/MutexGuard.h>

#include "bcc/ExecutionEngine/GDBJIT.h"

#include <fstream>

#ifdef ANDROID_ENGINEERING_BUILD
// Path to write dump output.
// It is expected that a debugger (plugin) sets this
// string to a writeable directory where files (such as JITted object files,
// IR dumps) are to be written. If this variable is 0, no debug dumps
// are generated.
char* gDebugDumpDirectory = 0;
#endif // ANDROID_ENGINEERING_BUILD

//************************************************************************
// COPIED/ADAPTED FROM llvm/lib/ExecutionEngine/JIT/JITDebugRegisterer.cpp
//************************************************************************
// This must be kept in sync with gdb/gdb/jit.h .
extern "C" {

  // We put information about the JITed function in this global, which the
  // debugger reads.  Make sure to specify the version statically, because the
  // debugger checks the version before we can set it during runtime.
  static struct jit_descriptor __jit_debug_descriptor = { 1, 0, 0, 0 };

}
//****************************************************************************
// END COPIED/ADAPTED FROM llvm/lib/ExecutionEngine/JIT/JITDebugRegisterer.cpp
//****************************************************************************

namespace {

// Buffer for an in-memory object file in executable memory
typedef llvm::DenseMap< const ObjectBuffer*, std::pair<std::size_t, jit_code_entry*> >
  RegisteredObjectBufferMap;

/// Global access point for the GDB JIT interface designed for use with a
/// singleton toolbox. Handles thread-safe registration and deregistration of
/// object files that are in executable memory managed by the client of this
/// class.
class GDBJITRegistrar {
  /// A map of in-memory object files that have been registered with the GDB JIT interface.
  RegisteredObjectBufferMap ObjectBufferMap;

public:
  /// Instantiates the GDB JIT service.
  GDBJITRegistrar() : ObjectBufferMap() {}

  /// Unregisters each object that was previously registered with GDB, and
  /// releases all internal resources.
  ~GDBJITRegistrar();

  /// Creates an entry in the GDB JIT registry for the buffer @p Object,
  /// which must contain an object file in executable memory with any
  /// debug information for GDB.
  void registerObject(const ObjectBuffer* Object, std::size_t Size);

  /// Removes the internal registration of @p Object, and
  /// frees associated resources.
  /// Returns true if @p Object was found in ObjectBufferMap.
  bool deregisterObject(const ObjectBuffer* Object);

private:
  /// Deregister the debug info for the given object file from the debugger
  /// and delete any temporary copies.  This private method does not remove
  /// the function from Map so that it can be called while iterating over Map.
  void deregisterObjectInternal(RegisteredObjectBufferMap::iterator I);
};

/// Lock used to serialize all gdb-jit registration events, since they
/// modify global variables.
llvm::sys::Mutex JITDebugLock;

/// Acquire the lock and do the registration.
void NotifyGDB(jit_code_entry* JITCodeEntry) {
  llvm::MutexGuard locked(JITDebugLock);
  __jit_debug_descriptor.action_flag = JIT_REGISTER_FN;

  // Insert this entry at the head of the list.
  JITCodeEntry->prev_entry = NULL;
  jit_code_entry* NextEntry = __jit_debug_descriptor.first_entry;
  JITCodeEntry->next_entry = NextEntry;
  if (NextEntry != NULL) {
    NextEntry->prev_entry = JITCodeEntry;
  }
  __jit_debug_descriptor.first_entry = JITCodeEntry;
  __jit_debug_descriptor.relevant_entry = JITCodeEntry;
  __jit_debug_register_code();
}

GDBJITRegistrar* RegistrarSingleton() {
  static GDBJITRegistrar* sRegistrar = NULL;
  if (sRegistrar == NULL) {
    // The mutex is here so that it won't slow down access once the registrar
    //   is instantiated
    llvm::MutexGuard locked(JITDebugLock);
    // Check again to be sure another thread didn't create this while we waited
    if (sRegistrar == NULL) {
      sRegistrar = new GDBJITRegistrar;
    }
  }
  return sRegistrar;
}

GDBJITRegistrar::~GDBJITRegistrar() {
  // Free all registered object files.
 for (RegisteredObjectBufferMap::iterator I = ObjectBufferMap.begin(), E = ObjectBufferMap.end();
       I != E; ++I) {
    // Call the private method that doesn't update the map so our iterator
    // doesn't break.
    deregisterObjectInternal(I);
  }
  ObjectBufferMap.clear();
}

void GDBJITRegistrar::registerObject(const ObjectBuffer* Object, std::size_t Size) {

  assert(Object && "Attempt to register a null object with a debugger.");
  assert(ObjectBufferMap.find(Object) == ObjectBufferMap.end()
    && "Second attempt to perform debug registration.");

  jit_code_entry* JITCodeEntry = new jit_code_entry();

  if (JITCodeEntry == 0) {
    llvm::report_fatal_error("Allocation failed when registering a GDB-JIT entry!\n");
  }
  else {
    JITCodeEntry->symfile_addr = Object;
    JITCodeEntry->symfile_size = Size;

    ObjectBufferMap[Object] = std::make_pair(Size, JITCodeEntry);
    NotifyGDB(JITCodeEntry);

#ifdef ANDROID_ENGINEERING_BUILD
    if (0 != gDebugDumpDirectory) {
      std::string Filename(gDebugDumpDirectory);
      Filename += "/jit_registered.o";

      std::ofstream outfile(Filename.c_str(), std::ofstream::binary);
      outfile.write((char*)JITCodeEntry->symfile_addr, JITCodeEntry->symfile_size);
      outfile.close();
    }
#endif
  }
}

bool GDBJITRegistrar::deregisterObject(const ObjectBuffer *Object) {
  RegisteredObjectBufferMap::iterator I = ObjectBufferMap.find(Object);

  if (I != ObjectBufferMap.end()) {
    deregisterObjectInternal(I);
    ObjectBufferMap.erase(I);
    return true;
  }
  return false;
}

void GDBJITRegistrar::deregisterObjectInternal(
    RegisteredObjectBufferMap::iterator I) {

  jit_code_entry*& JITCodeEntry = I->second.second;

  // Acquire the lock and do the unregistration.
  {
    llvm::MutexGuard locked(JITDebugLock);
    __jit_debug_descriptor.action_flag = JIT_UNREGISTER_FN;

    // Remove the jit_code_entry from the linked list.
    jit_code_entry* PrevEntry = JITCodeEntry->prev_entry;
    jit_code_entry* NextEntry = JITCodeEntry->next_entry;

    if (NextEntry) {
      NextEntry->prev_entry = PrevEntry;
    }
    if (PrevEntry) {
      PrevEntry->next_entry = NextEntry;
    }
    else {
      assert(__jit_debug_descriptor.first_entry == JITCodeEntry);
      __jit_debug_descriptor.first_entry = NextEntry;
    }

    // Tell GDB which entry we removed, and unregister the code.
    __jit_debug_descriptor.relevant_entry = JITCodeEntry;
    __jit_debug_register_code();
  }

  delete JITCodeEntry;
  JITCodeEntry = NULL;
}

} // end namespace

void registerObjectWithGDB(const ObjectBuffer* Object, std::size_t Size) {
  GDBJITRegistrar* Registrar = RegistrarSingleton();
  if (Registrar) {
    Registrar->registerObject(Object, Size);
  }
}

void deregisterObjectWithGDB(const ObjectBuffer* Object) {
  GDBJITRegistrar* Registrar = RegistrarSingleton();
  if (Registrar) {
    Registrar->deregisterObject(Object);
  }
}
