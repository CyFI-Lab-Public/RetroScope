//===- MipsRelocationFunction.h -------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//

#define DECL_MIPS_APPLY_RELOC_FUNC(Name) \
static MipsRelocator::Result Name(Relocation& pEntry, \
                                  MipsRelocator& pParent);

#define DECL_MIPS_APPLY_RELOC_FUNCS \
DECL_MIPS_APPLY_RELOC_FUNC(none) \
DECL_MIPS_APPLY_RELOC_FUNC(abs32) \
DECL_MIPS_APPLY_RELOC_FUNC(hi16) \
DECL_MIPS_APPLY_RELOC_FUNC(lo16) \
DECL_MIPS_APPLY_RELOC_FUNC(got16) \
DECL_MIPS_APPLY_RELOC_FUNC(call16) \
DECL_MIPS_APPLY_RELOC_FUNC(gprel32) \
DECL_MIPS_APPLY_RELOC_FUNC(gothi16) \
DECL_MIPS_APPLY_RELOC_FUNC(gotlo16)

#define DECL_MIPS_APPLY_RELOC_FUNC_PTRS \
  { &none,     0, "R_MIPS_NONE",             0}, \
  { &none,     1, "R_MIPS_16",              16}, \
  { &abs32,    2, "R_MIPS_32",              32}, \
  { &none,     3, "R_MIPS_REL32",           32}, \
  { &none,     4, "R_MIPS_26",              64}, \
  { &hi16,     5, "R_MIPS_HI16",            16}, \
  { &lo16,     6, "R_MIPS_LO16",            16}, \
  { &none,     7, "R_MIPS_GPREL16",         16}, \
  { &none,     8, "R_MIPS_LITERAL",         16}, \
  { &got16,    9, "R_MIPS_GOT16",           16}, \
  { &none,    10, "R_MIPS_PC16",            16}, \
  { &call16,  11, "R_MIPS_CALL16",          16}, \
  { &gprel32, 12, "R_MIPS_GPREL32",         32}, \
  { &none,    13, "R_MIPS_UNUSED1",          0}, \
  { &none,    14, "R_MIPS_UNUSED2",          0}, \
  { &none,    15, "R_MIPS_UNUSED3",          0}, \
  { &none,    16, "R_MIPS_SHIFT5",          32}, \
  { &none,    17, "R_MIPS_SHIFT6",          32}, \
  { &none,    18, "R_MIPS_64",              64}, \
  { &none,    19, "R_MIPS_GOT_DISP",        16}, \
  { &none,    20, "R_MIPS_GOT_PAGE",        16}, \
  { &none,    21, "R_MIPS_GOT_OFST",        16}, \
  { &gothi16, 22, "R_MIPS_GOT_HI16",        16}, \
  { &gotlo16, 23, "R_MIPS_GOT_LO16",        16}, \
  { &none,    24, "R_MIPS_SUB",             64}, \
  { &none,    25, "R_MIPS_INSERT_A",         0}, \
  { &none,    26, "R_MIPS_INSERT_B",         0}, \
  { &none,    27, "R_MIPS_DELETE",           0}, \
  { &none,    28, "R_MIPS_HIGHER",          16}, \
  { &none,    29, "R_MIPS_HIGHEST",         16}, \
  { &gothi16, 30, "R_MIPS_CALL_HI16",       16}, \
  { &gotlo16, 31, "R_MIPS_CALL_LO16",       16}, \
  { &none,    32, "R_MIPS_SCN_DISP",        32}, \
  { &none,    33, "R_MIPS_REL16",            0}, \
  { &none,    34, "R_MIPS_ADD_IMMEDIATE",    0}, \
  { &none,    35, "R_MIPS_PJUMP",            0}, \
  { &none,    36, "R_MIPS_RELGOT",           0}, \
  { &none,    37, "R_MIPS_JALR",            32}, \
  { &none,    38, "R_MIPS_TLS_DTPMOD32",    32}, \
  { &none,    39, "R_MIPS_TLS_DTPREL32",    32}, \
  { &none,    40, "R_MIPS_TLS_DTPMOD64",     0}, \
  { &none,    41, "R_MIPS_TLS_DTPREL64",     0}, \
  { &none,    42, "R_MIPS_TLS_GD",          16}, \
  { &none,    43, "R_MIPS_TLS_LDM",         16}, \
  { &none,    44, "R_MIPS_TLS_DTPREL_HI16", 16}, \
  { &none,    45, "R_MIPS_TLS_DTPREL_LO16", 16}, \
  { &none,    46, "R_MIPS_TLS_GOTTPREL",    16}, \
  { &none,    47, "R_MIPS_TLS_TPREL32",     32}, \
  { &none,    48, "R_MIPS_TLS_TPREL64",      0}, \
  { &none,    49, "R_MIPS_TLS_TPREL_HI16",  16}, \
  { &none,    50, "R_MIPS_TLS_TPREL_LO16",  16}, \
  { &none,    51, "R_MIPS_GLOB_DAT",         0}
