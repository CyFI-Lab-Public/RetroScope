//===- EhFrame.cpp --------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/LD/EhFrame.h>
#include <mcld/LD/LDSection.h>
#include <mcld/LD/SectionData.h>
#include <mcld/Object/ObjectBuilder.h>
#include <mcld/Support/MemoryRegion.h>
#include <mcld/Support/GCFactory.h>

#include <llvm/Support/ManagedStatic.h>

using namespace mcld;

typedef GCFactory<EhFrame, MCLD_SECTIONS_PER_INPUT> EhFrameFactory;

static llvm::ManagedStatic<EhFrameFactory> g_EhFrameFactory;

//===----------------------------------------------------------------------===//
// EhFrame::CIE
//===----------------------------------------------------------------------===//
EhFrame::CIE::CIE(MemoryRegion& pRegion)
  : RegionFragment(pRegion) {
}

//===----------------------------------------------------------------------===//
// EhFrame::FDE
//===----------------------------------------------------------------------===//
EhFrame::FDE::FDE(MemoryRegion& pRegion,
                  const EhFrame::CIE& pCIE,
                  uint32_t pDataStart)
  : RegionFragment(pRegion),
    m_CIE(pCIE),
    m_DataStart(pDataStart) {
}

//===----------------------------------------------------------------------===//
// EhFrame
//===----------------------------------------------------------------------===//
EhFrame::EhFrame()
  : m_pSection(NULL), m_pSectionData(NULL) {
}

EhFrame::EhFrame(LDSection& pSection)
  : m_pSection(&pSection),
    m_pSectionData(NULL) {
  m_pSectionData = SectionData::Create(pSection);
}

EhFrame::~EhFrame()
{
  // Since all CIEs, FDEs and regular fragments are stored in iplist, iplist
  // will delete the fragments and we do not need to handle with it.
}

EhFrame* EhFrame::Create(LDSection& pSection)
{
  EhFrame* result = g_EhFrameFactory->allocate();
  new (result) EhFrame(pSection);
  return result;
}

void EhFrame::Destroy(EhFrame*& pSection)
{
  pSection->~EhFrame();
  g_EhFrameFactory->deallocate(pSection);
  pSection = NULL;
}

void EhFrame::Clear()
{
  g_EhFrameFactory->clear();
}

const LDSection& EhFrame::getSection() const
{
  assert(NULL != m_pSection);
  return *m_pSection;
}

LDSection& EhFrame::getSection()
{
  assert(NULL != m_pSection);
  return *m_pSection;
}

void EhFrame::addFragment(RegionFragment& pFrag)
{
  uint32_t offset = 0;
  if (!m_pSectionData->empty())
    offset = m_pSectionData->back().getOffset() + m_pSectionData->back().size();

  m_pSectionData->getFragmentList().push_back(&pFrag);
  pFrag.setOffset(offset);
}

void EhFrame::addFragment(NullFragment& pFrag)
{
  uint32_t offset = 0;
  if (!m_pSectionData->empty())
    offset = m_pSectionData->back().getOffset() + m_pSectionData->back().size();

  m_pSectionData->getFragmentList().push_back(&pFrag);
  pFrag.setOffset(offset);
}

void EhFrame::addCIE(EhFrame::CIE& pCIE)
{
  m_CIEs.push_back(&pCIE);
  addFragment(pCIE);
}

void EhFrame::addFDE(EhFrame::FDE& pFDE)
{
  m_FDEs.push_back(&pFDE);
  addFragment(pFDE);
}

EhFrame& EhFrame::merge(EhFrame& pOther)
{
  ObjectBuilder::MoveSectionData(*pOther.getSectionData(), *m_pSectionData);

  m_CIEs.reserve(pOther.numOfCIEs() + m_CIEs.size());
  for (cie_iterator cie = pOther.cie_begin(); cie != pOther.cie_end(); ++cie)
    m_CIEs.push_back(*cie);

  m_FDEs.reserve(pOther.numOfFDEs() + m_FDEs.size());
  for (fde_iterator fde = pOther.fde_begin(); fde != pOther.fde_end(); ++fde)
    m_FDEs.push_back(*fde);

  pOther.m_CIEs.clear();
  pOther.m_FDEs.clear();
  return *this;
}

