//===- X86TargetMachine.cpp -----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "X86TargetMachine.h"

#include "X86.h"
#include <mcld/Target/TargetMachine.h>
#include <mcld/Support/TargetRegistry.h>

extern "C" void MCLDInitializeX86LDTarget() {
  // Register createTargetMachine function pointer to mcld::Target
  mcld::RegisterTargetMachine<mcld::X86TargetMachine> X(mcld::TheX86_32Target);
  mcld::RegisterTargetMachine<mcld::X86TargetMachine> Y(mcld::TheX86_64Target);
}

mcld::X86TargetMachine::X86TargetMachine(llvm::TargetMachine& pPM,
                                         const mcld::Target &pTarget,
                                         const std::string& pTriple)
  : mcld::MCLDTargetMachine(pPM, pTarget, pTriple) {
}

mcld::X86TargetMachine::~X86TargetMachine()
{
}

