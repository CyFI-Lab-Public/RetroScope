//===- ELFSegmentFactory.cpp ----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/LD/ELFSegmentFactory.h>

using namespace mcld;

//==========================
// ELFSegmentFactory

ELFSegmentFactory::ELFSegmentFactory(size_t pNum)
  : GCFactory<ELFSegment, 0>(pNum)
{
}

ELFSegmentFactory::~ELFSegmentFactory()
{
}

/// produce - produce an empty ELF segment information.
/// this function will create an ELF segment
/// @param pType - p_type in ELF program header
ELFSegment* ELFSegmentFactory::produce(uint32_t pType, uint32_t pFlag)
{
  ELFSegment* segment = allocate();
  new (segment) ELFSegment(pType, pFlag);
  return segment;
}

ELFSegment*
ELFSegmentFactory::find(uint32_t pType, uint32_t pFlagSet, uint32_t pFlagClear)
{
  iterator segment, segEnd = end();
  for (segment = begin(); segment != segEnd; ++segment) {
    if ((*segment).type() == pType &&
        ((*segment).flag() & pFlagSet) == pFlagSet &&
        ((*segment).flag() & pFlagClear) == 0x0) {
      return &(*segment);
    }
  }
  return NULL;
}

const ELFSegment*
ELFSegmentFactory::find(uint32_t pType,
                        uint32_t pFlagSet,
                        uint32_t pFlagClear) const
{
  const_iterator segment, segEnd = end();
  for (segment = begin(); segment != segEnd; ++segment) {
    if ((*segment).type() == pType &&
        ((*segment).flag() & pFlagSet) == pFlagSet &&
        ((*segment).flag() & pFlagClear) == 0x0) {
      return &(*segment);
    }
  }
  return NULL;
}

