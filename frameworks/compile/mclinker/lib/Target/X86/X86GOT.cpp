//===- impl.cpp -----------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "X86GOT.h"

#include <mcld/LD/LDFileFormat.h>
#include <mcld/LD/SectionData.h>

#include <llvm/Support/Casting.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// X86_32GOT
//===----------------------------------------------------------------------===//
X86_32GOT::X86_32GOT(LDSection& pSection)
  : GOT(pSection), m_pLast(NULL)
{
}

X86_32GOT::~X86_32GOT()
{
}

void X86_32GOT::reserve(size_t pNum)
{
  for (size_t i = 0; i < pNum; i++) {
    new X86_32GOTEntry(0, m_SectionData);
  }
}

X86_32GOTEntry* X86_32GOT::consume()
{
  if (NULL == m_pLast) {
    assert(!empty() && "Consume empty GOT entry!");
    m_pLast = llvm::cast<X86_32GOTEntry>(&m_SectionData->front());
    return m_pLast;
  }

  m_pLast = llvm::cast<X86_32GOTEntry>(m_pLast->getNextNode());
  return m_pLast;
}

//===----------------------------------------------------------------------===//
// X86_64GOT
//===----------------------------------------------------------------------===//
X86_64GOT::X86_64GOT(LDSection& pSection)
  : GOT(pSection), m_pLast(NULL)
{
}

X86_64GOT::~X86_64GOT()
{
}

void X86_64GOT::reserve(size_t pNum)
{
  for (size_t i = 0; i < pNum; i++) {
    new X86_64GOTEntry(0, m_SectionData);
  }
}

X86_64GOTEntry* X86_64GOT::consume()
{
  if (NULL == m_pLast) {
    assert(!empty() && "Consume empty GOT entry!");
    m_pLast = llvm::cast<X86_64GOTEntry>(&m_SectionData->front());
    return m_pLast;
  }

  m_pLast = llvm::cast<X86_64GOTEntry>(m_pLast->getNextNode());
  return m_pLast;
}

