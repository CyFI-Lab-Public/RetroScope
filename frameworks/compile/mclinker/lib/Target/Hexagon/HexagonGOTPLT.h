//===- HexagonGOTPLT.h ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_HEXAGON_GOTPLT_H
#define MCLD_HEXAGON_GOTPLT_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <llvm/ADT/DenseMap.h>
#include "HexagonGOT.h"
#include <mcld/Support/MemoryRegion.h>

namespace mcld {

class HexagonPLT;
class LDSection;

// Hexagon creates 4 entries for the GOTPLT0 entry
const unsigned int HexagonGOTPLT0Num = 4;

/** \class HexagonGOTPLT
 *  \brief Hexagon .got.plt section.
 */
class HexagonGOTPLT : public HexagonGOT
{
public:
  HexagonGOTPLT(LDSection &pSection);

  ~HexagonGOTPLT();

  // hasGOT1 - return if this section has any GOT1 entry
  bool hasGOT1() const;

  void applyGOT0(uint64_t pAddress);

  void applyAllGOTPLT(const HexagonPLT& pPLT);
};

} // namespace of mcld

#endif
