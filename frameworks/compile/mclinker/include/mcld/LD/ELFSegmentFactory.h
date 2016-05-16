//===- ELFSegmentFactory.h ------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_ELFSEGMENT_FACTORY_H
#define MCLD_ELFSEGMENT_FACTORY_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/Support/GCFactory.h>
#include <mcld/LD/ELFSegment.h>

namespace mcld
{

/** \class ELFSegmentFactory
 *  \brief provide the interface to create and delete an ELFSegment
 */
class ELFSegmentFactory : public GCFactory<ELFSegment, 0>
{
public:
  /// ELFSegmentFactory - the factory of ELFSegment
  /// pNum is the magic number of the ELF segments in the output
  ELFSegmentFactory(size_t pNum);
  ~ELFSegmentFactory();

  /// produce - produce an empty ELF segment information.
  /// this function will create an ELF segment
  /// @param pType - p_type in ELF program header
  ELFSegment* produce(uint32_t pType, uint32_t pFlag = llvm::ELF::PF_R);

  ELFSegment*
  find(uint32_t pType, uint32_t pFlagSet, uint32_t pFlagClear);

  const ELFSegment*
  find(uint32_t pType, uint32_t pFlagSet, uint32_t pFlagClear) const;
};

} // namespace of mcld

#endif

