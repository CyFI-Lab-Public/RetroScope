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

#ifndef ELF_SECTION_HXX
#define ELF_SECTION_HXX

#include "utils/raw_ostream.h"

#include <llvm/Support/raw_ostream.h>

#include "ELFSectionHeader.h"
#include "ELFSectionStrTab.h"
#include "ELFSectionSymTab.h"
#include "ELFSectionProgBits.h"
#include "ELFSectionNoBits.h"
#include "ELFSectionRelTable.h"

template <unsigned Bitwidth>
template <typename Archiver>
inline ELFSection<Bitwidth> *
ELFSection<Bitwidth>::read(Archiver &AR,
                           ELFObjectTy *owner,
                           ELFSectionHeaderTy const *sh) {
  using namespace std;

  switch (sh->getType()) {
    default:
      // Uknown type of ELF section.  Return NULL.
      //llvm::errs() << "WARNING: Unknown section type.\n";
      return 0;

    case SHT_STRTAB:
      return ELFSectionStrTabTy::read(AR, sh);

    case SHT_SYMTAB:
      return ELFSectionSymTabTy::read(AR, owner, sh);

    case SHT_PROGBITS:
      return ELFSectionProgBitsTy::read(AR, owner, sh);

    case SHT_NOBITS:
      return ELFSectionNoBitsTy::read(AR, sh);

    case SHT_REL:
    case SHT_RELA:
      return ELFSectionRelTableTy::read(AR, sh);

    case SHT_NULL:
      // TODO: Not Yet Implemented
      return 0;
  };
}

#endif // ELF_SECTION_HXX
