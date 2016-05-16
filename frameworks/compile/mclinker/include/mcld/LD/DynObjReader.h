//===- DynObjReader.h -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_DYNAMIC_SHARED_OBJECT_READER_H
#define MCLD_DYNAMIC_SHARED_OBJECT_READER_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include "mcld/LD/LDReader.h"
#include <llvm/Support/system_error.h>

namespace mcld {

class TargetLDBackend;
class Input;

/** \class DynObjReader
 *  \brief DynObjReader provides an common interface for different object
 *  formats.
 */
class DynObjReader : public LDReader
{
protected:
  DynObjReader()
  { }

public:
  virtual ~DynObjReader() { }

  virtual bool readHeader(Input& pFile) = 0;

  virtual bool readSymbols(Input& pFile) = 0;

};

} // namespace of mcld

#endif

