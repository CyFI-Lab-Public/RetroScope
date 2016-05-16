//===- ARMTargetMachine.h -------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_ARM_TARGET_MACHINE_H
#define MCLD_ARM_TARGET_MACHINE_H

#include "ARM.h"
#include <mcld/Target/TargetMachine.h>

namespace mcld {

class ARMBaseTargetMachine : public MCLDTargetMachine
{
public:
  ARMBaseTargetMachine(llvm::TargetMachine &pTM,
                       const mcld::Target &pTarget,
                       const std::string &pTriple);

  virtual ~ARMBaseTargetMachine();
};

} // namespace of mcld

#endif

