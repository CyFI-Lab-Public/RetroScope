//===- GOT.h --------------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_GLOBAL_OFFSET_TABLE_H
#define MCLD_GLOBAL_OFFSET_TABLE_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/LD/LDSection.h>
#include <mcld/LD/SectionData.h>
#include <mcld/Fragment/TargetFragment.h>

namespace mcld {

class GOT;
class LDSection;
class ResolveInfo;

/** \class GOT
 *  \brief The Global Offset Table
 */
class GOT
{
protected:
  GOT(LDSection& pSection);

public:
  typedef SectionData::iterator iterator;
  typedef SectionData::const_iterator const_iterator;

  template<size_t SIZE>
  class Entry : public TargetFragment
  {
  public:
    enum { EntrySize = SIZE };

  public:
    Entry(uint64_t pValue, SectionData* pParent)
      : TargetFragment(Fragment::Target, pParent),
        f_Value(pValue) {
    }

    virtual ~Entry() {}

    uint64_t getValue() const
    { return f_Value; }

    void setValue(uint64_t pValue)
    { f_Value = pValue; }

    // Override pure virtual function
    size_t size() const
    { return EntrySize; }

  protected:
    uint64_t f_Value;
  };

public:
  virtual ~GOT();

  // ----- observers -----//
  uint64_t addr() const { return m_Section.addr(); }
  uint32_t size() const { return m_Section.size(); }

  const_iterator begin() const { return m_SectionData->begin(); }
  iterator       begin()       { return m_SectionData->begin(); }
  const_iterator end  () const { return m_SectionData->end();   }
  iterator       end  ()       { return m_SectionData->end();   }

  bool empty() const
  { return m_SectionData->empty(); }

  // finalizeSectionSize - set LDSection size
  virtual void finalizeSectionSize();

  /// reserve - reseve number of pNum of empty entries
  /// Before layout, we scan all relocations to determine if GOT entries are
  /// needed. If an entry is needed, the empty entry is reserved for layout
  /// to adjust the fragment offset. After that, we fill up the entries when
  /// applying relocations.
  virtual void reserve(size_t pNum = 1) = 0;

protected:
  LDSection& m_Section;
  SectionData* m_SectionData;
};

} // namespace of mcld

#endif

