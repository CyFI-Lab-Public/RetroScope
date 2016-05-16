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

#ifndef BCC_EXECUTION_ENGINE_OBJECT_LOADER_H
#define BCC_EXECUTION_ENGINE_OBJECT_LOADER_H

#include <cstddef>

#include "bcc/Support/Log.h"

#include <utils/Vector.h>

namespace bcc {

class FileBase;
class ObjectLoaderImpl;
class SymbolResolverInterface;

class ObjectLoader {
public:
  enum SymbolType {
    // TODO: More types.
    kFunctionType,
    kUnknownType,
  };

private:
  ObjectLoaderImpl *mImpl;

  void *mDebugImage;

  ObjectLoader() : mImpl(NULL), mDebugImage(0) { }

public:
  // Load from a in-memory object. pName is a descriptive name of this memory.
  static ObjectLoader *Load(void *pMemStart, size_t pMemSize, const char *pName,
                            SymbolResolverInterface &pResolver,
                            bool pEnableGDBDebug);

  // Load from a file.
  static ObjectLoader *Load(FileBase &pFile,
                            SymbolResolverInterface &pResolver,
                            bool pEnableGDBDebug);

  void *getSymbolAddress(const char *pName) const;

  size_t getSymbolSize(const char *pName) const;

  // Get the symbol name where the symbol is of the type pType. If kUnknownType
  // is given, it returns all symbols' names in the object.
  bool getSymbolNameList(android::Vector<const char *>& pNameList,
                         SymbolType pType = kUnknownType) const;

  ~ObjectLoader();
};

} // namespace bcc

#endif // BCC_EXECUTION_ENGINE_OBJECT_LOADER_H
