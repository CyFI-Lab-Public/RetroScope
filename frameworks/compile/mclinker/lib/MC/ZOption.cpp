//===- ZOption.cpp --------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/MC/ZOption.h>

using namespace mcld;

//==========================
// ZOption

ZOption::ZOption()
  : m_Kind(Unknown),
    m_PageSize(0x0)
{
}

ZOption::~ZOption()
{
}

