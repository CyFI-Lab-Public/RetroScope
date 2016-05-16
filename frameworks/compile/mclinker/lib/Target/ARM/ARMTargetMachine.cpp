//===- ARMTargetMachine.cpp -----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "ARMTargetMachine.h"
#include "ARM.h"

#include <mcld/Support/TargetRegistry.h>

mcld::ARMBaseTargetMachine::ARMBaseTargetMachine(llvm::TargetMachine& pPM,
                                                 const mcld::Target &pTarget,
                                                 const std::string& pTriple)
  : mcld::MCLDTargetMachine(pPM, pTarget, pTriple) {
}

mcld::ARMBaseTargetMachine::~ARMBaseTargetMachine()
{
}

//===----------------------------------------------------------------------===//
// Initialize MCLDTargetMachine
//===----------------------------------------------------------------------===//
extern "C" void MCLDInitializeARMLDTarget() {
  // Register createTargetMachine function pointer to mcld::Target
  mcld::RegisterTargetMachine<mcld::ARMBaseTargetMachine> X(mcld::TheARMTarget);
  mcld::RegisterTargetMachine<mcld::ARMBaseTargetMachine> Y(mcld::TheThumbTarget);
}

