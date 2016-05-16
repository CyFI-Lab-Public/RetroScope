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

#ifndef BCC_EXECUTION_ENGINE_ELF_OBJECT_LOADER_IMPL_H
#define BCC_EXECUTION_ENGINE_ELF_OBJECT_LOADER_IMPL_H

#include "ObjectLoaderImpl.h"

// ELFObject and ELFSectionSymTab comes from librsloader. They're both
// defined under global scope without a namespace enclosed.
template <unsigned Bitwidth>
class ELFObject;

template <unsigned Bitwidth>
class ELFSectionSymTab;

namespace bcc {

class ELFObjectLoaderImpl : public ObjectLoaderImpl {
private:
  ELFObject<32> *mObject;
  ELFSectionSymTab<32> *mSymTab;

public:
  ELFObjectLoaderImpl() : ObjectLoaderImpl(), mObject(NULL), mSymTab(NULL) { }

  virtual bool load(const void *pMem, size_t pMemSize);

  virtual bool relocate(SymbolResolverInterface &pResolver);

  virtual bool prepareDebugImage(void *pDebugImg, size_t pDebugImgSize);

  virtual void *getSymbolAddress(const char *pName) const;

  virtual size_t getSymbolSize(const char *pName) const;

  virtual bool getSymbolNameList(android::Vector<const char *>& pNameList,
                                 ObjectLoader::SymbolType pType) const;
  ~ELFObjectLoaderImpl();
};

} // end namespace bcc

#endif // BCC_EXECUTION_ENGINE_ELF_OBJECT_LOADER_IMPL_H
