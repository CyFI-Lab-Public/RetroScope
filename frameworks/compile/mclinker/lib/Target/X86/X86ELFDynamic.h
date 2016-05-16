//===- X86ELFDynamic.h ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_X86_ELFDYNAMIC_SECTION_H
#define MCLD_X86_ELFDYNAMIC_SECTION_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/Target/ELFDynamic.h>

namespace mcld {

class X86ELFDynamic : public ELFDynamic
{
public:
  X86ELFDynamic(const GNULDBackend& pParent, const LinkerConfig& pConfig);
  ~X86ELFDynamic();

private:
  void reserveTargetEntries(const ELFFileFormat& pFormat);
  void applyTargetEntries(const ELFFileFormat& pFormat);
};

} // namespace of mcld

#endif
