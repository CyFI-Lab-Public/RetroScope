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

#ifndef ELF_SECTION_STR_TAB_HXX
#define ELF_SECTION_STR_TAB_HXX

#include "utils/helper.h"
#include "utils/raw_ostream.h"

#include <llvm/ADT/OwningPtr.h>
#include <llvm/Support/Format.h>
#include <llvm/Support/raw_ostream.h>

template <unsigned Bitwidth>
template <typename Archiver>
ELFSectionStrTab<Bitwidth> *
ELFSectionStrTab<Bitwidth>::read(Archiver &AR,
                                 ELFSectionHeaderTy const *sh) {

  llvm::OwningPtr<ELFSectionStrTab> st(new ELFSectionStrTab());
  st->buf.resize(sh->getSize());

  // Save section_header
  st->section_header = sh;

  AR.seek(sh->getOffset(), true);
  AR.prologue(sh->getSize());
  AR.readBytes(&*st->buf.begin(), sh->getSize());
  AR.epilogue(sh->getSize());

  if (!AR) {
    // Unable to read the string table.
    return 0;
  }

  return st.take();
}

template <unsigned Bitwidth>
void ELFSectionStrTab<Bitwidth>::print() const {
  using namespace llvm;

  out() << '\n' << fillformat('=', 79) << '\n';
  out().changeColor(raw_ostream::WHITE, true);
  out() << "ELF String Table: " << this->section_header->getName() << '\n';
  out().resetColor();
  out() << fillformat('-', 79) << '\n';

  dump_hex((unsigned char const *)&*buf.begin(), buf.size(), 0, buf.size());

  out() << fillformat('=', 79) << '\n';
}

#endif // ELF_SECTION_STR_TAB_HXX
