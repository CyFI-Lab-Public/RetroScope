//===- FGNode.cpp ---------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Fragment/FGNode.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// FGNode
//===----------------------------------------------------------------------===//
FGNode::FGNode()
  : m_Index(0x0)
{
}

FGNode::FGNode(uint32_t pIndex)
  : m_Index(pIndex)
{
}

void FGNode::addFragment(Fragment* pFrag)
{
  m_Fragments.push_back(pFrag);
}

void FGNode::addSignal(Signal pSignal)
{
  m_Signals.push_back(pSignal);
}

void FGNode::addSlot(Slot pSlot)
{
  m_Slots.push_back(pSlot);
}

