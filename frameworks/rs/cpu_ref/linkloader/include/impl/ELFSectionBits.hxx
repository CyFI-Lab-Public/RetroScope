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

#ifndef ELF_SECTION_BITS_HXX
#define ELF_SECTION_BITS_HXX

#include "utils/flush_cpu_cache.h"
#include "utils/helper.h"

#include <llvm/Support/raw_ostream.h>

#ifndef USE_MINGW       /* TODO create a proper HAVE_MMAN_H */
#include <sys/mman.h>
#else
#include "mmanWindows.h"
#endif

template <unsigned Bitwidth>
inline void ELFSectionBits<Bitwidth>::print() const {
  using namespace llvm;

  char const *section_type_str =
    (sh->getType() == SHT_NOBITS) ? "NOBITS" : "PROGBITS";

  out() << '\n' << fillformat('=', 79) << '\n';
  out().changeColor(raw_ostream::WHITE, true);
  out() << "ELF " << section_type_str << ": " << sh->getName() << '\n';
  out().resetColor();
  out() << fillformat('-', 79) << '\n';

  out() << "  Size         : " << sh->getSize() << '\n';
  out() << "  Start Address: " << (void *)chunk.getBuffer() << '\n';
  out() << fillformat('-', 79) << '\n';

  chunk.print();

  out() << fillformat('=', 79) << '\n';
}

template <unsigned Bitwidth>
inline bool ELFSectionBits<Bitwidth>::protect() {
  int prot = PROT_READ;

  if (sh->getFlags() & SHF_WRITE) {
    prot |= PROT_WRITE;
  }

  if (sh->getFlags() & SHF_EXECINSTR) {
    prot |= PROT_EXEC;
  }

  return chunk.protect(prot);
}

#endif // ELF_SECTION_BITS_HXX
