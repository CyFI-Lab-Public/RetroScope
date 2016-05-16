//===- BinaryReader.h -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_Binary_READER_H
#define MCLD_Binary_READER_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include "mcld/LD/LDReader.h"
#include <llvm/Support/system_error.h>

namespace mcld {

class Module;
class Input;

/** \class BinaryReader
 *  \brief BinaryReader provides an common interface for different Binary
 *  formats.
 */
class BinaryReader : public LDReader
{
protected:
  BinaryReader()
  { }

public:
  virtual ~BinaryReader()
  { }

  virtual bool isMyFormat(Input& pInput) const
  { return true; }

  virtual bool readBinary(Input& pFile) = 0;
};

} // namespace of mcld

#endif

