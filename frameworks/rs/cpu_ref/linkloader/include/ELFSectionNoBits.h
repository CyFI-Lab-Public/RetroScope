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

#ifndef ELF_SECTION_NOBITS_H
#define ELF_SECTION_NOBITS_H

#include "ELFTypes.h"
#include "ELFSectionBits.h"
#include "ELFSectionHeader.h"
#include "MemChunk.h"

template <unsigned Bitwidth>
class ELFSectionNoBits : public ELFSectionBits<Bitwidth> {
public:
  ELF_TYPE_INTRO_TO_TEMPLATE_SCOPE(Bitwidth);

public:
  template <typename Archiver>
  static ELFSectionNoBits *read(Archiver &AR, ELFSectionHeaderTy const *sh);
};

#include "impl/ELFSectionNoBits.hxx"

#endif // ELF_SECTION_NOBITS_H
