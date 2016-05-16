//===- HexagonTargetMachine.cpp -------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "HexagonTargetMachine.h"
#include "Hexagon.h"
#include <mcld/Target/TargetMachine.h>
#include <mcld/Support/TargetRegistry.h>

extern "C" void MCLDInitializeHexagonLDTarget() {
  // Register createTargetMachine function pointer to mcld::Target
  mcld::RegisterTargetMachine<mcld::HexagonTargetMachine>
     X(mcld::TheHexagonTarget);
}

mcld::HexagonTargetMachine::HexagonTargetMachine(llvm::TargetMachine& pPM,
                                                 const mcld::Target &pTarget,
                                                 const std::string& pTriple)
  : mcld::MCLDTargetMachine(pPM, pTarget, pTriple) {
}

mcld::HexagonTargetMachine::~HexagonTargetMachine()
{
}

