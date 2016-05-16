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

#ifndef ELF_SECTION_SYMTAB_H
#define ELF_SECTION_SYMTAB_H

#include "ELFTypes.h"

#include <llvm/ADT/StringMap.h>

#include <vector>
#include <string>

template <unsigned Bitwidth>
class ELFSectionSymTab : public ELFSection<Bitwidth> {
public:
  ELF_TYPE_INTRO_TO_TEMPLATE_SCOPE(Bitwidth);

private:
  std::vector<ELFSymbolTy *> table;
  llvm::StringMap<ELFSymbolTy *> name_map;

private:
  ELFSectionSymTab() { }

public:
  ~ELFSectionSymTab();

  template <typename Archiver>
  static ELFSectionSymTab *
  read(Archiver &AR, ELFObjectTy *owner, ELFSectionHeaderTy const *sh);

  virtual void print() const;

  size_t size() const {
    return table.size();
  }

  ELFSymbolTy const *operator[](size_t index) const {
    return table[index];
  }

  ELFSymbolTy *operator[](size_t index) {
    return table[index];
  }

  void buildNameMap();

  ELFSymbolTy const *getByName(std::string const &name) const;

  ELFSymbolTy *getByName(std::string const &name) {
    return const_cast<ELFSymbolTy *>(
           const_cast<ELFSectionSymTabTy const *>(this)->getByName(name));
  }

  size_t getFuncCount() const;

  void getFuncNameList(size_t size, char const **list) const;

  size_t getExternFuncCount() const;

};

#include "impl/ELFSectionSymTab.hxx"

#endif // ELF_SECTION_SYMTAB_H
