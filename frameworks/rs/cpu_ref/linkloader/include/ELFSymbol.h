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

#ifndef ELF_SYMBOL_H
#define ELF_SYMBOL_H

#include "ELFTypes.h"
#include "ELF.h"

#include <llvm/ADT/OwningPtr.h>

#include <string>
#include <algorithm>

#include <stdint.h>
#include <stdlib.h>

class ELFSymbolHelperMixin {
protected:
  static char const *getTypeStr(uint8_t);
  static char const *getBindingAttributeStr(uint8_t);
  static char const *getVisibilityStr(uint8_t);
};

template <unsigned Bitwidth>
class ELFSymbol_CRTP : private ELFSymbolHelperMixin {
public:
  ELF_TYPE_INTRO_TO_TEMPLATE_SCOPE(Bitwidth);

protected:
  ELFObject<Bitwidth> const *owner;

  size_t index;

  word_t st_name;
  byte_t st_info;
  byte_t st_other;
  half_t st_shndx;
  addr_t st_value;
  symsize_t st_size;

  mutable void *my_addr;

protected:
  ELFSymbol_CRTP() { my_addr = 0; }

  ~ELFSymbol_CRTP() {
#if 0
    if (my_addr != 0 &&
        getType() == STT_OBJECT &&
        getSectionIndex() == SHN_COMMON) {
      std::free(my_addr);
    }
#endif
  }

public:
  size_t getIndex() const {
    return index;
  }

  word_t getNameIndex() const {
    return st_name;
  }

  char const *getName() const;

// I don't want to include elf.h in .h file, so define those macro by ourself.
#define ELF_ST_BIND(i)   ((i)>>4)
#define ELF_ST_TYPE(i)   ((i)&0xf)
#define ELF_ST_INFO(b,t) (((b)<<4)+((t)&0xf))
  byte_t getType() const {
    return ELF_ST_TYPE(st_info);
  }

  byte_t getBindingAttribute() const {
    return ELF_ST_BIND(st_info);
  }
#undef ELF_ST_BIND
#undef ELF_ST_TYPE
#undef ELF_ST_INFO

#define ELF_ST_VISIBILITY(o) ((o)&0x3)
  byte_t getVisibility() const {
    return ELF_ST_VISIBILITY(st_other);
  }
#undef ELF_ST_VISIBILITY

  half_t getSectionIndex() const {
    return st_shndx;
  }

  addr_t getValue() const {
    return st_value;
  }

  symsize_t getSize() const {
    return st_size;
  }

  void *getAddress(int machine, bool autoAlloc = true) const;

  void setAddress(void *addr) {
    my_addr = addr;
  }

  bool isValid() const {
    // FIXME: Should check the correctness of the section header.
    return true;
  }

  bool isConcreteFunc() const {
    return getType() == STT_FUNC;
  }

  bool isExternFunc() const {
    return getType() == STT_NOTYPE;
  }

  template <typename Archiver>
  static ELFSymbolTy *
  read(Archiver &AR, ELFObject<Bitwidth> const *owner, size_t index = 0);

  void print(bool shouldPrintHeader = false) const;

private:
  ELFSymbolTy *concrete() {
    return static_cast<ELFSymbolTy *>(this);
  }

  ELFSymbolTy const *concrete() const {
    return static_cast<ELFSymbolTy const *>(this);
  }
};

template <>
class ELFSymbol<32> : public ELFSymbol_CRTP<32> {
  friend class ELFSymbol_CRTP<32>;

private:
  ELFSymbol() {
  }

  template <typename Archiver>
  bool serialize(Archiver &AR) {
    AR.prologue(TypeTraits<ELFSymbol>::size);

    AR & st_name;
    AR & st_value;
    AR & st_size;
    AR & st_info;
    AR & st_other;
    AR & st_shndx;

    AR.epilogue(TypeTraits<ELFSymbol>::size);
    return AR;
  }
};

template <>
class ELFSymbol<64> : public ELFSymbol_CRTP<64> {
  friend class ELFSymbol_CRTP<64>;

private:
  ELFSymbol() {
  }

  template <typename Archiver>
  bool serialize(Archiver &AR) {
    AR.prologue(TypeTraits<ELFSymbol>::size);

    AR & st_name;
    AR & st_info;
    AR & st_other;
    AR & st_shndx;
    AR & st_value;
    AR & st_size;

    AR.epilogue(TypeTraits<ELFSymbol>::size);
    return AR;
  }
};

#include "impl/ELFSymbol.hxx"

#endif // ELF_SYMBOL_H
