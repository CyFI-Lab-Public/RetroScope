//===- Stub.cpp -----------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//

#include <mcld/Fragment/Stub.h>

using namespace mcld;

Stub::Stub()
 : Fragment(Fragment::Stub),
   m_pSymInfo(NULL)
{
}

Stub::~Stub()
{
  for (fixup_iterator fixup= fixup_begin(); fixup != fixup_end(); ++fixup)
    delete(*fixup);
}

void Stub::setSymInfo(ResolveInfo* pSymInfo)
{
  m_pSymInfo = pSymInfo;
}

void Stub::addFixup(DWord pOffset, SWord pAddend, Type pType)
{
  assert(pOffset < size());
  m_FixupList.push_back(new Fixup(pOffset, pAddend, pType));
}

void Stub::addFixup(const Fixup& pFixup)
{
  assert(pFixup.offset() < size());
  m_FixupList.push_back(new Fixup(pFixup));
}

