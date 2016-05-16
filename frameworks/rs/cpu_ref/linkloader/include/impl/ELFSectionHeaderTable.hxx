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

#ifndef ELF_SECTION_HEADER_TABLE_HXX
#define ELF_SECTION_HEADER_TABLE_HXX

#include "ELFHeader.h"
#include "ELFObject.h"
#include "ELFSectionHeader.h"

#include "utils/rsl_assert.h"

template <unsigned Bitwidth>
ELFSectionHeaderTable<Bitwidth>::~ELFSectionHeaderTable() {
  for (size_t i = 0; i < table.size(); ++i) {
    delete table[i];
  }
}

template <unsigned Bitwidth>
template <typename Archiver>
inline ELFSectionHeaderTable<Bitwidth> *
ELFSectionHeaderTable<Bitwidth>::read(Archiver &AR, ELFObjectTy *owner) {
  if (!AR) {
    // Archiver is in bad state before calling read function.
    // Return NULL and do nothing.
    return 0;
  }

  // Allocate a new section header table and assign the owner.
  llvm::OwningPtr<ELFSectionHeaderTable> tab(new ELFSectionHeaderTable());

  // Get ELF header
  ELFHeaderTy const *header = owner->getHeader();

  rsl_assert(header->getSectionHeaderEntrySize() ==
         TypeTraits<ELFSectionHeaderTy>::size);

  // Seek to the address of section header
  AR.seek(header->getSectionHeaderTableOffset(), true);

  for (size_t i = 0; i < header->getSectionHeaderNum(); ++i) {
    llvm::OwningPtr<ELFSectionHeaderTy> sh(
      ELFSectionHeaderTy::read(AR, owner, i));

    if (!sh) {
      // Something wrong while reading the section header.
      return 0;
    }

    tab->table.push_back(sh.take());
  }

  return tab.take();
}

template <unsigned Bitwidth>
inline void ELFSectionHeaderTable<Bitwidth>::print() const {
  using namespace llvm;

  out() << '\n' << fillformat('=', 79) << '\n';
  out().changeColor(raw_ostream::WHITE, true);
  out() << "ELF Section Header Table" << '\n';
  out().resetColor();

  for (size_t i = 0; i < table.size(); ++i) {
    (*this)[i]->print();
  }

  out() << fillformat('=', 79) << '\n';
}

template <unsigned Bitwidth>
inline void ELFSectionHeaderTable<Bitwidth>::buildNameMap() {
  for (size_t i = 0; i < table.size(); ++i) {
    ELFSectionHeaderTy *sh = table[i];
    if ( sh ) {
      name_map[sh->getName()] = sh;
    }
  }
}

template <unsigned Bitwidth>
inline ELFSectionHeader<Bitwidth> const *
ELFSectionHeaderTable<Bitwidth>::getByName(const std::string &name) const {
  typename llvm::StringMap<ELFSectionHeaderTy *>::const_iterator sh =
    name_map.find(name);
  if (sh == name_map.end()) {
    // Return SHN_UNDEF section header;
    return table[0];
  }
  return sh->getValue();
}

template <unsigned Bitwidth>
inline ELFSectionHeader<Bitwidth> *
ELFSectionHeaderTable<Bitwidth>::getByName(const std::string &name) {
  ELFSectionHeaderTableTy const *const_this = this;
  ELFSectionHeaderTy const *shptr = const_this->getByName(name);
  // Const cast for the same API's const and non-const versions.
  return const_cast<ELFSectionHeaderTy *>(shptr);
}

#endif // ELF_SECTION_HEADER_TABLE_HXX
