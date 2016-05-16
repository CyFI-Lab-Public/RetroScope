//===- GOT.cpp ------------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <llvm/Support/Casting.h>

#include <mcld/LD/LDSection.h>
#include <mcld/Target/GOT.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/IRBuilder.h>

#include <cstring>
#include <cstdlib>

using namespace mcld;

//===----------------------------------------------------------------------===//
// GOT
//===----------------------------------------------------------------------===//
GOT::GOT(LDSection& pSection)
  : m_Section(pSection) {
  m_SectionData = IRBuilder::CreateSectionData(pSection);
}

GOT::~GOT()
{
}

void GOT::finalizeSectionSize()
{
  uint32_t offset = 0;
  SectionData::iterator frag, fragEnd = m_SectionData->end();
  for (frag = m_SectionData->begin(); frag != fragEnd; ++frag) {
    frag->setOffset(offset);
    offset += frag->size();
  }

  m_Section.setSize(offset);
}

