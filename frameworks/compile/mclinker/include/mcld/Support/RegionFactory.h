//===- RegionFactory.h ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_SUPPORT_REGION_FACTORY_H
#define MCLD_SUPPORT_REGION_FACTORY_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/Config/Config.h>
#include <mcld/Support/GCFactory.h>
#include <mcld/Support/MemoryRegion.h>

namespace mcld {

class MemoryArea;

/** \class RegionFactory
 *  \brief RegionFactory produces and destroys MemoryRegions
 *
 */
class RegionFactory : public GCFactory<MemoryRegion, MCLD_REGION_CHUNK_SIZE>
{
public:
  typedef GCFactory<MemoryRegion, MCLD_REGION_CHUNK_SIZE> Alloc;
  typedef MemoryRegion::Address Address;
  typedef MemoryRegion::ConstAddress ConstAddress;

public:
  MemoryRegion* produce(Address pVMAStart, size_t pSize);

  void destruct(MemoryRegion* pRegion);
};

} // namespace of mcld

#endif

