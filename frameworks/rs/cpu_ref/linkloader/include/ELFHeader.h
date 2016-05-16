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

#ifndef ELF_HEADER_H
#define ELF_HEADER_H

#include "ELFTypes.h"
#include "ELF.h"

#include <llvm/ADT/OwningPtr.h>

#include <string.h>

class ELFHeaderHelperMixin {
protected:
  static char const *getClassStr(int clazz);
  static char const *getEndiannessStr(int endianness);
  static char const *getOSABIStr(int abi);
  static char const *getObjectTypeStr(uint16_t type);
  static char const *getMachineStr(uint16_t machine);
  static char const *getVersionStr(uint32_t version);
};

template <unsigned Bitwidth>
class ELFHeader : private ELFHeaderHelperMixin {
public:
  ELF_TYPE_INTRO_TO_TEMPLATE_SCOPE(Bitwidth);

protected:
  byte_t   e_ident[EI_NIDENT];
  half_t   e_type;
  half_t   e_machine;
  word_t   e_version;
  addr_t   e_entry;
  offset_t e_phoff;
  offset_t e_shoff;
  word_t   e_flags;
  half_t   e_ehsize;
  half_t   e_phentsize;
  half_t   e_phnum;
  half_t   e_shentsize;
  half_t   e_shnum;
  half_t   e_shstrndx;

protected:
  ELFHeader() { }

public:
  byte_t getClass() const {
    return e_ident[EI_CLASS];
  }

  byte_t getEndianness() const {
    return e_ident[EI_DATA];
  }

  byte_t getVersionFromIdent() const {
    return e_ident[EI_VERSION];
  }

  byte_t getOSABI() const {
    return e_ident[EI_OSABI];
  }

  byte_t getABIVersion() const {
    return e_ident[EI_ABIVERSION];
  }

  bool is32bit() const {
    return e_ident[EI_CLASS] == ELFCLASS32;
  }

  bool is64bit() const {
    return e_ident[EI_CLASS] == ELFCLASS64;
  }

  bool isBigEndian() const {
    return e_ident[EI_DATA] == ELFDATA2MSB;
  }

  bool isLittleEndian() const {
    return e_ident[EI_DATA] == ELFDATA2LSB;
  }

  half_t getObjectType() const {
    return e_type;
  }

  half_t getMachine() const {
    return e_machine;
  }

  word_t getVersion() const {
    return e_version;
  }

  addr_t getEntryAddress() const {
    return e_entry;
  }

  offset_t getProgramHeaderTableOffset() const {
    return e_phoff;
  }

  offset_t getSectionHeaderTableOffset() const {
    return e_shoff;
  }

  word_t getFlags() const {
    return e_flags;
  }

  half_t getELFHeaderSize() const {
    return e_ehsize;
  }

  half_t getProgramHeaderEntrySize() const {
    return e_phentsize;
  }

  half_t getProgramHeaderNum() const {
    return e_phnum;
  }

  half_t getSectionHeaderEntrySize() const {
    return e_shentsize;
  }

  half_t getSectionHeaderNum() const {
    return e_shnum;
  }

  half_t getStringSectionIndex() const {
    return e_shstrndx;
  }

  template <typename Archiver>
  static ELFHeader *read(Archiver &AR) {
    if (!AR) {
      // Archiver is in bad state before calling read function.
      // Return NULL and do nothing.
      return 0;
    }

    llvm::OwningPtr<ELFHeader> header(new ELFHeader());
    if (!header->serialize(AR)) {
      // Unable to read the structure.  Return NULL.
      return 0;
    }

    if (!header->isValid()) {
      // Header read from archiver is not valid.  Return NULL.
      return 0;
    }

    return header.take();
  }

  void print();

  bool isValid() const {
    return (isValidELFIdent() && isCompatibleHeaderSize());
  }

private:
  template <typename Archiver>
  bool serialize(Archiver &AR) {
    AR.prologue(TypeTraits<ELFHeaderTy>::size);

    AR & e_ident;
    AR & e_type;
    AR & e_machine;
    AR & e_version;
    AR & e_entry;
    AR & e_phoff;
    AR & e_shoff;
    AR & e_flags;
    AR & e_ehsize;
    AR & e_phentsize;
    AR & e_phnum;
    AR & e_shentsize;
    AR & e_shnum;
    AR & e_shstrndx;

    AR.epilogue(TypeTraits<ELFHeaderTy>::size);
    return AR;
  }

  bool isValidMagicWord() const {
    return (memcmp(e_ident, "\x7f" "ELF", 4) == 0);
  }

  bool isValidClass() const {
    return ((Bitwidth == 32 && is32bit()) ||
            (Bitwidth == 64 && is64bit()));
  }

  bool isValidEndianness() const {
    return (isBigEndian() || isLittleEndian());
  }

  bool isValidHeaderVersion() const {
    return (getVersion() == EV_CURRENT);
  }

  bool isUnusedZeroedPadding() const {
    for (size_t i = EI_PAD; i < EI_NIDENT; ++i) {
      if (e_ident[i] != 0) {
        return false;
      }
    }
    return true;
  }

  bool isValidELFIdent() const {
    return (isValidMagicWord() &&
            isValidClass() &&
            isValidEndianness() &&
            isValidHeaderVersion() &&
            isUnusedZeroedPadding());
  }

  bool isCompatibleHeaderSize() const {
    return (
      (e_ehsize == TypeTraits<ELFHeaderTy>::size) &&
      (e_phnum == 0 || e_phentsize == TypeTraits<ELFProgramHeaderTy>::size) &&
      (e_shnum == 0 || e_shentsize == TypeTraits<ELFSectionHeaderTy>::size));
  }
};

#include "impl/ELFHeader.hxx"

#endif // ELF_HEADER_H
