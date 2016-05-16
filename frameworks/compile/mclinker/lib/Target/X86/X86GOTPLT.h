//===- X86GOTPLT.h --------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_X86_GOTPLT_H
#define MCLD_X86_GOTPLT_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <llvm/ADT/DenseMap.h>

#include "X86GOT.h"

namespace mcld {

class X86PLT;
class LDSection;

const unsigned int X86GOTPLT0Num = 3;

/** \class X86_32GOTPLT
 *  \brief X86_32 .got.plt section.
 */
class X86_32GOTPLT : public X86_32GOT
{
public:
  X86_32GOTPLT(LDSection &pSection);

  ~X86_32GOTPLT();

  // hasGOT1 - return if this section has any GOT1 entry
  bool hasGOT1() const;

  void applyGOT0(uint64_t pAddress);

  void applyAllGOTPLT(const X86PLT& pPLT);
};

/** \class X86_64GOTPLT
 *  \brief X86_64 .got.plt section.
 */
class X86_64GOTPLT : public X86_64GOT
{
public:
  X86_64GOTPLT(LDSection &pSection);

  ~X86_64GOTPLT();

  // hasGOT1 - return if this section has any GOT1 entry
  bool hasGOT1() const;

  void applyGOT0(uint64_t pAddress);

  void applyAllGOTPLT(const X86PLT& pPLT);
};

} // namespace of mcld

#endif

