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

#ifndef ELF_SECTION_NOBITS_HXX
#define ELF_SECTION_NOBITS_HXX

#include "ELFTypes.h"

#include "utils/raw_ostream.h"

#include <llvm/Support/Format.h>
#include <llvm/Support/raw_ostream.h>

template <unsigned Bitwidth>
template <typename Archiver>
inline ELFSectionNoBits<Bitwidth> *
ELFSectionNoBits<Bitwidth>::read(Archiver &AR, ELFSectionHeaderTy const *sh) {
  llvm::OwningPtr<ELFSectionNoBits> result(new ELFSectionNoBits());

  if (!result->chunk.allocate(sh->getSize())) {
    return NULL;
  }

  result->sh = sh;

  return result.take();
}

#endif // ELF_SECTION_NOBITS_HXX
