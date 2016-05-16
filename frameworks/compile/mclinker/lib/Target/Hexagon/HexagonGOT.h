//===- HexagonGOT.h -------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_TARGET_HEXAGON_GOT_H
#define MCLD_TARGET_HEXAGON_GOT_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/Target/GOT.h>

namespace mcld {

class LDSection;
class SectionData;

/** \class HexagonGOTEntry
 *  \brief GOT Entry with size of 4 bytes
 */
class HexagonGOTEntry : public GOT::Entry<4>
{
public:
  HexagonGOTEntry(uint64_t pContent, SectionData* pParent)
   : GOT::Entry<4>(pContent, pParent)
  {}
};

/** \class HexagonGOT
 *  \brief Hexagon Global Offset Table.
 */

class HexagonGOT : public GOT
{
public:
  HexagonGOT(LDSection& pSection);

  ~HexagonGOT();

  void reserve(size_t pNum = 1);

  HexagonGOTEntry* consume();

private:
  HexagonGOTEntry* m_pLast; ///< the last consumed entry
};

} // namespace of mcld

#endif

