//===- HexagonTargetInfo.cpp -----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Target/TargetMachine.h>
#include <mcld/Support/TargetRegistry.h>

namespace mcld {

mcld::Target TheHexagonTarget;

extern "C" void MCLDInitializeHexagonLDTargetInfo() {
  // register into mcld::TargetRegistry
  mcld::RegisterTarget X(TheHexagonTarget, "hexagon");
}

} // namespace of mcld

