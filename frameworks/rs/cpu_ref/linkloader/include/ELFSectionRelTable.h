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

#ifndef ELF_SECTION_REL_TABLE_H
#define ELF_SECTION_REL_TABLE_H

#include "ELFTypes.h"
#include "ELFSection.h"

#include <string>
#include <vector>

template <unsigned Bitwidth>
class ELFSectionRelTable : public ELFSection<Bitwidth> {
public:
  ELF_TYPE_INTRO_TO_TEMPLATE_SCOPE(Bitwidth);

private:
  std::vector<ELFRelocTy *> table;

private:
  ELFSectionRelTable() { }

public:
  virtual ~ELFSectionRelTable();

  virtual void print() const;

  template <typename Archiver>
  static ELFSectionRelTable *read(Archiver &AR, ELFSectionHeaderTy const *sh);

  size_t size() const {
    return table.size();
  }

  ELFRelocTy const *operator[](size_t index) const {
    return table[index];
  }

  ELFRelocTy *operator[](size_t index) {
    return table[index];
  }

  size_t getMaxNumStubs(ELFObjectTy const *) const;
};

#include "impl/ELFSectionRelTable.hxx"

#endif // ELF_SECTION_REL_TABLE_H
