//===- AttributeSet.h -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_ATTRIBUTE_SET_H
#define MCLD_ATTRIBUTE_SET_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/ADT/Uncopyable.h>
#include <vector>

namespace mcld {

class Attribute;

/** \class AttributeSet
 *  \brief AttributeSet is a set of Attribute.
 *
 *  Clients delegates Attributes to AttributeSet. AttributeSet deletes delegated
 *  Attributes during destruction.
 */
class AttributeSet : private Uncopyable
{
private:
  typedef std::vector<Attribute*> AttrSet;

public:
  typedef AttrSet::iterator iterator;
  typedef AttrSet::const_iterator const_iterator;

public:
  AttributeSet(unsigned int pNum, const Attribute& pPredefined);

  ~AttributeSet();

  // -----  iterators  ----- //
  const_iterator begin() const { return m_AttrSet.begin(); }
  iterator       begin()       { return m_AttrSet.begin(); }
  const_iterator end  () const { return m_AttrSet.end(); }
  iterator       end  ()       { return m_AttrSet.end(); }

  // exists- return the recorded attribute whose content is identical to the
  // input attribute.
  Attribute *exists(const Attribute& pAttr) const;

  // record - record the attribute no mater if it has been recorded.
  void record(Attribute& pAttr);

private:
  AttrSet m_AttrSet;
  const Attribute& m_Predefined;
};

} // namespace of mcld

#endif

