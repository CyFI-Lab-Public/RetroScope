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

#ifndef ELF_SECTION_REL_TABLE_HXX
#define ELF_SECTION_REL_TABLE_HXX

#include "ELF.h"
#include "ELFHeader.h"
#include "ELFObject.h"
#include "ELFReloc.h"
#include "ELFTypes.h"

#include <set>

template <unsigned Bitwidth>
ELFSectionRelTable<Bitwidth>::~ELFSectionRelTable() {
  using namespace std;
  for (size_t i = 0; i < table.size(); ++i) {
    delete table[i];
  }
}

template <unsigned Bitwidth>
void ELFSectionRelTable<Bitwidth>::print() const {
  using namespace llvm;

  out() << '\n' << fillformat('=', 79) << '\n';
  out().changeColor(raw_ostream::WHITE, true);
  out() << "Relocation Table" << '\n';
  out().resetColor();

  for (size_t i = 0; i < this->size(); ++i) {
    (*this)[i]->print();
  }

  out() << fillformat('=', 79) << '\n';
}

template <unsigned Bitwidth>
template <typename Archiver>
ELFSectionRelTable<Bitwidth> *
ELFSectionRelTable<Bitwidth>::read(Archiver &AR,
                                   ELFSectionHeaderTy const *sh) {

  rsl_assert(sh->getType() == SHT_REL || sh->getType() == SHT_RELA);

  llvm::OwningPtr<ELFSectionRelTable> rt(new ELFSectionRelTable());

  // Seek to the start of the table
  AR.seek(sh->getOffset(), true);

  // Count the relocation entries
  size_t size = sh->getSize() / sh->getEntrySize();

  // Read every relocation entries
  if (sh->getType() == SHT_REL) {
    rsl_assert(sh->getEntrySize() == TypeTraits<ELFRelocRelTy>::size);
    for (size_t i = 0; i < size; ++i) {
      rt->table.push_back(ELFRelocTy::readRel(AR, i));
    }

  } else {
    rsl_assert(sh->getEntrySize() == TypeTraits<ELFRelocRelaTy>::size);
    for (size_t i = 0; i < size; ++i) {
      rt->table.push_back(ELFRelocTy::readRela(AR, i));
    }
  }

  if (!AR) {
    // Unable to read the table.
    return 0;
  }

  return rt.take();
}

template <unsigned Bitwidth>
size_t ELFSectionRelTable<Bitwidth>::
getMaxNumStubs(ELFObjectTy const *obj) const {
  switch (obj->getHeader()->getMachine()) {
  case EM_ARM:
    {
      std::set<uint32_t> sym_index_set;

      for (size_t i = 0; i < size(); ++i) {
        ELFRelocTy *rel = table[i];

        switch (rel->getType()) {
        case R_ARM_CALL:
        case R_ARM_THM_CALL:
        case R_ARM_JUMP24:
        case R_ARM_THM_JUMP24:
          sym_index_set.insert(rel->getSymTabIndex());
          break;
        }
      }

      return sym_index_set.size();
    }

  case EM_MIPS:
    {
      std::set<uint32_t> sym_index_set;

      for (size_t i = 0; i < size(); ++i) {
        ELFRelocTy *rel = table[i];

        if (rel->getType() == R_MIPS_26) {
          sym_index_set.insert(rel->getSymTabIndex());
        }
      }

      return sym_index_set.size();
    }

  case EM_386:
  case EM_X86_64:
    return 0;

  default:
    rsl_assert(0 && "Only support ARM, MIPS, X86, and X86_64 relocation.");
    return 0;
  }
}

#endif // ELF_SECTION_REL_TABLE_HXX
