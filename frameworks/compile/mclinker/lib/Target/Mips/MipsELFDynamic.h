//===- MipsELFDynamic.h ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_Mips_ELFDYNAMIC_SECTION_H
#define MCLD_Mips_ELFDYNAMIC_SECTION_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/Target/ELFDynamic.h>

namespace mcld {

class MipsGNULDBackend;

class MipsELFDynamic : public ELFDynamic
{
public:
  MipsELFDynamic(const MipsGNULDBackend& pParent, const LinkerConfig& pConfig);
  ~MipsELFDynamic();

private:
  const MipsGNULDBackend& m_pParent;

private:
  void reserveTargetEntries(const ELFFileFormat& pFormat);
  void applyTargetEntries(const ELFFileFormat& pFormat);

  size_t getSymTabNum(const ELFFileFormat& pFormat) const;
  size_t getGotSym(const ELFFileFormat& pFormat) const;
  size_t getLocalGotNum(const ELFFileFormat& pFormat) const;
};

} // namespace of mcld

#endif
