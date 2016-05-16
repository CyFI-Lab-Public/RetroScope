//===- TargetFragment.h ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_LD_TARGET_FRAGMENT_H
#define MCLD_LD_TARGET_FRAGMENT_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/Fragment/Fragment.h>

namespace mcld {

class SectionData;

/** \class TargetFragment
 *  \brief TargetFragment is a kind of MCFragment inherited by
 *  target-depedent Fragment.
 */
class TargetFragment : public Fragment
{
protected:
  TargetFragment(Fragment::Type pKind, SectionData* pSD = NULL)
    : Fragment(pKind, pSD) {}

public:
  virtual ~TargetFragment() {}

  static bool classof(const Fragment *F)
  { return F->getKind() == Fragment::Target; }

  static bool classof(const TargetFragment *)
  { return true; }
};

} // namespace of mcld

#endif

