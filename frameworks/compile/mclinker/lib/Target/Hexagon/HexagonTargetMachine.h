//===- HexagonTargetMachine.h ---------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_HEXAGON_TARGET_MACHINE_H
#define MCLD_HEXAGON_TARGET_MACHINE_H
#include "Hexagon.h"
#include <mcld/Target/TargetMachine.h>

namespace mcld {

class HexagonTargetMachine : public MCLDTargetMachine
{
public:
  HexagonTargetMachine(llvm::TargetMachine &pTM,
                       const mcld::Target &pTarget,
                       const std::string &pTriple);

  virtual ~HexagonTargetMachine();
};

} // namespace of mcld

#endif
