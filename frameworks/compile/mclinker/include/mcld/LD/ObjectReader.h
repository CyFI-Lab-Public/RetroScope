//===- ObjectReader.h -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_OBJECT_READER_H
#define MCLD_OBJECT_READER_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include "mcld/LD/LDReader.h"
#include <llvm/Support/system_error.h>
#include <mcld/ADT/HashTable.h>
#include <mcld/ADT/StringHash.h>
#include <mcld/LD/ResolveInfo.h>

namespace mcld {

class Module;
class Input;

/** \class ObjectReader
 *  \brief ObjectReader provides an common interface for different object
 *  formats.
 */
class ObjectReader : public LDReader
{
protected:
  typedef HashTable<ResolveInfo, hash::StringHash<hash::ELF> > GroupSignatureMap;

protected:
  ObjectReader()
  { }

public:
  virtual ~ObjectReader() { f_GroupSignatureMap.clear(); }

  virtual bool readHeader(Input& pFile) = 0;

  virtual bool readSymbols(Input& pFile) = 0;

  virtual bool readSections(Input& pFile) = 0;

  /// readRelocations - read relocation sections
  ///
  /// This function should be called after symbol resolution.
  virtual bool readRelocations(Input& pFile) = 0;

  GroupSignatureMap& signatures()
  { return f_GroupSignatureMap; }

  const GroupSignatureMap& signatures() const
  { return f_GroupSignatureMap; }

protected:
  GroupSignatureMap f_GroupSignatureMap;

};

} // namespace of mcld

#endif

