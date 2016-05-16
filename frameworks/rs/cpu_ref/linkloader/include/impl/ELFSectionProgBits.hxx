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

#ifndef ELF_SECTION_PROGBITS_HXX
#define ELF_SECTION_PROGBITS_HXX

#include "ELFTypes.h"
#include "StubLayout.h"

#include <llvm/Support/Format.h>
#include <llvm/Support/raw_ostream.h>

#include "utils/raw_ostream.h"

#include <string.h>

template <unsigned Bitwidth>
template <typename Archiver>
ELFSectionProgBits<Bitwidth> *
ELFSectionProgBits<Bitwidth>::read(Archiver &AR,
                                   ELFObjectTy *owner,
                                   ELFSectionHeaderTy const *sh) {
  int machine = owner->getHeader()->getMachine();
  ELFSectionProgBits *secp = new ELFSectionProgBits(machine);
  llvm::OwningPtr<ELFSectionProgBits> result(secp);
  size_t max_num_stubs = 0;
  // Align section boundary to 4 bytes.
  size_t section_size = (sh->getSize() + 3) / 4 * 4;
  size_t alloc_size = section_size;
  StubLayout *stubs = result->getStubLayout();
  if (stubs) {
    // Compute the maximal possible numbers of stubs
    std::string reltab_name(".rel" + std::string(sh->getName()));

    ELFSectionRelTableTy const *reltab =
      static_cast<ELFSectionRelTableTy *>(
        owner->getSectionByName(reltab_name.c_str()));

    if (reltab) {
      // If we have relocation table, then get the approximation of
      // maximum numbers of stubs.
      max_num_stubs = reltab->getMaxNumStubs(owner);
    }

    // Compute the stub table size
    size_t stub_table_size = stubs->calcStubTableSize(max_num_stubs);

    // Allocate PROGBITS section with stubs table
    alloc_size += stub_table_size;
  }

  // Allocate text section
  if (!result->chunk.allocate(alloc_size)) {
    return NULL;
  }

  if (stubs) {
    stubs->initStubTable(result->chunk.getBuffer() + section_size,
                         max_num_stubs);
  }

  result->sh = sh;

  if (!result->serialize(AR)) {
    // Unable to read the progbits section.
    return NULL;
  }

  return result.take();
}

#endif // ELF_SECTION_PROGBITS_HXX
