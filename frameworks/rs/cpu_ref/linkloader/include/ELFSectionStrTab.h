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

#ifndef ELF_SECTION_STRTAB_H
#define ELF_SECTION_STRTAB_H

#include "ELFTypes.h"
#include "ELFSection.h"

#include <vector>

template <unsigned Bitwidth>
class ELFSectionStrTab : public ELFSection<Bitwidth> {
public:
  ELF_TYPE_INTRO_TO_TEMPLATE_SCOPE(Bitwidth);

private:
  ELFSectionHeaderTy const *section_header;
  std::vector<char> buf;

private:
  ELFSectionStrTab() { }

public:
  template <typename Archiver>
  static ELFSectionStrTab *read(Archiver &AR, ELFSectionHeaderTy const *sh);

  virtual void print() const;

  char const *operator[](size_t index) const {
    return &*buf.begin() + index;
  }
};

#include "impl/ELFSectionStrTab.hxx"

#endif // ELF_SECTION_STRTAB_H
