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

#ifndef ELF_SECTION_HEADER_H
#define ELF_SECTION_HEADER_H

#include "ELFTypes.h"

#include <llvm/ADT/OwningPtr.h>
#include <stdint.h>

class ELFSectionHeaderHelperMixin {
protected:
  static char const *getSectionTypeStr(uint32_t type);
};

template <unsigned Bitwidth>
class ELFSectionHeader_CRTP : private ELFSectionHeaderHelperMixin {
public:
  ELF_TYPE_INTRO_TO_TEMPLATE_SCOPE(Bitwidth);

protected:
  ELFObjectTy const *owner;

  size_t index;

  word_t sh_name;
  word_t sh_type;
  addr_t sh_addr;
  offset_t sh_offset;
  word_t sh_link;
  word_t sh_info;

protected:
  ELFSectionHeader_CRTP() { }
  ~ELFSectionHeader_CRTP() { }

public:
  size_t getIndex() const {
    return index;
  }

  word_t getNameIndex() const {
    return sh_name;
  }

  char const *getName() const;

  word_t getType() const {
    return sh_type;
  }

  addr_t getAddress() const {
    return sh_addr;
  }

  offset_t getOffset() const {
    return sh_offset;
  }

  word_t getLink() const {
    return sh_link;
  }

  word_t getExtraInfo() const {
    return sh_info;
  }

  bool isValid() const {
    // FIXME: Should check the correctness of the section header.
    return true;
  }

  template <typename Archiver>
  static ELFSectionHeaderTy *
  read(Archiver &AR, ELFObjectTy const *owner, size_t index = 0);

  void print(bool shouldPrintHeader = false) const;

private:
  ELFSectionHeaderTy *concrete() {
    return static_cast<ELFSectionHeaderTy *>(this);
  }

  ELFSectionHeaderTy const *concrete() const {
    return static_cast<ELFSectionHeaderTy const *>(this);
  }
};


#include "impl/ELFSectionHeader.hxx"

template <>
class ELFSectionHeader<32> : public ELFSectionHeader_CRTP<32> {
  friend class ELFSectionHeader_CRTP<32>;

private:
  word_t sh_flags;
  word_t sh_size;
  word_t sh_addralign;
  word_t sh_entsize;

private:
  ELFSectionHeader() {
  }

  template <typename Archiver>
  bool serialize(Archiver &AR) {
    AR.prologue(TypeTraits<ELFSectionHeader>::size);

    AR & sh_name;
    AR & sh_type;
    AR & sh_flags;
    AR & sh_addr;
    AR & sh_offset;
    AR & sh_size;
    AR & sh_link;
    AR & sh_info;
    AR & sh_addralign;
    AR & sh_entsize;

    AR.epilogue(TypeTraits<ELFSectionHeader>::size);
    return AR;
  }

public:
  word_t getFlags() const {
    return sh_flags;
  }

  word_t getSize() const {
    return sh_size;
  }

  word_t getAddressAlign() const {
    return sh_addralign;
  }

  word_t getEntrySize() const {
    return sh_entsize;
  }
};

template <>
class ELFSectionHeader<64> : public ELFSectionHeader_CRTP<64> {
  friend class ELFSectionHeader_CRTP<64>;

private:
  xword_t sh_flags;
  xword_t sh_size;
  xword_t sh_addralign;
  xword_t sh_entsize;

private:
  ELFSectionHeader() {
  }

  template <typename Archiver>
  bool serialize(Archiver &AR) {
    AR.prologue(TypeTraits<ELFSectionHeader>::size);

    AR & sh_name;
    AR & sh_type;
    AR & sh_flags;
    AR & sh_addr;
    AR & sh_offset;
    AR & sh_size;
    AR & sh_link;
    AR & sh_info;
    AR & sh_addralign;
    AR & sh_entsize;

    AR.epilogue(TypeTraits<ELFSectionHeader>::size);
    return AR;
  }

public:
  xword_t getFlags() const {
    return sh_flags;
  }

  xword_t getSize() const {
    return sh_size;
  }

  xword_t getAddressAlign() const {
    return sh_addralign;
  }

  xword_t getEntrySize() const {
    return sh_entsize;
  }
};

#endif // ELF_SECTION_HEADER_H
