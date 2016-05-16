//===- TypeTraits.h -------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_TYPE_TRAITS_H
#define MCLD_TYPE_TRAITS_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <cstdlib>

namespace mcld
{
template<typename DataType>
struct NonConstTraits;

template<typename DataType>
struct ConstTraits
{
  typedef DataType                 value_type;
  typedef const DataType*          pointer;
  typedef const DataType&          reference;
  typedef size_t                   size_type;
  typedef ConstTraits<DataType>    const_traits;
  typedef NonConstTraits<DataType> nonconst_traits;
};

template<typename DataType>
struct NonConstTraits
{
  typedef DataType                 value_type;
  typedef DataType*                pointer;
  typedef DataType&                reference;
  typedef size_t                   size_type;
  typedef ConstTraits<DataType>    const_traits;
  typedef NonConstTraits<DataType> nonconst_traits;
};

template<typename DataType>
struct ConstIteratorTraits
{
  typedef DataType                          value_type;
  typedef const DataType*                   pointer;
  typedef const DataType&                   reference;
  typedef size_t                            size_type;
  typedef ConstTraits<DataType>             const_traits;
  typedef NonConstTraits<DataType>          nonconst_traits;
  typedef typename DataType::const_iterator iterator;
};

template<typename DataType>
struct NonConstIteratorTraits
{
  typedef DataType                    value_type;
  typedef DataType*                   pointer;
  typedef DataType&                   reference;
  typedef size_t                      size_type;
  typedef ConstTraits<DataType>       const_traits;
  typedef NonConstTraits<DataType>    nonconst_traits;
  typedef typename DataType::iterator iterator;
};

} // namespace of mcld

#endif

