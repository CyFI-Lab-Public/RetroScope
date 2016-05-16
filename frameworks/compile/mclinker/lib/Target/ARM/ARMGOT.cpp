//===- impl.cpp -----------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "ARMGOT.h"

#include <llvm/Support/Casting.h>

#include <mcld/LD/LDSection.h>
#include <mcld/LD/LDFileFormat.h>
#include <mcld/Support/MemoryRegion.h>
#include <mcld/Support/MsgHandling.h>

namespace {
  const unsigned int ARMGOT0Num = 3;
} // end of anonymous namespace

using namespace mcld;

//===----------------------------------------------------------------------===//
// ARMGOT
ARMGOT::ARMGOT(LDSection& pSection)
  : GOT(pSection), m_pLast(NULL)
{
  // Create GOT0 entries.
  reserve(ARMGOT0Num);

  // Skip GOT0 entries.
  for (unsigned int i = 0; i < ARMGOT0Num; ++i) {
    consume();
  }
}

ARMGOT::~ARMGOT()
{
}

bool ARMGOT::hasGOT1() const
{
  return (m_SectionData->size() > ARMGOT0Num);
}

void ARMGOT::reserve(size_t pNum)
{
  for (size_t i = 0; i < pNum; i++) {
    new ARMGOTEntry(0, m_SectionData);
  }
}

ARMGOTEntry* ARMGOT::consume()
{
  if (NULL == m_pLast) {
    assert(!empty() && "Consume empty GOT entry!");
    m_pLast = llvm::cast<ARMGOTEntry>(&m_SectionData->front());
    return m_pLast;
  }

  m_pLast = llvm::cast<ARMGOTEntry>(m_pLast->getNextNode());
  return m_pLast;
}

void ARMGOT::reserveGOTPLT()
{
  ARMGOTEntry* entry = new ARMGOTEntry(0, m_SectionData);
  if (NULL == m_GOTPLT.front) {
    // GOTPLT is empty
    if (NULL == m_GOT.front) {
      // GOT part is also empty. Since entry is the last entry, we can assign
      // it to GOTPLT directly.
      m_GOTPLT.front = entry;
    }
    else {
      // GOTn is not empty. Shift GOTn backward by one entry.
      m_GOTPLT.front = m_GOT.front;
      m_GOT.front = llvm::cast<ARMGOTEntry>(m_GOT.front->getNextNode());
    }
  }
  else {
    // GOTPLT is not empty
    if (NULL != m_GOT.front)
      m_GOT.front = llvm::cast<ARMGOTEntry>(m_GOT.front->getNextNode());
  }
}

void ARMGOT::reserveGOT()
{
  ARMGOTEntry* entry = new ARMGOTEntry(0, m_SectionData);
  if (NULL == m_GOT.front) {
    // Entry must be the last entry. We can directly assign it to GOT part.
    m_GOT.front = entry;
  }
}

ARMGOTEntry* ARMGOT::consumeGOTPLT()
{
  assert(NULL != m_GOTPLT.front && "Consuming empty GOTPLT section!");

  if (NULL == m_GOTPLT.last_used) {
    m_GOTPLT.last_used = m_GOTPLT.front;
  }
  else {
    m_GOTPLT.last_used = llvm::cast<ARMGOTEntry>(m_GOTPLT.last_used->getNextNode());
    assert(m_GOTPLT.last_used != m_GOT.front && "No GOT/PLT entry to consume!");
  }
  return m_GOTPLT.last_used;
}

ARMGOTEntry* ARMGOT::consumeGOT()
{
  assert(NULL != m_GOT.front && "Consuming empty GOT section!");

  if (NULL == m_GOT.last_used) {
    m_GOT.last_used = m_GOT.front;
  }
  else {
    m_GOT.last_used = llvm::cast<ARMGOTEntry>(m_GOT.last_used->getNextNode());
    assert(m_GOT.last_used != NULL && "No GOTn entry to consume!");
  }
  return m_GOT.last_used;
}

void ARMGOT::applyGOT0(uint64_t pAddress)
{
  llvm::cast<ARMGOTEntry>
    (*(m_SectionData->getFragmentList().begin())).setValue(pAddress);
}

void ARMGOT::applyGOTPLT(uint64_t pPLTBase)
{
  if (NULL == m_GOTPLT.front)
    return;

  SectionData::iterator entry(m_GOTPLT.front);
  SectionData::iterator e_end;
  if (NULL == m_GOT.front)
    e_end = m_SectionData->end();
  else
    e_end = SectionData::iterator(m_GOT.front);

  while (entry != e_end) {
    llvm::cast<ARMGOTEntry>(entry)->setValue(pPLTBase);
    ++entry;
  }
}

uint64_t ARMGOT::emit(MemoryRegion& pRegion)
{
  uint32_t* buffer = reinterpret_cast<uint32_t*>(pRegion.getBuffer());

  ARMGOTEntry* got = NULL;
  uint64_t result = 0x0;
  for (iterator it = begin(), ie = end(); it != ie; ++it, ++buffer) {
      got = &(llvm::cast<ARMGOTEntry>((*it)));
      *buffer = static_cast<uint32_t>(got->getValue());
      result += ARMGOTEntry::EntrySize;
  }
  return result;
}

