//===- SymbolEntryMap.h ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_TARGET_SYMBOL_ENTRY_MAP_H
#define MCLD_TARGET_SYMBOL_ENTRY_MAP_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <vector>

namespace mcld {

class ResolveInfo;

/** \class SymbolEntryMap
 *  \brief SymbolEntryMap is a <const ResolveInfo*, ENTRY*> map.
 */
template<typename ENTRY>
class SymbolEntryMap
{
public:
  typedef ENTRY EntryType;

private:
  struct Mapping {
    const ResolveInfo* symbol;
    EntryType*   entry;
  };

  typedef std::vector<Mapping> SymbolEntryPool;

public:
  typedef typename SymbolEntryPool::iterator iterator;
  typedef typename SymbolEntryPool::const_iterator const_iterator;

public:
  const EntryType* lookUp(const ResolveInfo& pSymbol) const;
  EntryType*       lookUp(const ResolveInfo& pSymbol);

  void record(const ResolveInfo& pSymbol, EntryType& pEntry);

  bool   empty() const { return m_Pool.empty(); }
  size_t size () const { return m_Pool.size(); }

  const_iterator begin() const { return m_Pool.begin(); }
  iterator       begin()       { return m_Pool.begin(); }
  const_iterator end  () const { return m_Pool.end();   }
  iterator       end  ()       { return m_Pool.end();   }

  void reserve(size_t pSize) { m_Pool.reserve(pSize); }

private:
  SymbolEntryPool m_Pool;

};

template<typename EntryType>
const EntryType*
SymbolEntryMap<EntryType>::lookUp(const ResolveInfo& pSymbol) const
{
  const_iterator mapping, mEnd = m_Pool.end();
  for (mapping = m_Pool.begin(); mapping != mEnd; ++mapping) {
    if (mapping->symbol == &pSymbol) {
      return mapping->entry;
    }
  }

  return NULL;
}

template<typename EntryType>
EntryType*
SymbolEntryMap<EntryType>::lookUp(const ResolveInfo& pSymbol)
{
  iterator mapping, mEnd = m_Pool.end();
  for (mapping = m_Pool.begin(); mapping != mEnd; ++mapping) {
    if (mapping->symbol == &pSymbol) {
      return mapping->entry;
    }
  }

  return NULL;
}

template<typename EntryType>
void
SymbolEntryMap<EntryType>::record(const ResolveInfo& pSymbol, EntryType& pEntry)
{
  Mapping mapping;
  mapping.symbol = &pSymbol;
  mapping.entry = &pEntry;
  m_Pool.push_back(mapping);
}

} // namespace of mcld

#endif

