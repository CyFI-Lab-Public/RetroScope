//===- TargetRegistry.cpp -------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Support/TargetRegistry.h>


mcld::TargetRegistry::TargetListTy mcld::TargetRegistry::s_TargetList;

void mcld::TargetRegistry::RegisterTarget(mcld::Target &T)
{
  s_TargetList.push_back(&T);
}

const mcld::Target*
mcld::TargetRegistry::lookupTarget(const llvm::Target &pTarget)
{
  mcld::Target *result = 0;
  TargetListTy::const_iterator TIter, TEnd = s_TargetList.end();
  for (TIter=s_TargetList.begin(); TIter!=TEnd; ++TIter) {
    if ((*TIter)->get()==&pTarget) {
      result = (*TIter);
      break;
    }
  }
  return result;
}

const mcld::Target*
mcld::TargetRegistry::lookupTarget(const std::string &pTriple,
                                   std::string &pError)
{
  const llvm::Target* target = llvm::TargetRegistry::lookupTarget(pTriple, pError);
  if (!target)
    return NULL;

  return lookupTarget( *target );
}

