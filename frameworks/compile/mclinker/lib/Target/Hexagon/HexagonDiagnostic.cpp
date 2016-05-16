//===- HexagonDiagnostic.cpp ----------------------------------------------===//
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
#include "Hexagon.h"

using namespace mcld;

namespace mcld {

// createHexagonDiagnostic - the help function to create corresponding
// HexagonDiagnostic
//
DiagnosticLineInfo*
createHexagonDiagLineInfo(const Target& pTarget, const std::string &pTriple)
{
  return new DWARFLineInfo();
}

} // namespace of mcld

//===----------------------------------------------------------------------===//
// InitializeHexagonDiagnostic
//===----------------------------------------------------------------------===//
extern "C" void MCLDInitializeHexagonDiagnosticLineInfo() {
  // Register the linker frontend
  mcld::TargetRegistry::RegisterDiagnosticLineInfo(TheHexagonTarget,
                                                   createHexagonDiagLineInfo);
}

