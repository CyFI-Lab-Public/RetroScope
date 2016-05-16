//===- Target.cpp ---------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Support/TargetRegistry.h>
#include <mcld/Target/TargetMachine.h>
#include <llvm/Support/TargetRegistry.h>
#include <llvm/Target/TargetMachine.h>

using namespace llvm;

mcld::Target::Target()
  : TargetMachineCtorFn(NULL),
    MCLinkerCtorFn(NULL),
    TargetLDBackendCtorFn(NULL),
    DiagnosticLineInfoCtorFn(NULL),
    m_pT(NULL)
{
}

