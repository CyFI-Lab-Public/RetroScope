//===- HashEntryFactory.h --------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_HASH_ENTRY_FACTORY_H
#define MCLD_HASH_ENTRY_FACTORY_H

namespace mcld {

/** \class HashEntryFactory
 *  \brief HashEntryFactoy is a factory wrapper for those entries who have
 *  factory methods.
 */
template<typename HashEntryTy>
class HashEntryFactory
{
public:
  typedef HashEntryTy           entry_type;
  typedef typename HashEntryTy::key_type key_type;

public:
  entry_type* produce(const key_type& pKey)
  { return HashEntryTy::Create(pKey); }

  void destroy(entry_type*& pEntry)
  { HashEntryTy::Destroy(pEntry); }
};

} // namespace of mcld

#endif

