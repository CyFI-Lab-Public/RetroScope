//===- ARMDiagnostic.cpp --------------------------------------------------===//
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
#include "ARM.h"

using namespace mcld;

//===----------------------------------------------------------------------===//
// ARMDiagnostic


namespace mcld {
//===----------------------------------------------------------------------===//
// createARMDiagnostic - the help function to create corresponding ARMDiagnostic
//
DiagnosticLineInfo* createARMDiagLineInfo(const mcld::Target& pTarget,
                                          const std::string &pTriple)
{
  return new DWARFLineInfo();
}

} // namespace of mcld

//==========================
// InitializeARMDiagnostic
extern "C" void MCLDInitializeARMDiagnosticLineInfo() {
  // Register the linker frontend
  mcld::TargetRegistry::RegisterDiagnosticLineInfo(TheARMTarget, createARMDiagLineInfo);
  mcld::TargetRegistry::RegisterDiagnosticLineInfo(TheThumbTarget, createARMDiagLineInfo);
}

