//===- X86Diagnostic.cpp --------------------------------------------------===//
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
#include "X86.h"

using namespace mcld;

//===----------------------------------------------------------------------===//
// X86Diagnostic


namespace mcld {
//===----------------------------------------------------------------------===//
// createX86Diagnostic - the help function to create corresponding X86Diagnostic
//
DiagnosticLineInfo* createX86DiagLineInfo(const mcld::Target& pTarget,
                                          const std::string &pTriple)
{
  return new DWARFLineInfo();
}

} // namespace of mcld

//==========================
// InitializeX86Diagnostic
extern "C" void MCLDInitializeX86DiagnosticLineInfo() {
  // Register the linker frontend
  mcld::TargetRegistry::RegisterDiagnosticLineInfo(TheX86_32Target, createX86DiagLineInfo);
  mcld::TargetRegistry::RegisterDiagnosticLineInfo(TheX86_64Target, createX86DiagLineInfo);
}

