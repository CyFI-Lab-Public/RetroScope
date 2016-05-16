//===- PLT.h --------------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_PROCEDURE_LINKAGE_TABLE_H
#define MCLD_PROCEDURE_LINKAGE_TABLE_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/LD/LDSection.h>
#include <mcld/LD/SectionData.h>
#include <mcld/Fragment/TargetFragment.h>

namespace mcld {

class LDSection;
class ResolveInfo;

/** \class PLTEntryDefaultBase
 *  \brief PLTEntryDefaultBase provides the default interface for PLE Entry
 */
class PLTEntryBase : public TargetFragment
{
public:
  PLTEntryBase(SectionData& pParent)
    : TargetFragment(Fragment::Target, &pParent), m_pValue(NULL)
  {}

  virtual ~PLTEntryBase()
  {
    delete m_pValue;
  }

  void setValue(unsigned char* pValue)
  { m_pValue = pValue; }

  const unsigned char* getValue() const
  { return m_pValue; }

  //Used by llvm::cast<>.
  static bool classof(const Fragment *O)
  { return true; }

protected:
  unsigned char* m_pValue;
};

/** \class PLT
 *  \brief Procedure linkage table
 */
class PLT
{
public:
  typedef SectionData::iterator iterator;
  typedef SectionData::const_iterator const_iterator;

  template<size_t SIZE, typename EntryBase = PLTEntryBase>
  class Entry : public EntryBase
  {
  public:
    enum { EntrySize = SIZE };

  public:
    Entry(SectionData& pParent)
      : EntryBase(pParent)
    {}

    virtual ~Entry() {}

    size_t size() const
    { return EntrySize; }
  };

public:
  PLT(LDSection& pSection);

  virtual ~PLT();

  /// reserveEntry - reseve the number of pNum of empty entries
  /// The empty entris are reserved for layout to adjust the fragment offset.
  virtual void reserveEntry(size_t pNum = 1) = 0;

  // finalizeSectionSize - set LDSection size
  virtual void finalizeSectionSize() = 0;

  uint64_t addr() const { return m_Section.addr(); }

  const_iterator begin() const { return m_SectionData->begin(); }
  iterator       begin()       { return m_SectionData->begin(); }
  const_iterator end  () const { return m_SectionData->end();   }
  iterator       end  ()       { return m_SectionData->end();   }

protected:
  LDSection& m_Section;
  SectionData* m_SectionData;
};

} // namespace of mcld

#endif

