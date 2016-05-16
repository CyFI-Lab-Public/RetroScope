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

#ifndef ELF_SECTION_HEADER_TABLE_H
#define ELF_SECTION_HEADER_TABLE_H

#include "ELFTypes.h"

#include <llvm/ADT/OwningPtr.h>
#include <llvm/ADT/StringMap.h>

#include <vector>
#include <string>

template <unsigned Bitwidth>
class ELFSectionHeaderTable {
public:
  ELF_TYPE_INTRO_TO_TEMPLATE_SCOPE(Bitwidth);

private:
  std::vector<ELFSectionHeaderTy *> table;
  llvm::StringMap<ELFSectionHeaderTy *> name_map;

private:
  ELFSectionHeaderTable() {
  }

public:
  ~ELFSectionHeaderTable();

  template <typename Archiver>
  static ELFSectionHeaderTableTy *read(Archiver &AR, ELFObjectTy *owner);

  ELFSectionHeaderTy const *operator[](size_t i) const {
    return table[i];
  }

  ELFSectionHeaderTy *operator[](size_t i) {
    return table[i];
  }

  void buildNameMap();

  ELFSectionHeaderTy const *getByName(const std::string &name) const;
  ELFSectionHeaderTy *getByName(const std::string &name);

  void print() const;
};

#include "impl/ELFSectionHeaderTable.hxx"

#endif // ELF_SECTION_HEADER_TABLE_H
