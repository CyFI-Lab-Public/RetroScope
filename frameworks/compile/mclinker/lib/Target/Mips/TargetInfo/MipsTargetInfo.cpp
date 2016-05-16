//===- MipsTargetInfo.cpp -------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "mcld/Target/TargetMachine.h"
#include "mcld/Support/TargetRegistry.h"

namespace mcld {

mcld::Target TheMipselTarget;

extern "C" void MCLDInitializeMipsLDTargetInfo() {
  // register into mcld::TargetRegistry
  mcld::RegisterTarget X(TheMipselTarget, "mipsel");
}

} // namespace of mcld
