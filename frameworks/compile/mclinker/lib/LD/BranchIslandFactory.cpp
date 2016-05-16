//===- BranchIslandFactory.cpp --------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/LD/BranchIslandFactory.h>
#include <mcld/Fragment/Fragment.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// BranchIslandFactory
//===----------------------------------------------------------------------===//

/// ctor
/// @param pMaxBranchRange - the max branch range of the target backend
/// @param pMaxIslandSize - a predifned value (1KB here) to decide the max
///                         size of the island
BranchIslandFactory::BranchIslandFactory(uint64_t pMaxBranchRange,
                                         uint64_t pMaxIslandSize)
 : GCFactory<BranchIsland, 0>(1u), // magic number
   m_MaxBranchRange(pMaxBranchRange - pMaxIslandSize),
   m_MaxIslandSize(pMaxIslandSize)
{
}

BranchIslandFactory::~BranchIslandFactory()
{
}

/// produce - produce a island for the given fragment
/// @param pFragment - the fragment needs a branch island
BranchIsland* BranchIslandFactory::produce(Fragment& pFragment)
{
  assert(NULL == find(pFragment));
  uint64_t island_offset = pFragment.getOffset() + m_MaxBranchRange -
                           (pFragment.getOffset() % m_MaxBranchRange);

  // find out the last fragment whose offset is smaller than the calculated
  // offset of the island
  Fragment* frag = &pFragment;
  while (NULL != frag->getNextNode()) {
    if (frag->getNextNode()->getOffset() > island_offset)
      break;
    frag = frag->getNextNode();
  }

  // fall back one step if needed
  if (NULL != frag &&
      (frag->getOffset() + frag->size()) > island_offset)
    frag = frag->getPrevNode();

  // check not to break the alignment constraint in the target section
  // (i.e., do not insert the island after a Alignment fragment)
  while (NULL != frag &&
         Fragment::Alignment == frag->getKind()) {
    frag = frag->getPrevNode();
  }

  // can not find an entry fragment to bridge the island
  if (NULL == frag)
    return NULL;

  BranchIsland *island = allocate();
  new (island) BranchIsland(*frag,           // entry fragment to the island
                            m_MaxIslandSize, // the max size of the island
                            size() - 1u);     // index in the island factory
  return island;
}

/// find - find a island for the given fragment
/// @param pFragment - the fragment needs a branch isladn
BranchIsland* BranchIslandFactory::find(const Fragment& pFragment)
{
  // Currently we always find the island in a forward direction.
  // TODO: If we can search backward, then we may reduce the number of islands.
  for (iterator it = begin(), ie = end(); it != ie; ++it) {
    if ((pFragment.getOffset() < (*it).offset()) &&
        ((pFragment.getOffset() + m_MaxBranchRange) >= (*it).offset()))
      return &(*it);
  }
  return NULL;
}

