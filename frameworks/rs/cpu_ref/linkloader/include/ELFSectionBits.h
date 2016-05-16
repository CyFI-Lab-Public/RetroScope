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

#ifndef ELF_SECTION_BITS_H
#define ELF_SECTION_BITS_H

#include "ELFTypes.h"
#include "ELFSection.h"
#include "MemChunk.h"

#include <llvm/ADT/OwningPtr.h>

template <unsigned Bitwidth>
class ELFSectionBits : public ELFSection<Bitwidth> {
protected:
  ELFSectionHeader<Bitwidth> const *sh;
  MemChunk chunk;

protected:
  ELFSectionBits() : sh(NULL) { }

public:
  virtual void print() const;

  bool protect();

  unsigned char const *getBuffer() const {
    return chunk.getBuffer();
  }

  unsigned char *getBuffer() {
    return chunk.getBuffer();
  }

  unsigned char &operator[](size_t index) {
    return chunk[index];
  }

  unsigned char const &operator[](size_t index) const {
    return chunk[index];
  }

  size_t size() const {
    return chunk.size();
  }

};

#include "impl/ELFSectionBits.hxx"

#endif // ELF_SECTION_BITS_H
