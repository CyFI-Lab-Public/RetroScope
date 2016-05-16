//===- MipsGNUInfo.h ------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_TARGET_MIPS_GNU_INFO_H
#define MCLD_TARGET_MIPS_GNU_INFO_H
#include <mcld/Target/GNUInfo.h>

#include <llvm/Support/ELF.h>

namespace mcld {

class MipsGNUInfo : public GNUInfo
{
public:
  enum {
    // The original o32 abi.
    E_MIPS_ABI_O32    = 0x00001000,
    // O32 extended to work on 64 bit architectures.
    E_MIPS_ABI_O64    = 0x00002000,
    // EABI in 32 bit mode.
    E_MIPS_ABI_EABI32 = 0x00003000,
    // EABI in 64 bit mode.
    E_MIPS_ABI_EABI64 = 0x00004000
  };

public:
  MipsGNUInfo(const llvm::Triple& pTriple) : GNUInfo(pTriple) { }

  uint32_t machine() const { return llvm::ELF::EM_MIPS; }

  uint64_t defaultTextSegmentAddr() const { return 0x80000; }

  uint64_t flags() const
  {
    // TODO: (simon) The correct flag's set depend on command line
    // arguments and flags from input .o files.
    return llvm::ELF::EF_MIPS_ARCH_32R2 |
           llvm::ELF::EF_MIPS_NOREORDER |
           llvm::ELF::EF_MIPS_PIC |
           llvm::ELF::EF_MIPS_CPIC |
           E_MIPS_ABI_O32;
  }

  const char* entry() const { return "__start"; }

  const char* dyld() const { return "/lib/ld.so.1"; }

  uint64_t abiPageSize() const { return 0x10000; }
};

} // namespace of mcld

#endif

