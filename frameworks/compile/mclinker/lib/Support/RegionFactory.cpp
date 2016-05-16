//===- RegionFactory.cpp --------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Support/RegionFactory.h>
#include <mcld/Support/Space.h>

#include <new>

using namespace mcld;

//===----------------------------------------------------------------------===//
// RegionFactory
//===----------------------------------------------------------------------===//
MemoryRegion*
RegionFactory::produce(Address pVMAStart, size_t pSize)
{
  MemoryRegion* result = Alloc::allocate();
  new (result) MemoryRegion(pVMAStart, pSize);
  return result;
}

void RegionFactory::destruct(MemoryRegion* pRegion)
{
  destroy(pRegion);
  deallocate(pRegion);
}

