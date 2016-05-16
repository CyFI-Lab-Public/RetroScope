//===- PathCache.h --------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_PATHCACHE_H
#define MCLD_PATHCACHE_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/ADT/HashEntry.h>
#include <mcld/ADT/HashTable.h>
#include <mcld/ADT/StringHash.h>
#include <mcld/Support/Path.h>

namespace mcld {
namespace sys  {
namespace fs   {

namespace {
  typedef HashEntry<llvm::StringRef,
                    mcld::sys::fs::Path,
                    hash::StringCompare<llvm::StringRef> > HashEntryType;
} // anonymous namespace

typedef HashTable<HashEntryType, hash::StringHash<hash::BKDR>, EntryFactory<HashEntryType> > PathCache;

} // namespace of fs
} // namespace of sys
} // namespace of mcld

#endif

