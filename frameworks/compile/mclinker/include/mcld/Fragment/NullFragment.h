//===- NullFragment.h -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_LD_NULL_FRAGMENT_H
#define MCLD_LD_NULL_FRAGMENT_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/Fragment/Fragment.h>

namespace mcld {

class SectionData;

/** \class NullFragment
 *  \brief NullFragment is a kind of MCFragment that presents the "end fragment"
 *         referenced by some special symbols
 */
class NullFragment : public Fragment
{
public:
  NullFragment(SectionData* pSD = NULL);

  /// size -
  size_t size() const { return 0x0; }

  static bool classof(const Fragment *F)
  { return F->getKind() == Fragment::Null; }

  static bool classof(const NullFragment *)
  { return true; }
};

} // namespace of mcld

#endif

