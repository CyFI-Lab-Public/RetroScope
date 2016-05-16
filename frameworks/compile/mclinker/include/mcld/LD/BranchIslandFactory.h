//===- BranchIslandFactory.h ----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_LD_BRANCH_ISLAND_FACTORY_H
#define MCLD_LD_BRANCH_ISLAND_FACTORY_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <llvm/Support/DataTypes.h>
#include <mcld/Support/GCFactory.h>
#include <mcld/LD/BranchIsland.h>

namespace mcld
{

class Fragment;

/** \class BranchIslandFactory
 *  \brief
 *
 */
class BranchIslandFactory : public GCFactory<BranchIsland, 0>
{
public:
  /// ctor
  /// @param pMaxBranchRange - the max branch range of the target backend
  /// @param pMaxIslandSize - a predifned value (64KB here) to decide the max
  ///                         size of the island
  BranchIslandFactory(uint64_t pMaxBranchRange,
                      uint64_t pMaxIslandSize = 65536U);

  ~BranchIslandFactory();

  /// produce - produce a island for the given fragment
  /// @param pFragment - the fragment needs a branch island
  BranchIsland* produce(Fragment& pFragment);

  /// find - find a island for the given fragment
  /// @param pFragment - the fragment needs a branch isladn
  BranchIsland* find(const Fragment& pFragment);

private:
  uint64_t m_MaxBranchRange;
  uint64_t m_MaxIslandSize;
};

} // namespace of mcld

#endif

