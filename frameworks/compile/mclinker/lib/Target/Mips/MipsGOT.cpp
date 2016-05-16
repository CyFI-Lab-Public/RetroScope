//===- MipsGOT.cpp --------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//

#include <llvm/Support/Casting.h>
#include <llvm/Support/ELF.h>

#include <mcld/LD/ResolveInfo.h>
#include <mcld/Support/MemoryRegion.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/Target/OutputRelocSection.h>

#include "MipsGOT.h"
#include "MipsRelocator.h"

namespace {
  const size_t MipsGOT0Num = 1;
  const size_t MipsGOTGpOffset = 0x7FF0;
  const size_t MipsGOTSize = MipsGOTGpOffset + 0x7FFF;
}

using namespace mcld;

//===----------------------------------------------------------------------===//
// MipsGOTEntry
//===----------------------------------------------------------------------===//
MipsGOTEntry::MipsGOTEntry(uint64_t pContent, SectionData* pParent)
   : GOT::Entry<4>(pContent, pParent)
{}

//===----------------------------------------------------------------------===//
// MipsGOT::GOTMultipart
//===----------------------------------------------------------------------===//
MipsGOT::GOTMultipart::GOTMultipart(size_t local, size_t global)
  : m_LocalNum(local),
    m_GlobalNum(global),
    m_ConsumedLocal(0),
    m_ConsumedGlobal(0),
    m_pLastLocal(NULL),
    m_pLastGlobal(NULL)
{
}

bool MipsGOT::GOTMultipart::isConsumed() const
{
  return m_LocalNum == m_ConsumedLocal &&
         m_GlobalNum == m_ConsumedGlobal;
}

void MipsGOT::GOTMultipart::consumeLocal()
{
  assert(m_ConsumedLocal < m_LocalNum &&
         "Consumed too many local GOT entries");
  ++m_ConsumedLocal;
  m_pLastLocal = llvm::cast<MipsGOTEntry>(m_pLastLocal->getNextNode());
}

void MipsGOT::GOTMultipart::consumeGlobal()
{
  assert(m_ConsumedGlobal < m_GlobalNum &&
         "Consumed too many global GOT entries");
  ++m_ConsumedGlobal;
  m_pLastGlobal = llvm::cast<MipsGOTEntry>(m_pLastGlobal->getNextNode());
}

//===----------------------------------------------------------------------===//
// MipsGOT
//===----------------------------------------------------------------------===//
MipsGOT::MipsGOT(LDSection& pSection)
  : GOT(pSection),
    m_pInput(NULL),
    m_CurrentGOTPart(0)
{
}

SizeTraits<32>::Address MipsGOT::getGPDispAddress() const
{
  return addr() + MipsGOTGpOffset;
}

void MipsGOT::reserve(size_t pNum)
{
  for (size_t i = 0; i < pNum; i++) {
    new MipsGOTEntry(0, m_SectionData);
  }
}

bool MipsGOT::hasGOT1() const
{
  return !m_MultipartList.empty();
}

bool MipsGOT::hasMultipleGOT() const
{
  return m_MultipartList.size() > 1;
}

void MipsGOT::finalizeScanning(OutputRelocSection& pRelDyn)
{
  for (MultipartListType::iterator it = m_MultipartList.begin();
       it != m_MultipartList.end(); ++it) {
    reserve(MipsGOT0Num);
    it->m_pLastLocal = llvm::cast<MipsGOTEntry>(&m_SectionData->back());
    reserve(it->m_LocalNum);
    it->m_pLastGlobal = llvm::cast<MipsGOTEntry>(&m_SectionData->back());
    reserve(it->m_GlobalNum);

    if (it == m_MultipartList.begin())
      // Reserve entries in the second part of the primary GOT.
      // These entries correspond to the global symbols in all
      // non-primary GOTs.
      reserve(getGlobalNum() - it->m_GlobalNum);
    else {
      // Reserve reldyn entries for R_MIPS_REL32 relocations
      // for all global entries of secondary GOTs.
      // FIXME: (simon) Do not count local entries for non-pic.
      size_t count = it->m_GlobalNum + it->m_LocalNum;
      for (size_t i = 0; i < count; ++i)
        pRelDyn.reserveEntry();
    }
  }
}

bool MipsGOT::dynSymOrderCompare(const LDSymbol* pX, const LDSymbol* pY) const
{
  SymbolOrderMapType::const_iterator itX = m_SymbolOrderMap.find(pX);
  SymbolOrderMapType::const_iterator itY = m_SymbolOrderMap.find(pY);

  if (itX != m_SymbolOrderMap.end() && itY != m_SymbolOrderMap.end())
    return itX->second < itY->second;

  return itX == m_SymbolOrderMap.end() && itY != m_SymbolOrderMap.end();
}

uint64_t MipsGOT::emit(MemoryRegion& pRegion)
{
  uint32_t* buffer = reinterpret_cast<uint32_t*>(pRegion.getBuffer());

  uint64_t result = 0;
  for (iterator it = begin(), ie = end();
       it != ie; ++it, ++buffer) {
    MipsGOTEntry* got = &(llvm::cast<MipsGOTEntry>((*it)));
    *buffer = static_cast<uint32_t>(got->getValue());
    result += got->size();
  }
  return result;
}

void MipsGOT::initGOTList()
{
  m_SymbolOrderMap.clear();

  m_MultipartList.clear();
  m_MultipartList.push_back(GOTMultipart());

  m_MultipartList.back().m_Inputs.insert(m_pInput);

  m_MergedGlobalSymbols.clear();
  m_InputGlobalSymbols.clear();
  m_MergedLocalSymbols.clear();
  m_InputLocalSymbols.clear();
}

void MipsGOT::changeInput()
{
  m_MultipartList.back().m_Inputs.insert(m_pInput);

  for (SymbolSetType::iterator it = m_InputLocalSymbols.begin(),
                               end = m_InputLocalSymbols.end();
       it != end; ++it)
    m_MergedLocalSymbols.insert(*it);

  m_InputLocalSymbols.clear();

  for (SymbolUniqueMapType::iterator it = m_InputGlobalSymbols.begin(),
                                     end = m_InputGlobalSymbols.end();
       it != end; ++it)
    m_MergedGlobalSymbols.insert(it->first);

  m_InputGlobalSymbols.clear();
}

bool MipsGOT::isGOTFull() const
{
  uint64_t gotCount = MipsGOT0Num +
                      m_MultipartList.back().m_LocalNum +
                      m_MultipartList.back().m_GlobalNum;

  gotCount += 1;

  return (gotCount * mcld::MipsGOTEntry::EntrySize) > MipsGOTSize;
}

void MipsGOT::split()
{
  m_MergedLocalSymbols.clear();
  m_MergedGlobalSymbols.clear();

  size_t uniqueCount = 0;
  for (SymbolUniqueMapType::const_iterator it = m_InputGlobalSymbols.begin(),
                                           end = m_InputGlobalSymbols.end();
       it != end; ++it) {
    if (it->second)
      ++uniqueCount;
  }

  m_MultipartList.back().m_LocalNum -= m_InputLocalSymbols.size();
  m_MultipartList.back().m_GlobalNum -= uniqueCount;
  m_MultipartList.back().m_Inputs.erase(m_pInput);

  m_MultipartList.push_back(GOTMultipart(m_InputLocalSymbols.size(),
                                         m_InputGlobalSymbols.size()));
  m_MultipartList.back().m_Inputs.insert(m_pInput);
}

void MipsGOT::initializeScan(const Input& pInput)
{
  if (m_pInput == NULL) {
    m_pInput = &pInput;
    initGOTList();
  }
  else {
    m_pInput = &pInput;
    changeInput();
  }
}

void MipsGOT::finalizeScan(const Input& pInput)
{
}

bool MipsGOT::reserveLocalEntry(ResolveInfo& pInfo)
{
  if (pInfo.type() != ResolveInfo::Section) {
    if (m_InputLocalSymbols.count(&pInfo))
      return false;

    if (m_MergedLocalSymbols.count(&pInfo)) {
      m_InputLocalSymbols.insert(&pInfo);
      return false;
    }
  }

  if (isGOTFull())
    split();

  if (pInfo.type() != ResolveInfo::Section)
    m_InputLocalSymbols.insert(&pInfo);

  ++m_MultipartList.back().m_LocalNum;
  return true;
}

bool MipsGOT::reserveGlobalEntry(ResolveInfo& pInfo)
{
  if (m_InputGlobalSymbols.count(&pInfo))
    return false;

  if (m_MergedGlobalSymbols.count(&pInfo)) {
    m_InputGlobalSymbols[&pInfo] = false;
    return false;
  }

  if (isGOTFull())
    split();

  m_InputGlobalSymbols[&pInfo] = true;
  ++m_MultipartList.back().m_GlobalNum;

  if (!(pInfo.reserved() & MipsRelocator::ReserveGot)) {
    m_SymbolOrderMap[pInfo.outSymbol()] = m_SymbolOrderMap.size();
    pInfo.setReserved(pInfo.reserved() | MipsRelocator::ReserveGot);
  }

  return true;
}

bool MipsGOT::isPrimaryGOTConsumed()
{
  return m_CurrentGOTPart > 0;
}

MipsGOTEntry* MipsGOT::consumeLocal()
{
  assert(m_CurrentGOTPart < m_MultipartList.size() && "GOT number is out of range!");

  if (m_MultipartList[m_CurrentGOTPart].isConsumed())
    ++m_CurrentGOTPart;

  m_MultipartList[m_CurrentGOTPart].consumeLocal();

  return m_MultipartList[m_CurrentGOTPart].m_pLastLocal;
}

MipsGOTEntry* MipsGOT::consumeGlobal()
{
  assert(m_CurrentGOTPart < m_MultipartList.size() && "GOT number is out of range!");

  if (m_MultipartList[m_CurrentGOTPart].isConsumed())
    ++m_CurrentGOTPart;

  m_MultipartList[m_CurrentGOTPart].consumeGlobal();

  return m_MultipartList[m_CurrentGOTPart].m_pLastGlobal;
}

SizeTraits<32>::Address MipsGOT::getGPAddr(const Input& pInput) const
{
  uint64_t gotSize = 0;
  for (MultipartListType::const_iterator it = m_MultipartList.begin();
                                         it != m_MultipartList.end(); ++it) {
    if (it->m_Inputs.count(&pInput))
      break;

    gotSize += (MipsGOT0Num + it->m_LocalNum + it->m_GlobalNum);
    if (it == m_MultipartList.begin())
      gotSize += getGlobalNum() - it->m_GlobalNum;
  }

  return addr() + gotSize * MipsGOTEntry::EntrySize + MipsGOTGpOffset;
}

SizeTraits<32>::Offset MipsGOT::getGPRelOffset(const Input& pInput,
                                               const MipsGOTEntry& pEntry) const
{
  SizeTraits<32>::Address gpAddr = getGPAddr(pInput);
  return addr() + pEntry.getOffset() - gpAddr;
}

void MipsGOT::recordEntry(const ResolveInfo* pInfo, MipsGOTEntry* pEntry)
{
  GotEntryKey key;
  key.m_GOTPage = m_CurrentGOTPart;
  key.m_pInfo = pInfo;
  m_GotEntriesMap[key] = pEntry;
}

MipsGOTEntry* MipsGOT::lookupEntry(const ResolveInfo* pInfo)
{
  GotEntryKey key;
  key.m_GOTPage= m_CurrentGOTPart;
  key.m_pInfo = pInfo;
  GotEntryMapType::iterator it = m_GotEntriesMap.find(key);

  if (it == m_GotEntriesMap.end())
    return NULL;

  return it->second;
}

size_t MipsGOT::getLocalNum() const
{
  assert(!m_MultipartList.empty() && "GOT is empty!");
  return m_MultipartList[0].m_LocalNum + MipsGOT0Num;
}

size_t MipsGOT::getGlobalNum() const
{
  return m_SymbolOrderMap.size();
}
