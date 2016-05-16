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

#ifndef ELF_SECTION_H
#define ELF_SECTION_H

#include "ELFTypes.h"
#include <llvm/ADT/OwningPtr.h>

template <unsigned Bitwidth>
class ELFSection {
public:
  ELF_TYPE_INTRO_TO_TEMPLATE_SCOPE(Bitwidth);

protected:
  ELFSection() { }

public:
  virtual ~ELFSection() { }

  virtual void print() const = 0;

  template <typename Archiver>
  static ELFSection *read(Archiver &AR, ELFObjectTy *,
                          ELFSectionHeaderTy const *);
};

#include "impl/ELFSection.hxx"

#endif // ELF_SECTION_H
