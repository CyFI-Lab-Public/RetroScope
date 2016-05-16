//===- RegionFragment.h ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_LD_REGION_FRAGMENT_H
#define MCLD_LD_REGION_FRAGMENT_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <mcld/Fragment/Fragment.h>

namespace mcld {

class MemoryRegion;

/** \class RegionFragment
 *  \brief RegionFragment is a kind of Fragment containing mcld::MemoryRegion
 */
class RegionFragment : public Fragment
{
public:
  RegionFragment(MemoryRegion& pRegion, SectionData* pSD = NULL);

  ~RegionFragment();

  const MemoryRegion& getRegion() const { return m_Region; }
  MemoryRegion&       getRegion()       { return m_Region; }

  static bool classof(const Fragment *F)
  { return F->getKind() == Fragment::Region; }

  static bool classof(const RegionFragment *)
  { return true; }

  size_t size() const;

private:
  MemoryRegion& m_Region;
};

} // namespace of mcld

#endif

