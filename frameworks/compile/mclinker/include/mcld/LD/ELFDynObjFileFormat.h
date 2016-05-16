//===- ELFDynObjFileFormat.h ----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_ELF_DYNAMIC_OBJECT_FILE_FROMAT_H
#define MCLD_ELF_DYNAMIC_OBJECT_FILE_FROMAT_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/LD/ELFFileFormat.h>

namespace mcld {

class ObjectBuilder;

/** \class ELFDynObjFileFormat
 *  \brief ELFDynObjFileFormat describes the format for ELF dynamic objects.
 */
class ELFDynObjFileFormat : public ELFFileFormat
{
  /// initObjectFormat - initialize sections that are dependent on shared
  /// objects.
  void initObjectFormat(ObjectBuilder& pBuilder, unsigned int pBitClass);
};

} // namespace of mcld

#endif

