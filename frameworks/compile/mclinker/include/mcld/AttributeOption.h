//===- AttributeOption.h --------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_ATTRIBUTE_OPTIONS_H
#define MCLD_ATTRIBUTE_OPTIONS_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/MC/Attribute.h>

namespace mcld {

class AttributeOption
{
public:
  AttributeOption();
  ~AttributeOption();

  const Attribute& predefined() const { return m_Predefined; }
  Attribute&       predefined()       { return m_Predefined; }

  const AttrConstraint& constraint() const { return m_Constraint; }
  AttrConstraint&       constraint()       { return m_Constraint; }

private:
  Attribute m_Predefined;
  AttrConstraint m_Constraint;
};

} // namespace mcld

#endif

