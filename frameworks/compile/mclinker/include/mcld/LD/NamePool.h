//===- NamePool.h ---------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_NAME_POOL_H
#define MCLD_NAME_POOL_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/Config/Config.h>
#include <mcld/ADT/HashTable.h>
#include <mcld/ADT/StringHash.h>
#include <mcld/ADT/Uncopyable.h>
#include <mcld/LD/Resolver.h>
#include <mcld/LD/ResolveInfo.h>
#include <mcld/Support/GCFactory.h>

#include <utility>

#include <llvm/ADT/StringRef.h>

namespace mcld {

class StringTable;
class SymbolTableIF;
class SectionData;

/** \class NamePool
 *  \brief Store symbol and search symbol by name. Can help symbol resolution.
 *
 *  - MCLinker is responsed for creating NamePool.
 */
class NamePool : private Uncopyable
{
public:
  typedef HashTable<ResolveInfo, hash::StringHash<hash::ELF> > Table;
  typedef size_t size_type;

public:
  explicit NamePool(size_type pSize = 3);

  ~NamePool();

  // -----  modifiers  ----- //
  /// createSymbol - create a symbol but do not insert into the pool.
  /// The created symbol did not go through the path of symbol resolution.
  ResolveInfo* createSymbol(const llvm::StringRef& pName,
                            bool pIsDyn,
                            ResolveInfo::Type pType,
                            ResolveInfo::Desc pDesc,
                            ResolveInfo::Binding pBinding,
                            ResolveInfo::SizeType pSize,
                            ResolveInfo::Visibility pVisibility = ResolveInfo::Default);
  
  /// insertSymbol - insert a symbol and resolve the symbol immediately
  /// @param pOldInfo - if pOldInfo is not NULL, the old ResolveInfo being
  ///                   overriden is kept in pOldInfo.
  /// @param pResult the result of symbol resultion.
  /// @note pResult.override is true if the output LDSymbol also need to be
  ///       overriden
  void insertSymbol(const llvm::StringRef& pName,
                    bool pIsDyn,
                    ResolveInfo::Type pType,
                    ResolveInfo::Desc pDesc,
                    ResolveInfo::Binding pBinding,
                    ResolveInfo::SizeType pSize,
                    ResolveInfo::Visibility pVisibility,
                    ResolveInfo* pOldInfo,
                    Resolver::Result& pResult);

  /// findSymbol - find the resolved output LDSymbol
  const LDSymbol* findSymbol(const llvm::StringRef& pName) const;
  LDSymbol*       findSymbol(const llvm::StringRef& pName);

  /// findInfo - find the resolved ResolveInfo
  const ResolveInfo* findInfo(const llvm::StringRef& pName) const;
  ResolveInfo*       findInfo(const llvm::StringRef& pName);

  /// insertString - insert a string
  /// if the string has existed, modify pString to the existing string
  /// @return the StringRef points to the hash table
  llvm::StringRef insertString(const llvm::StringRef& pString);

  // -----  observers  ----- //
  size_type size() const
  { return m_Table.numOfEntries(); }

  bool empty() const
  { return m_Table.empty(); }

  // -----  capacity  ----- //
  void reserve(size_type pN);

  size_type capacity() const;

private:
  typedef GCFactory<ResolveInfo*, 128> FreeInfoSet;

private:
  Resolver* m_pResolver;
  Table m_Table;
  FreeInfoSet m_FreeInfoSet;
};

} // namespace of mcld

#endif

