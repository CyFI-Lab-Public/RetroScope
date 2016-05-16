//===- LinkerScript.h -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_LINKER_SCRIPT_H
#define MCLD_LINKER_SCRIPT_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <string>
#include <llvm/ADT/StringRef.h>
#include <mcld/ADT/StringEntry.h>
#include <mcld/ADT/StringHash.h>
#include <mcld/ADT/HashTable.h>
#include <mcld/Object/SectionMap.h>
#include <mcld/MC/SearchDirs.h>

namespace mcld {

/** \class LinkerScript
 *
 */
class LinkerScript
{
public:
  typedef HashTable<StringEntry<llvm::StringRef>,
                    hash::StringHash<hash::ELF>,
                    StringEntryFactory<llvm::StringRef> > SymbolRenameMap;

  typedef HashTable<StringEntry<uint64_t>,
                    hash::StringHash<hash::ELF>,
                    StringEntryFactory<uint64_t> > AddressMap;

  typedef HashTable<StringEntry<llvm::StringRef>,
                    hash::StringHash<hash::ELF>,
                    StringEntryFactory<llvm::StringRef> > DefSymMap;

public:
  LinkerScript();

  ~LinkerScript();

  const SymbolRenameMap& renameMap() const { return m_SymbolRenames; }
  SymbolRenameMap&       renameMap()       { return m_SymbolRenames; }

  const AddressMap& addressMap() const { return m_AddressMap; }
  AddressMap&       addressMap()       { return m_AddressMap; }

  const SectionMap& sectionMap() const { return m_SectionMap; }
  SectionMap&       sectionMap()       { return m_SectionMap; }

  const DefSymMap& defSymMap() const { return m_DefSymMap; }
  DefSymMap&       defSymMap()       { return m_DefSymMap; }

  /// search directory
  const SearchDirs& directories() const { return m_SearchDirs; }
  SearchDirs&       directories()       { return m_SearchDirs; }

  /// sysroot
  const sys::fs::Path& sysroot() const;

  void setSysroot(const sys::fs::Path &pPath);

  bool hasSysroot() const;

private:
  SymbolRenameMap m_SymbolRenames;
  AddressMap m_AddressMap;
  SectionMap m_SectionMap;
  DefSymMap m_DefSymMap;
  SearchDirs m_SearchDirs;
};

} // namespace of mcld

#endif

