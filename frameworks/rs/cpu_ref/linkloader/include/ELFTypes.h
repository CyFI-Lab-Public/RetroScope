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

#ifndef ELF_TYPES_H
#define ELF_TYPES_H

#include "utils/traits.h"

#include <stdint.h>
#include <llvm/Support/raw_ostream.h>


// ELF structure forward declarations
template <unsigned Bitwidth> class ELFHeader;
template <unsigned Bitwidth> class ELFObject;
template <unsigned Bitwidth> class ELFProgramHeader;
template <unsigned Bitwidth> class ELFReloc;
template <unsigned Bitwidth> class ELFRelocRel; // For TypeTraits
template <unsigned Bitwidth> class ELFRelocRela; // For TypeTraits
template <unsigned Bitwidth> class ELFSection;
template <unsigned Bitwidth> class ELFSectionBits;
template <unsigned Bitwidth> class ELFSectionHeader;
template <unsigned Bitwidth> class ELFSectionHeaderTable;
template <unsigned Bitwidth> class ELFSectionNoBits;
template <unsigned Bitwidth> class ELFSectionProgBits;
template <unsigned Bitwidth> class ELFSectionRelTable;
template <unsigned Bitwidth> class ELFSectionStrTab;
template <unsigned Bitwidth> class ELFSectionSymTab;
template <unsigned Bitwidth> class ELFSymbol;

// Note: Following TypeTraits specialization MUST be compliant to the
// System V Application Binary Interface, Chap 4, Data Representation.

TYPE_TRAITS_SPECIALIZE(ELFHeader<32>        , 52, 4)
TYPE_TRAITS_SPECIALIZE(ELFHeader<64>        , 64, 8)

TYPE_TRAITS_SPECIALIZE(ELFProgramHeader<32> , 32, 4)
TYPE_TRAITS_SPECIALIZE(ELFProgramHeader<64> , 56, 8)

TYPE_TRAITS_SPECIALIZE(ELFSectionHeader<32> , 40, 4)
TYPE_TRAITS_SPECIALIZE(ELFSectionHeader<64> , 64, 8)

TYPE_TRAITS_SPECIALIZE(ELFSymbol<32>        , 16, 4)
TYPE_TRAITS_SPECIALIZE(ELFSymbol<64>        , 24, 8)

TYPE_TRAITS_SPECIALIZE(ELFRelocRel<32>      , 8, 4)
TYPE_TRAITS_SPECIALIZE(ELFRelocRel<64>      , 16, 8)

TYPE_TRAITS_SPECIALIZE(ELFRelocRela<32>     , 12, 4)
TYPE_TRAITS_SPECIALIZE(ELFRelocRela<64>     , 24, 8)


// ELF primitive type wrappers
namespace detail {
#define ELF_TYPE_WRAPPER(TYPE, IMPL)                                        \
  struct TYPE {                                                             \
    IMPL value;                                                             \
                                                                            \
    TYPE() : value(0) { }                                                   \
    TYPE(IMPL val) : value(val) { }                                         \
                                                                            \
    TYPE &operator=(TYPE const &with) { value = with.value; return *this; } \
    TYPE &operator=(IMPL val) { value = val; return *this; }                \
                                                                            \
    operator IMPL() const { return value; }                                 \
  };

  ELF_TYPE_WRAPPER(ELFHalf      , uint16_t)
  ELF_TYPE_WRAPPER(ELFWord      , uint32_t)
  ELF_TYPE_WRAPPER(ELFSword     , int32_t)
  ELF_TYPE_WRAPPER(ELFXword     , uint64_t)
  ELF_TYPE_WRAPPER(ELFSxword    , int64_t)
  ELF_TYPE_WRAPPER(ELF32Address , uint32_t)
  ELF_TYPE_WRAPPER(ELF32Offset  , uint32_t)
  ELF_TYPE_WRAPPER(ELF64Address , uint64_t)
  ELF_TYPE_WRAPPER(ELF64Offset  , uint64_t)

#undef ELF_TYPE_WRAPPER

  extern llvm::raw_ostream &operator<<(llvm::raw_ostream &,
                                       ELF32Address const &);
  extern llvm::raw_ostream &operator<<(llvm::raw_ostream &,
                                       ELF32Offset const &);
  extern llvm::raw_ostream &operator<<(llvm::raw_ostream &,
                                       ELF64Address const &);
  extern llvm::raw_ostream &operator<<(llvm::raw_ostream &,
                                       ELF64Offset const &);
}

// Note: Following TypeTraits specialization MUST be compliant to the
// System V Application Binary Interface, Chap 4, Data Representation.

TYPE_TRAITS_SPECIALIZE(detail::ELFHalf      , 2, 2)
TYPE_TRAITS_SPECIALIZE(detail::ELFWord      , 4, 4)
TYPE_TRAITS_SPECIALIZE(detail::ELFSword     , 4, 4)
TYPE_TRAITS_SPECIALIZE(detail::ELFXword     , 8, 8)
TYPE_TRAITS_SPECIALIZE(detail::ELFSxword    , 8, 8)
TYPE_TRAITS_SPECIALIZE(detail::ELF32Address , 4, 4)
TYPE_TRAITS_SPECIALIZE(detail::ELF32Offset  , 4, 4)
TYPE_TRAITS_SPECIALIZE(detail::ELF64Address , 8, 8)
TYPE_TRAITS_SPECIALIZE(detail::ELF64Offset  , 8, 8)

template <unsigned Bitwidth>
struct ELFPrimitiveTypes;

template <>
struct ELFPrimitiveTypes<32> {
  typedef detail::ELF32Address  address;
  typedef detail::ELF32Offset   offset;

  typedef unsigned char         byte;
  typedef detail::ELFHalf       half;
  typedef detail::ELFWord       word;
  typedef detail::ELFSword      sword;

  typedef detail::ELFWord       relinfo;
  typedef detail::ELFSword      addend;
  typedef detail::ELFWord       symsize;

  // Note: Don't use these types.  They are not in the specification of
  // ELF 32.  However, we need these typedefs to define the type introduce
  // macro.
  typedef void xword;
  typedef void sxword;
};

template <>
struct ELFPrimitiveTypes<64> {
  typedef detail::ELF64Address  address;
  typedef detail::ELF64Offset   offset;

  typedef unsigned char         byte;
  typedef detail::ELFHalf       half;
  typedef detail::ELFWord       word;
  typedef detail::ELFSword      sword;
  typedef detail::ELFXword      xword;
  typedef detail::ELFSxword     sxword;

  typedef detail::ELFXword      relinfo;
  typedef detail::ELFSxword     addend;
  typedef detail::ELFXword      symsize;
};


// Macros to introduce these ELF types to a specific scope

#define ELF_STRUCT_TYPE_INTRO_TO_SCOPE(BITWIDTH)                            \
  typedef ELFHeader<BITWIDTH>             ELFHeaderTy;                      \
  typedef ELFObject<BITWIDTH>             ELFObjectTy;                      \
  typedef ELFProgramHeader<BITWIDTH>      ELFProgramHeaderTy;               \
  typedef ELFReloc<BITWIDTH>              ELFRelocTy;                       \
  typedef ELFRelocRel<BITWIDTH>           ELFRelocRelTy;                    \
  typedef ELFRelocRela<BITWIDTH>          ELFRelocRelaTy;                   \
  typedef ELFSection<BITWIDTH>            ELFSectionTy;                     \
  typedef ELFSectionBits<BITWIDTH>        ELFSectionBitsTy;                 \
  typedef ELFSectionHeader<BITWIDTH>      ELFSectionHeaderTy;               \
  typedef ELFSectionHeaderTable<BITWIDTH> ELFSectionHeaderTableTy;          \
  typedef ELFSectionNoBits<BITWIDTH>      ELFSectionNoBitsTy;               \
  typedef ELFSectionProgBits<BITWIDTH>    ELFSectionProgBitsTy;             \
  typedef ELFSectionRelTable<BITWIDTH>    ELFSectionRelTableTy;             \
  typedef ELFSectionStrTab<BITWIDTH>      ELFSectionStrTabTy;               \
  typedef ELFSectionSymTab<BITWIDTH>      ELFSectionSymTabTy;               \
  typedef ELFSymbol<BITWIDTH>             ELFSymbolTy;


#define ELF_TYPE_INTRO_TO_TEMPLATE_SCOPE(BITWIDTH)                          \
  /* ELF structures */                                                      \
  ELF_STRUCT_TYPE_INTRO_TO_SCOPE(BITWIDTH)                                  \
                                                                            \
  /* ELF primitives */                                                      \
  typedef typename ELFPrimitiveTypes<BITWIDTH>::address addr_t;             \
  typedef typename ELFPrimitiveTypes<BITWIDTH>::offset  offset_t;           \
  typedef typename ELFPrimitiveTypes<BITWIDTH>::byte    byte_t;             \
  typedef typename ELFPrimitiveTypes<BITWIDTH>::half    half_t;             \
  typedef typename ELFPrimitiveTypes<BITWIDTH>::word    word_t;             \
  typedef typename ELFPrimitiveTypes<BITWIDTH>::sword   sword_t;            \
  typedef typename ELFPrimitiveTypes<BITWIDTH>::xword   xword_t;            \
  typedef typename ELFPrimitiveTypes<BITWIDTH>::sxword  sxword_t;           \
  typedef typename ELFPrimitiveTypes<BITWIDTH>::relinfo relinfo_t;          \
  typedef typename ELFPrimitiveTypes<BITWIDTH>::addend  addend_t;           \
  typedef typename ELFPrimitiveTypes<BITWIDTH>::symsize symsize_t;


#define ELF_TYPE_INTRO_TO_SCOPE(BITWIDTH)                                   \
  /* ELF structures */                                                      \
  ELF_STRUCT_TYPE_INTRO_TO_SCOPE(BITWIDTH)                                  \
                                                                            \
  /* ELF primitives */                                                      \
  typedef ELFPrimitiveTypes<BITWIDTH>::address  addr_t;                     \
  typedef ELFPrimitiveTypes<BITWIDTH>::offset   offset_t;                   \
  typedef ELFPrimitiveTypes<BITWIDTH>::byte     byte_t;                     \
  typedef ELFPrimitiveTypes<BITWIDTH>::half     half_t;                     \
  typedef ELFPrimitiveTypes<BITWIDTH>::word     word_t;                     \
  typedef ELFPrimitiveTypes<BITWIDTH>::sword    sword_t;                    \
  typedef ELFPrimitiveTypes<BITWIDTH>::xword    xword_t;                    \
  typedef ELFPrimitiveTypes<BITWIDTH>::sxword   sxword_t;                   \
  typedef ELFPrimitiveTypes<BITWIDTH>::relinfo  relinfo_t;                  \
  typedef ELFPrimitiveTypes<BITWIDTH>::addend   addend_t;                   \
  typedef ELFPrimitiveTypes<BITWIDTH>::symsize  symsize_t;


#endif // ELF_TYPES_H
