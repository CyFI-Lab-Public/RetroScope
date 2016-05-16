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

#ifndef ELF_RELOC_H
#define ELF_RELOC_H

#include "ELFTypes.h"
#include "utils/rsl_assert.h"

#include <llvm/ADT/OwningPtr.h>
#include <string>
#include <stdint.h>

template <unsigned Bitwidth>
class ELFReloc_CRTP {
public:
  ELF_TYPE_INTRO_TO_TEMPLATE_SCOPE(Bitwidth);

protected:
  size_t index;

  addr_t    r_offset;
  relinfo_t r_info;
  addend_t  r_addend;

protected:
  ELFReloc_CRTP() : index(0), r_offset(0), r_addend(0) { }
  ~ELFReloc_CRTP() { }

public:
  size_t getIndex() const {
    return index;
  }

  addr_t getOffset() const {
    return r_offset;
  }

  addend_t getAddend() const {
    return r_addend;
  }

  bool isValid() const {
    // FIXME: Should check the correctness of the relocation entite.
    return true;
  }

  template <typename Archiver>
  static ELFRelocTy *readRel(Archiver &AR, size_t index);

  template <typename Archiver>
  static ELFRelocTy *readRela(Archiver &AR, size_t index);

  void print(bool shouldPrintHeader = false) const;

private:
  ELFRelocTy *concrete() {
    return static_cast<ELFRelocTy *>(this);
  }

  ELFRelocTy const *concrete() const {
    return static_cast<ELFRelocTy const *>(this);
  }

  template <typename Archiver>
  bool serializeRel(Archiver &AR) {
    rsl_assert(r_addend == 0 && "r_addend should be zero before serialization.");

    AR.prologue(TypeTraits<ELFRelocRelTy>::size);

    AR & r_offset;
    AR & r_info;

    AR.epilogue(TypeTraits<ELFRelocRelTy>::size);
    return AR;
  }

  template <typename Archiver>
  bool serializeRela(Archiver &AR) {
    AR.prologue(TypeTraits<ELFRelocRelaTy>::size);

    AR & r_offset;
    AR & r_info;
    AR & r_addend;

    AR.epilogue(TypeTraits<ELFRelocRelaTy>::size);
    return AR;
  }

};

template <>
class ELFReloc<32> : public ELFReloc_CRTP<32> {
  friend class ELFReloc_CRTP<32>;

private:
  ELFReloc() {
  }

public:
  word_t getSymTabIndex() const {
#define ELF32_R_SYM(i)  ((i)>>8)
    return ELF32_R_SYM(this->r_info);
#undef ELF32_R_SYM
  }

  word_t getType() const {
#define ELF32_R_TYPE(i)   ((unsigned char)(i))
    return ELF32_R_TYPE(this->r_info);
#undef ELF32_R_TYPE
  }

};

template <>
class ELFReloc<64> : public ELFReloc_CRTP<64> {
  friend class ELFReloc_CRTP<64>;

private:
  ELFReloc() {
  }

public:
  xword_t getSymTabIndex() const {
#define ELF64_R_SYM(i)    ((i)>>32)
    return ELF64_R_SYM(this->r_info);
#undef ELF64_R_SYM
  }

  xword_t getType() const {
#define ELF64_R_TYPE(i)   ((i)&0xffffffffL)
    return ELF64_R_TYPE(this->r_info);
#undef ELF64_R_TYPE
  }
};

#include "impl/ELFReloc.hxx"

#endif // ELF_RELOC_H
