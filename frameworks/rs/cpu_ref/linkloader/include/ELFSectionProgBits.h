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

#ifndef ELF_SECTION_PROGBITS_H
#define ELF_SECTION_PROGBITS_H

#include "ELFTypes.h"
#include "ELFSectionBits.h"
#include "ELFSectionHeader.h"
#include "MemChunk.h"
#include "StubLayout.h"

template <unsigned Bitwidth>
class ELFSectionProgBits : public ELFSectionBits<Bitwidth> {
public:
  ELF_TYPE_INTRO_TO_TEMPLATE_SCOPE(Bitwidth);

private:
  StubLayout *stubs;

public:
  template <typename Archiver>
  static ELFSectionProgBits *read(Archiver &AR,
                                  ELFObjectTy *owner,
                                  ELFSectionHeaderTy const *sh);

  StubLayout *getStubLayout() {
    return stubs;
  }

  ELFSectionProgBits(int machine) {
    switch(machine) {
    case EM_ARM:
        stubs = new StubLayoutARM();
      break;

    case EM_MIPS:
        stubs = new StubLayoutMIPS();
      break;

    default:
        stubs = NULL;
    }
  }

  ~ELFSectionProgBits() {
    if (stubs)
      delete stubs;
  }

private:
  template <typename Archiver>
  bool serialize(Archiver &AR) {
    ELFSectionHeaderTy const *sh = this->sh;
    MemChunk &chunk = this->chunk;

    AR.seek(sh->getOffset(), true);
    AR.prologue(sh->getSize());
    AR.readBytes(chunk.getBuffer(), sh->getSize());
    AR.epilogue(sh->getSize());

    return AR;
  }
};

#include "impl/ELFSectionProgBits.hxx"

#endif // ELF_SECTION_PROGBITS_H
