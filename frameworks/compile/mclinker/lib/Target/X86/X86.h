//===- X86.h --------------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_X86_H
#define MCLD_X86_H
#include <string>
#include "mcld/Target/TargetMachine.h"

namespace mcld {
class TargetLDBackend;

extern mcld::Target TheX86_32Target;
extern mcld::Target TheX86_64Target;

TargetLDBackend *createX86LDBackend(const llvm::Target&, const std::string&);

} // namespace of mcld

#endif

