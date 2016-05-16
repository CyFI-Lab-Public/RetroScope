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

#include "bcc/ExecutionEngine/SymbolResolvers.h"

#if !defined(_WIN32)  /* TODO create a HAVE_DLFCN_H */
#include <dlfcn.h>
#else
/* TODO hack: definitions from bionic/libc/include/dlfcn.h */
void* dlopen(const char*  filename, int flag) {
  return NULL;
}

int dlclose(void*  handle) {
  return -1;
}

const char* dlerror(void) {
  return "Unspecified error!";
}

void* dlsym(void*  handle, const char*  symbol) {
  return NULL;
}

#define RTLD_NOW    0
#define RTLD_LAZY   1
#define RTLD_LOCAL  0
#define RTLD_GLOBAL 2
#define RTLD_DEFAULT  ((void*) 0xffffffff)
#define RTLD_NEXT     ((void*) 0xfffffffe)
#endif

#include <cassert>
#include <cstdio>
#include <new>

using namespace bcc;

//===----------------------------------------------------------------------===//
// DyldSymbolResolver
//===----------------------------------------------------------------------===//
DyldSymbolResolver::DyldSymbolResolver(const char *pFileName,
                                       bool pLazyBinding) : mError(NULL) {
  int flags = (pLazyBinding) ? RTLD_LAZY : RTLD_NOW;

  // Make the symbol within the given library to be local such that it won't
  // be available for symbol resolution of subsequently loaded libraries.
  flags |= RTLD_LOCAL;

  mHandle = ::dlopen(pFileName, flags);
  if (mHandle == NULL) {
    const char *err = ::dlerror();

#define DYLD_ERROR_MSG_PATTERN  "Failed to load %s! (%s)"
    size_t error_length = ::strlen(DYLD_ERROR_MSG_PATTERN) +
                          ::strlen(pFileName) + 1;
    if (err != NULL) {
      error_length += ::strlen(err);
    }

    mError = new (std::nothrow) char [error_length];
    if (mError != NULL) {
      ::snprintf(mError, error_length, DYLD_ERROR_MSG_PATTERN, pFileName,
                 ((err != NULL) ? err : ""));
    }
  }
#undef DYLD_ERROR_MSG_PATTERN
}

void *DyldSymbolResolver::getAddress(const char *pName) {
  assert((mHandle != NULL) && "Invalid DyldSymbolResolver!");
  return ::dlsym(mHandle, pName);
}

DyldSymbolResolver::~DyldSymbolResolver() {
  if (mHandle != NULL) {
    ::dlclose(mHandle);
    mHandle = NULL;
  }
  delete [] mError;
}
