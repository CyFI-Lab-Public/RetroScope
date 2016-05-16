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

#ifndef ELF_SECTION_SYM_TAB_HXX
#define ELF_SECTION_SYM_TAB_HXX

#include "ELFSectionHeader.h"
#include "ELFSymbol.h"
#include "utils/rsl_assert.h"

template <unsigned Bitwidth>
ELFSectionSymTab<Bitwidth>::~ELFSectionSymTab() {
  for (size_t i = 0; i < table.size(); ++i) {
    delete table[i];
  }
}

template <unsigned Bitwidth>
size_t ELFSectionSymTab<Bitwidth>::getFuncCount() const {
  size_t result = 0;
  for (size_t i = 0; i < table.size(); ++i) {
    if (table[i] && table[i]->isConcreteFunc()) {
      result++;
    }
  }
  return result;
}

template <unsigned Bitwidth>
inline size_t ELFSectionSymTab<Bitwidth>::getExternFuncCount() const {
  size_t result = 0;
  for (size_t i = 0; i < table.size(); ++i) {
    if (table[i] && table[i]->isExternFunc()) {
      result++;
    }
  }
  return result;
}

template <unsigned Bitwidth>
inline void ELFSectionSymTab<Bitwidth>::buildNameMap() {
  for (size_t i = 0; i < table.size(); ++i) {
    ELFSymbolTy *symbol = table[i];
    if ( symbol ) {
      name_map[symbol->getName()] = symbol;
    }
  }
}

template <unsigned Bitwidth>
inline ELFSymbol<Bitwidth> const *
ELFSectionSymTab<Bitwidth>::getByName(std::string const &name) const {
  typename llvm::StringMap<ELFSymbolTy *>::const_iterator symbol =
    name_map.find(name);
  if (symbol == name_map.end()) {
    return NULL;
  }
  return symbol->getValue();
}

template <unsigned Bitwidth>
inline void
ELFSectionSymTab<Bitwidth>::getFuncNameList(size_t size,
                                            char const **list) const {
  for (size_t i = 0, j = 0; i < table.size() && j < size; ++i) {
    if (table[i] && table[i]->isConcreteFunc()) {
      list[j++] = table[i]->getName();
    }
  }
}

template <unsigned Bitwidth>
template <typename Archiver>
ELFSectionSymTab<Bitwidth> *
ELFSectionSymTab<Bitwidth>::read(Archiver &AR,
                                 ELFObjectTy *owner,
                                 ELFSectionHeaderTy const *sh) {

  llvm::OwningPtr<ELFSectionSymTabTy> st(new ELFSectionSymTabTy());

  // Assert that entry size will be the same as standard.
  rsl_assert(sh->getEntrySize() == TypeTraits<ELFSymbolTy>::size);

  // Seek to the start of symbol table
  AR.seek(sh->getOffset(), true);

  // Read all symbol table entry
  size_t size = sh->getSize() / sh->getEntrySize();
  for (size_t i = 0; i < size; ++i) {
    st->table.push_back(ELFSymbolTy::read(AR, owner, i));
  }

  if (!AR) {
    // Unable to read the table.
    return 0;
  }

  return st.take();
}

template <unsigned Bitwidth>
void ELFSectionSymTab<Bitwidth>::print() const {
  using namespace llvm;

  out() << '\n' << fillformat('=', 79) << '\n';
  out().changeColor(raw_ostream::WHITE, true);
  out() << "Symbol Table" << '\n';
  out().resetColor();

  for (size_t i = 0; i < table.size(); ++i) {
    table[i]->print();
  }

  out() << fillformat('=', 79) << '\n';
}

#endif // ELF_SECTION_SYM_TAB_HXX
