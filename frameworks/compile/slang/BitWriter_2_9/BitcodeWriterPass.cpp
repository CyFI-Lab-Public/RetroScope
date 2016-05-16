//===--- Bitcode/Writer/BitcodeWriterPass.cpp - Bitcode Writer ------------===//
//
//                     The LLVM Compiler Infrastructure
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
//
// BitcodeWriterPass implementation.
//
//===----------------------------------------------------------------------===//

#include "ReaderWriter_2_9.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/Instructions.h"
#include "llvm/IR/Module.h"
#include "llvm/Pass.h"
using namespace llvm;

namespace {
  class WriteBitcodePass : public ModulePass {
    raw_ostream &OS; // raw_ostream to print on

    bool expandCaseRange(Function &F);
  public:
    static char ID; // Pass identification, replacement for typeid
    explicit WriteBitcodePass(raw_ostream &o)
      : ModulePass(ID), OS(o) {}

    const char *getPassName() const { return "Bitcode Writer"; }

    bool runOnModule(Module &M) {
      bool Changed = false;
      for (Module::iterator F = M.begin(), E = M.end(); F != E; ++F)
        if (!F->isDeclaration())
          Changed |= expandCaseRange(*F);

      llvm_2_9::WriteBitcodeToFile(&M, OS);
      return Changed;
    }
  };
}

char WriteBitcodePass::ID = 0;

/// expandCaseRange - Expand case range into explicit case values within the
/// range
bool WriteBitcodePass::expandCaseRange(Function &F) {
  bool Changed = false;

  for (Function::iterator BB = F.begin(), E = F.end(); BB != E; ++BB) {
    SwitchInst *SI = dyn_cast<SwitchInst>(BB->getTerminator());
    if (SI == NULL) {
      continue;
    }

    for (SwitchInst::CaseIt i = SI->case_begin(), e = SI->case_end();
         i != e; ++i) {
      IntegersSubset& CaseRanges = i.getCaseValueEx();

      // All case ranges are already in single case values
      if (CaseRanges.isSingleNumbersOnly()) {
        continue;
      }

      // Create a new case
      Type *IntTy = SI->getCondition()->getType();
      IntegersSubsetToBB CaseBuilder;
      Changed = true;

      for (unsigned ri = 0, rn = CaseRanges.getNumItems(); ri != rn; ++ri) {
        IntegersSubset::Range r = CaseRanges.getItem(ri);
        bool IsSingleNumber = CaseRanges.isSingleNumber(ri);

        if (IsSingleNumber) {
          CaseBuilder.add(r);
        } else {
          const APInt &Low = r.getLow();
          const APInt &High = r.getHigh();

          for (APInt V = Low; V != High; V++) {
            assert(r.isInRange(V) && "Unexpected out-of-range case value!");
            CaseBuilder.add(IntItem::fromType(IntTy, V));
          }
        }

        IntegersSubset Case = CaseBuilder.getCase();
        i.setValueEx(Case);
      }
    }
  }
  return Changed;
}

/// createBitcodeWriterPass - Create and return a pass that writes the module
/// to the specified ostream.
llvm::ModulePass *llvm_2_9::createBitcodeWriterPass(llvm::raw_ostream &Str) {
  return new WriteBitcodePass(Str);
}
