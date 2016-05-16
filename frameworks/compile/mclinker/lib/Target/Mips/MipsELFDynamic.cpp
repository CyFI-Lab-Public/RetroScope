//===- MipsELFDynamic.cpp -------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "MipsELFDynamic.h"

#include "MipsLDBackend.h"
#include <mcld/LD/ELFFileFormat.h>
#include <mcld/Target/GNULDBackend.h>

using namespace mcld;

// MIPS mandatory dynamic section entries
enum {
  MIPS_RLD_VERSION  = 0x70000001,
  MIPS_FLAGS        = 0x70000005,
  MIPS_BASE_ADDRESS = 0x70000006,
  MIPS_LOCAL_GOTNO  = 0x7000000a,
  MIPS_SYMTABNO     = 0x70000011,
  MIPS_GOTSYM       = 0x70000013
};

MipsELFDynamic::MipsELFDynamic(const MipsGNULDBackend& pParent,
                               const LinkerConfig& pConfig)
  : ELFDynamic(pParent, pConfig),
    m_pParent(pParent)
{
}

MipsELFDynamic::~MipsELFDynamic()
{
}

void MipsELFDynamic::reserveTargetEntries(const ELFFileFormat& pFormat)
{
  // reservePLTGOT
  if (pFormat.hasGOT())
    reserveOne(llvm::ELF::DT_PLTGOT);

  reserveOne(MIPS_RLD_VERSION);
  reserveOne(MIPS_FLAGS);
  reserveOne(MIPS_BASE_ADDRESS);
  reserveOne(MIPS_LOCAL_GOTNO);
  reserveOne(MIPS_SYMTABNO);
  reserveOne(MIPS_GOTSYM);
}

void MipsELFDynamic::applyTargetEntries(const ELFFileFormat& pFormat)
{
  // applyPLTGOT
  if (pFormat.hasGOT())
    applyOne(llvm::ELF::DT_PLTGOT, pFormat.getGOT().addr());

  applyOne(MIPS_RLD_VERSION, 1);
  applyOne(MIPS_FLAGS, 0);
  applyOne(MIPS_BASE_ADDRESS, 0);
  applyOne(MIPS_LOCAL_GOTNO, getLocalGotNum(pFormat));
  applyOne(MIPS_SYMTABNO, getSymTabNum(pFormat));
  applyOne(MIPS_GOTSYM, getGotSym(pFormat));
}

size_t MipsELFDynamic::getSymTabNum(const ELFFileFormat& pFormat) const
{
  if (!pFormat.hasDynSymTab())
    return 0;

  const LDSection& dynsym = pFormat.getDynSymTab();
  return dynsym.size() / symbolSize();
}

size_t MipsELFDynamic::getGotSym(const ELFFileFormat& pFormat) const
{
  if (!pFormat.hasGOT())
    return 0;

  return getSymTabNum(pFormat) - m_pParent.getGOT().getGlobalNum();
}

size_t MipsELFDynamic::getLocalGotNum(const ELFFileFormat& pFormat) const
{
  if (!pFormat.hasGOT())
    return 0;

  return m_pParent.getGOT().getLocalNum();
}
