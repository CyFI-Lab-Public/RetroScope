//===- GCFactoryListTraits.h ----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_GC_FACTORY_LIST_TRAITS_H
#define MCLD_GC_FACTORY_LIST_TRAITS_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <llvm/ADT/ilist_node.h>
#include <llvm/ADT/ilist.h>

#include <assert.h>

namespace mcld {

/** \class GCFactoryListTraits
 *  \brief GCFactoryListTraits provides trait class for llvm::iplist when
 *  the nodes in the list is produced by GCFactory.
 */
template<typename DataType>
class GCFactoryListTraits : public llvm::ilist_default_traits<DataType>
{
private:
  class SentinelNode : public DataType
  {
  public:
    SentinelNode() { }
  };

public:
  // override the traits provided in llvm::ilist_sentinel_traits since we've
  // defined our own sentinel.
  DataType *createSentinel() const
  { return reinterpret_cast<DataType*>(&mSentinel); }

  static void destroySentinel(DataType*) { }

  DataType *provideInitialHead() const
  { return createSentinel(); }

  DataType *ensureHead(DataType*) const
  { return createSentinel(); }

  static void noteHead(DataType*, DataType*) { }

  // override the traits provided in llvm::ilist_node_traits since
  static DataType *createNode(const DataType &V) {
    assert(false && "Only GCFactory knows how to create a node.");
  }
  static void deleteNode(DataType *V) {
    // No action. GCFactory will handle it by itself.
  }

private:
  mutable SentinelNode mSentinel;
};

} // namespace of mcld

#endif
