//===- ARMPLT.cpp -----------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "ARMGOT.h"
#include "ARMPLT.h"

#include <new>

#include <llvm/Support/Casting.h>

#include <mcld/LD/LDSection.h>
#include <mcld/Support/MemoryRegion.h>
#include <mcld/Support/MsgHandling.h>

using namespace mcld;

ARMPLT0::ARMPLT0(SectionData& pParent)
  : PLT::Entry<sizeof(arm_plt0)>(pParent) {}

ARMPLT1::ARMPLT1(SectionData& pParent)
  : PLT::Entry<sizeof(arm_plt1)>(pParent) {}

//===----------------------------------------------------------------------===//
// ARMPLT

ARMPLT::ARMPLT(LDSection& pSection,
               ARMGOT &pGOTPLT)
  : PLT(pSection), m_GOT(pGOTPLT), m_PLTEntryIterator() {
  new ARMPLT0(*m_SectionData);
  m_PLTEntryIterator = m_SectionData->begin();
}

ARMPLT::~ARMPLT()
{
}

bool ARMPLT::hasPLT1() const
{
  return (m_SectionData->size() > 1);
}

void ARMPLT::finalizeSectionSize()
{
  uint64_t size = (m_SectionData->size() - 1) * sizeof(arm_plt1) +
                     sizeof(arm_plt0);
  m_Section.setSize(size);

  uint32_t offset = 0;
  SectionData::iterator frag, fragEnd = m_SectionData->end();
  for (frag = m_SectionData->begin(); frag != fragEnd; ++frag) {
    frag->setOffset(offset);
    offset += frag->size();
  }
}

void ARMPLT::reserveEntry(size_t pNum)
{
  ARMPLT1* plt1_entry = 0;

  for (size_t i = 0; i < pNum; ++i) {
    plt1_entry = new (std::nothrow) ARMPLT1(*m_SectionData);

    if (!plt1_entry)
      fatal(diag::fail_allocate_memory_plt);

    m_GOT.reserveGOTPLT();
  }
}

ARMPLT1* ARMPLT::consume()
{
  ++m_PLTEntryIterator;
  assert(m_PLTEntryIterator != m_SectionData->end() &&
         "The number of PLT Entries and ResolveInfo doesn't match");

  return llvm::cast<ARMPLT1>(&(*m_PLTEntryIterator));
}

ARMPLT0* ARMPLT::getPLT0() const {

  iterator first = m_SectionData->getFragmentList().begin();

  assert(first != m_SectionData->getFragmentList().end() &&
         "FragmentList is empty, getPLT0 failed!");

  ARMPLT0* plt0 = &(llvm::cast<ARMPLT0>(*first));

  return plt0;
}

void ARMPLT::applyPLT0() {

  uint64_t plt_base = m_Section.addr();
  assert(plt_base && ".plt base address is NULL!");

  uint64_t got_base = m_GOT.addr();
  assert(got_base && ".got base address is NULL!");

  uint32_t offset = 0;

  if (got_base > plt_base)
    offset = got_base - (plt_base + 16);
  else
    offset = (plt_base + 16) - got_base;

  iterator first = m_SectionData->getFragmentList().begin();

  assert(first != m_SectionData->getFragmentList().end() &&
         "FragmentList is empty, applyPLT0 failed!");

  ARMPLT0* plt0 = &(llvm::cast<ARMPLT0>(*first));

  uint32_t* data = 0;
  data = static_cast<uint32_t*>(malloc(ARMPLT0::EntrySize));

  if (!data)
    fatal(diag::fail_allocate_memory_plt);

  memcpy(data, arm_plt0, ARMPLT0::EntrySize);
  data[4] = offset;

  plt0->setValue(reinterpret_cast<unsigned char*>(data));
}

void ARMPLT::applyPLT1() {

  uint64_t plt_base = m_Section.addr();
  assert(plt_base && ".plt base address is NULL!");

  uint64_t got_base = m_GOT.addr();
  assert(got_base && ".got base address is NULL!");

  ARMPLT::iterator it = m_SectionData->begin();
  ARMPLT::iterator ie = m_SectionData->end();
  assert(it != ie && "FragmentList is empty, applyPLT1 failed!");

  uint32_t GOTEntrySize = ARMGOTEntry::EntrySize;
  uint32_t GOTEntryAddress =
    got_base +  GOTEntrySize * 3;

  uint64_t PLTEntryAddress =
    plt_base + ARMPLT0::EntrySize; //Offset of PLT0

  ++it; //skip PLT0
  uint64_t PLT1EntrySize = ARMPLT1::EntrySize;
  ARMPLT1* plt1 = NULL;

  uint32_t* Out = NULL;
  while (it != ie) {
    plt1 = &(llvm::cast<ARMPLT1>(*it));
    Out = static_cast<uint32_t*>(malloc(ARMPLT1::EntrySize));

    if (!Out)
      fatal(diag::fail_allocate_memory_plt);

    // Offset is the distance between the last PLT entry and the associated
    // GOT entry.
    int32_t Offset = (GOTEntryAddress - (PLTEntryAddress + 8));

    Out[0] = arm_plt1[0] | ((Offset >> 20) & 0xFF);
    Out[1] = arm_plt1[1] | ((Offset >> 12) & 0xFF);
    Out[2] = arm_plt1[2] | (Offset & 0xFFF);

    plt1->setValue(reinterpret_cast<unsigned char*>(Out));
    ++it;

    GOTEntryAddress += GOTEntrySize;
    PLTEntryAddress += PLT1EntrySize;
  }

  m_GOT.applyGOTPLT(plt_base);
}

uint64_t ARMPLT::emit(MemoryRegion& pRegion)
{
  uint64_t result = 0x0;
  iterator it = begin();

  unsigned char* buffer = pRegion.getBuffer();
  memcpy(buffer, llvm::cast<ARMPLT0>((*it)).getValue(), ARMPLT0::EntrySize);
  result += ARMPLT0::EntrySize;
  ++it;

  ARMPLT1* plt1 = 0;
  ARMPLT::iterator ie = end();
  while (it != ie) {
    plt1 = &(llvm::cast<ARMPLT1>(*it));
    memcpy(buffer + result, plt1->getValue(), ARMPLT1::EntrySize);
    result += ARMPLT1::EntrySize;
    ++it;
  }
  return result;
}

