//===- MipsTargetMachine.h ------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MIPS_TARGET_MACHINE_H
#define MIPS_TARGET_MACHINE_H

#include "Mips.h"
#include <mcld/Target/TargetMachine.h>

namespace mcld {

class MipsBaseTargetMachine : public MCLDTargetMachine
{
public:
  MipsBaseTargetMachine(llvm::TargetMachine &pTM,
                        const mcld::Target &pTarget,
                        const std::string &pTriple);

  virtual ~MipsBaseTargetMachine();
};

} // namespace of mcld

#endif
