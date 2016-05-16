//===- BitcodeOption.cpp --------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/BitcodeOption.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// BitcodeOption
//===----------------------------------------------------------------------===//
BitcodeOption::BitcodeOption()
  : m_Position(-1) {
}

BitcodeOption::~BitcodeOption()
{
}

bool BitcodeOption::hasDefined() const
{
  return (m_Position != -1);
}
