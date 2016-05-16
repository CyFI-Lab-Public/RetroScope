//===- HexagonGOT.cpp -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "HexagonGOT.h"

#include <mcld/LD/LDFileFormat.h>
#include <mcld/LD/SectionData.h>

#include <llvm/Support/Casting.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// HexagonGOT
//===----------------------------------------------------------------------===//
HexagonGOT::HexagonGOT(LDSection& pSection)
  : GOT(pSection), m_pLast(NULL)
{
}

HexagonGOT::~HexagonGOT()
{
}

void HexagonGOT::reserve(size_t pNum)
{
  for (size_t i = 0; i < pNum; i++) {
    new HexagonGOTEntry(0, m_SectionData);
  }
}

HexagonGOTEntry* HexagonGOT::consume()
{
  if (NULL == m_pLast) {
    assert(!empty() && "Consume empty GOT entry!");
    m_pLast = llvm::cast<HexagonGOTEntry>(&m_SectionData->front());
    return m_pLast;
  }

  m_pLast = llvm::cast<HexagonGOTEntry>(m_pLast->getNextNode());
  return m_pLast;
}

