//===- MipsDiagnostic.cpp -------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <llvm/ADT/Triple.h>
#include <mcld/Support/TargetRegistry.h>
#include <mcld/LD/DWARFLineInfo.h>
#include "Mips.h"

using namespace mcld;


namespace mcld {
//===----------------------------------------------------------------------===//
// createMipsDiagnostic - the help function to create corresponding
// MipsDiagnostic
DiagnosticLineInfo* createMipsDiagLineInfo(const mcld::Target& pTarget,
                                           const std::string &pTriple)
{
  return new DWARFLineInfo();
}

} // namespace of mcld

//==========================
// InitializeMipsDiagnostic
extern "C" void MCLDInitializeMipsDiagnosticLineInfo() {
  // Register the linker frontend
  mcld::TargetRegistry::RegisterDiagnosticLineInfo(TheMipselTarget, createMipsDiagLineInfo);
}

