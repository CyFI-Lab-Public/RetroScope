//===- MipsTargetMachine.cpp ----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "MipsTargetMachine.h"

#include "Mips.h"
#include <mcld/Target/TargetMachine.h>
#include <mcld/Support/TargetRegistry.h>

extern "C" void MCLDInitializeMipsLDTarget() {
  // Register createTargetMachine function pointer to mcld::Target
  mcld::RegisterTargetMachine<mcld::MipsBaseTargetMachine>
        X(mcld::TheMipselTarget);
}

mcld::MipsBaseTargetMachine::MipsBaseTargetMachine(llvm::TargetMachine& pPM,
                                                   const mcld::Target &pTarget,
                                                   const std::string& pTriple)
  : mcld::MCLDTargetMachine(pPM, pTarget, pTriple) {
}

mcld::MipsBaseTargetMachine::~MipsBaseTargetMachine()
{
}
