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

#ifndef ELF_OBJECT_H
#define ELF_OBJECT_H

#include "ELFTypes.h"
#include "MemChunk.h"

#include "utils/rsl_assert.h"

#include <llvm/ADT/OwningPtr.h>

#include <string>
#include <vector>

template <unsigned Bitwidth>
class ELFObject {
public:
  ELF_TYPE_INTRO_TO_TEMPLATE_SCOPE(Bitwidth);

private:
  llvm::OwningPtr<ELFHeaderTy> header;
  llvm::OwningPtr<ELFSectionHeaderTableTy> shtab;
  std::vector<ELFSectionTy *> stab;

  MemChunk SHNCommonData;
  unsigned char *SHNCommonDataPtr;
  size_t SHNCommonDataFreeSize;

  bool missingSymbols;

  // TODO: Need refactor!
  bool initSHNCommonDataSize(size_t SHNCommonDataSize) {
    rsl_assert(!SHNCommonDataPtr && "Can't init twice.");
    if (!SHNCommonData.allocate(SHNCommonDataSize)) {
      return false;
    }

    SHNCommonDataPtr = SHNCommonData.getBuffer();
    SHNCommonDataFreeSize = SHNCommonDataSize;
    return true;
  }

private:
  ELFObject() : SHNCommonDataPtr(NULL), missingSymbols(false) { }

public:
  template <typename Archiver>
  static ELFObject *read(Archiver &AR);

  ELFHeaderTy const *getHeader() const {
    return header.get();
  }

  ELFSectionHeaderTableTy const *getSectionHeaderTable() const {
    return shtab.get();
  }

  char const *getSectionName(size_t i) const;
  ELFSectionTy const *getSectionByIndex(size_t i) const;
  ELFSectionTy *getSectionByIndex(size_t i);
  ELFSectionTy const *getSectionByName(std::string const &str) const;
  ELFSectionTy *getSectionByName(std::string const &str);

  inline bool getMissingSymbols() const {
    return missingSymbols;
  }

  void *allocateSHNCommonData(size_t size, size_t align = 1) {
    rsl_assert(size > 0 && align != 0);

    rsl_assert(SHNCommonDataPtr && "Must init common data size before use!");

    // Ensure alignment
    size_t rem = ((uintptr_t)SHNCommonDataPtr) % align;
    if (rem != 0) {
      SHNCommonDataPtr += align - rem;
      SHNCommonDataFreeSize -= align - rem;
    }

    // Ensure the free size is sufficient
    if (SHNCommonDataFreeSize < size) {
      return NULL;
    }

    // Allcoate
    void *result = SHNCommonDataPtr;
    SHNCommonDataPtr += size;
    SHNCommonDataFreeSize -= size;

    return result;
  }

  void relocate(void *(*find_sym)(void *context, char const *name),
                void *context);

  void print() const;

  ~ELFObject() {
    for (size_t i = 0; i < stab.size(); ++i) {
      // Delete will check the pointer is nullptr or not by himself.
      delete stab[i];
    }
  }

private:
  void relocateARM(void *(*find_sym)(void *context, char const *name),
                   void *context,
                   ELFSectionRelTableTy *reltab,
                   ELFSectionProgBitsTy *text);

  void relocateX86_32(void *(*find_sym)(void *context, char const *name),
                      void *context,
                      ELFSectionRelTableTy *reltab,
                      ELFSectionProgBitsTy *text);

  void relocateX86_64(void *(*find_sym)(void *context, char const *name),
                      void *context,
                      ELFSectionRelTableTy *reltab,
                      ELFSectionProgBitsTy *text);

  void relocateMIPS(void *(*find_sym)(void *context, char const *name),
                    void *context,
                    ELFSectionRelTableTy *reltab,
                    ELFSectionProgBitsTy *text);
};

#include "impl/ELFObject.hxx"

#endif // ELF_OBJECT_H
